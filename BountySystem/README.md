# BountySystem

Ein produktionsreifes Paper-Plugin für ein Bounty-System mit Vault- und
EternalEconomy-Unterstützung, exakter Geldbetrag-Verarbeitung (`BigDecimal`)
und einem Klick-Diebstahl-sicheren Bestätigungs-GUI.

## Voraussetzungen

- **Paper** 1.26.2 (oder neuer aus derselben Build-Reihe)
- **Java 25** (von Paper 26.x vorausgesetzt)
- **Vault** sowie ein dazu kompatibles Economy-Plugin (z. B. **EternalEconomy**,
  EssentialsX Economy, CMI, ...)

## Bauen

```bash
mvn -B package
```

Die fertige JAR liegt danach unter `target/BountySystem-1.0.0.jar`.
Der mitgelieferte GitHub-Actions-Workflow (`.github/workflows/maven.yml`)
baut das Projekt bei jedem Push/PR automatisch mit JDK 25 und lädt die JAR
als Artefakt hoch.

## Installation

1. `BountySystem-1.0.0.jar` in den `plugins`-Ordner des Servers legen.
2. Sicherstellen, dass **Vault** und ein kompatibles Economy-Plugin
   installiert sind.
3. Server (neu) starten.

## Befehle

| Befehl | Beschreibung | Berechtigung |
|---|---|---|
| `/bounty add <Spieler> <Betrag>` | Setzt ein Bounty aus (öffnet ein Bestätigungs-GUI) | `bounty.add` (Standard: alle) |
| `/bounty <Spieler>` | Zeigt das aktuelle Bounty eines Spielers | `bounty.show` (Standard: alle) |
| `/bountyadmin reload` (Alias `/bountyad reload`) | Lädt `config.yml` neu und wiederholt die Economy-Erkennung | `bounty.admin` (Standard: op) |

Ein Spieler darf auch ein Bounty auf sich selbst aussetzen. Stirbt er
anschließend **ohne** dass ein anderer Spieler ihn getötet hat (Sturz, Lava,
Ertrinken, `/kill`, sonstige Selbsttötung), kassiert er sein eigenes Bounty
selbst. Wird er von einem anderen Spieler getötet, erhält dieser das Bounty.

### Chat-Ablauf von `/bounty add`

Beim Ausführen von `/bounty add <Name> <Betrag>` passiert bei gültiger
Eingabe **nichts** im Chat – es öffnet sich direkt das Bestätigungs-GUI.
Erst nach Klick auf **Bestätigen** erscheinen die Nachrichten:

- beim Absender: `You added $<Betrag> to <Name>'s bounty`
- beim Ziel (falls online): `<Absender> added $<Betrag> to your Bounty`

Bei ungültigen Eingaben (unbekannter Spieler, ungültiger Betrag, zu wenig
Geld, keine Berechtigung, kein Economy-Plugin) erscheint sofort eine
entsprechende Fehlermeldung statt des GUIs.

## Geldbeträge

Alle Beträge werden intern ausschließlich mit `BigDecimal` verarbeitet, damit
sie exakt bleiben – auch bei sehr großen Bountys. Es wird an keiner Stelle
mit `double` gerechnet, außer an einer einzigen, technisch unvermeidbaren
Stelle: siehe [Technische Hinweise](#technische-hinweise) unten.

Unterstützte Schreibweisen bei `/bounty add` (Groß-/Kleinschreibung egal):

| Eingabe | Bedeutung | Wert |
|---|---|---|
| `1000000` | reine Zahl | 1.000.000 |
| `1000000.50` | reine Zahl mit Nachkommastellen | 1.000.000,50 |
| `1k` | Tausend | 1.000 |
| `1.5m` | Million | 1.500.000 |
| `1b` | Milliarde | 1.000.000.000 |
| `2.75b` | Milliarde | 2.750.000.000 |
| `10t` | Billion | 10.000.000.000.000 |
| `1q` | Billiarde | 1.000.000.000.000.000 |

## Konfiguration (`config.yml`)

- `minimum-amount` – kleinster erlaubter Bounty-Betrag (gleiche Schreibweise
  wie oben, z. B. `"100"` oder `"1k"`).
- `broadcast-bounty-payout` – wenn `true`, wird eine Auszahlung server-weit
  angekündigt; wenn `false`, bekommt nur der Begünstigte eine Nachricht.
- `messages.*` – alle Chat-Texte, inklusive Farbcodes (`&`) und Platzhaltern
  wie `%player%`, `%amount%`, `%target%`, `%sender%`, `%killer%`, `%victim%`.

## Persistenz

Aktive Bountys werden in `plugins/BountySystem/bounties.yml` gespeichert.
Jede Änderung (Bounty hinzugefügt, Bounty ausgezahlt) wird **sofort und
synchron** auf die Festplatte geschrieben, bevor die Erfolgsmeldung verschickt
wird. Dadurch überstehen Bountys sowohl `/bountyadmin reload` als auch
volle Server-Neustarts. Beträge werden als exakter Text
(`BigDecimal#toPlainString()`) gespeichert, nicht als `double`.

## Schutz vor Missbrauch

- **Kein doppeltes Auszahlen:** Das Entfernen eines Bountys beim Tod des
  Ziels geschieht atomar (lesen und löschen in einem Schritt). Ein Bounty
  kann dadurch nicht zweimal ausgezahlt werden.
- **Kein doppeltes Bestätigen:** Das GUI markiert einen Vorgang beim ersten
  Klick auf Bestätigen/Abbrechen sofort als erledigt (`compareAndSet`).
  Ein schneller Doppelklick löst die Transaktion nur einmal aus.
- **Kein Diebstahl aus dem GUI:** Jeder Klick und jedes Ziehen von Items wird
  abgebrochen, solange das Bestätigungs-GUI offen ist – auch Shift-Klicks aus
  dem eigenen Spieler-Inventar heraus.
- **Erneute Prüfung beim Bestätigen:** Guthaben wird nicht nur beim Öffnen des
  GUI, sondern erneut beim Klick auf Bestätigen geprüft (falls sich der
  Kontostand zwischenzeitlich geändert hat).

## Economy-Erkennung

Die Erkennung erfolgt in dieser Reihenfolge:

1. **EternalEconomy über Vault**, falls EternalEconomy bei Vault als
   Economy-Provider registriert ist.
2. **Ein anderer kompatibler Vault-Economy-Provider**, falls kein
   EternalEconomy registriert ist, aber ein anderer Vault-Provider
   verfügbar ist.
3. **Direkte EternalEconomy-Anbindung ohne Vault** – siehe Hinweis unten.
4. **Kein Economy-Plugin verfügbar** – alle Befehle melden dann einen klaren,
   konfigurierbaren Fehler (`economy-unavailable`) statt stillschweigend zu
   versagen.

## Technische Hinweise

**Zu Stufe 3 (direkte EternalEconomy-API):** EternalEconomy
(github.com/EternalCodeTeam/EternalEconomy) verlangt laut eigener
Dokumentation selbst zwingend das Vault-Plugin, um überhaupt als
Economy-Provider zu funktionieren ("EternalEconomy requires Vault"). Eine
öffentlich dokumentierte, eigenständige Java-API für Fremdplugins (ohne
Vault) konnte ich nicht verifizieren. Da ausdrücklich keine erfundenen
Klassen/Methoden verwendet werden sollen, führt das Plugin in diesem Fall
**keine** direkte Anbindung mit geratenen API-Aufrufen durch. Stattdessen
erkennt `EconomyManager` diesen Zustand zuverlässig (EternalEconomy ist
installiert, aber ohne funktionierende Vault-Brücke) und protokolliert eine
klare, verständliche Warnung. In der Praxis ist das unkritisch: Da
EternalEconomy selbst Vault voraussetzt, deckt Stufe 1 (EternalEconomy über
Vault) so gut wie jeden real vorkommenden Fall ab. Falls sich das ändert
(z. B. wird künftig eine offizielle Standalone-API veröffentlicht), kann
`EconomyManager#initialize()` um eine echte Stufe-3-Implementierung erweitert
werden.

**Zur `double`-Grenze bei Vault:** Die offizielle Vault-Economy-API
(`net.milkbowl.vault.economy.Economy`) rechnet in ihrer Schnittstelle
grundsätzlich mit `double` – das lässt sich ohne eine erfundene API nicht
umgehen. `VaultEconomyProvider` normalisiert deshalb jeden `BigDecimal`-Betrag
unmittelbar vor dem Aufruf an Vault exakt auf zwei Nachkommastellen und
wandelt ihn erst dann in `double` um. Überall sonst im Plugin – Speicherung,
Addition mehrerer Bounty-Einzahlungen, Anzeige im Chat und im GUI – wird
ausschließlich mit `BigDecimal` gerechnet, sodass der interne Betrag exakt
bleibt. Diese `double`-Umwandlung ist eine Eigenschaft des zugrunde liegenden
Economy-Systems selbst, nicht des Plugins.

**Bekannte Grenze der Datei-Persistenz:** Wie bei den meisten YAML-basierten
Plugins gilt: Stürzt der Server exakt zwischen dem Abbuchen des Geldes und
dem erfolgreichen Schreiben der Datei ab, könnte im Extremfall eine
Dateninkonsistenz entstehen. Für die allermeisten Server ist die synchrone
Sofort-Speicherung nach jeder Änderung ausreichend robust; wer maximale
Datensicherheit braucht, kann `BountyStorage` bei Bedarf auf eine
Datenbank (z. B. SQLite/MySQL über HikariCP) umstellen.

## Build-Koordinaten

- Paper-API: `io.papermc.paper:paper-api:[26.2.build,)` über
  `https://repo.papermc.io/repository/maven-public/`
- VaultAPI: `com.github.MilkBowl:VaultAPI:1.7` über `https://jitpack.io`

`groupId`/Package (`dev.bountysystem`) sind Platzhalter – bei Bedarf gerne
auf eine eigene Domain umstellen.
