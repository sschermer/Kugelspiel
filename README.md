# ORBIFLUX

Eigenständiger nativer Android-2D-Puzzle-Platformer, **Iteration 010 / Version 0.10.0**.

## Steuerung ab Version 0.10.0

Über die unten mittig eingeblendete Schaltfläche **MENÜ** lässt sich jederzeit zwischen **DREI TASTEN** und **ZWEI HÄLFTEN** wählen. Die Auswahl bleibt beim nächsten App-Start erhalten. Die klassische Steuerung bleibt zunächst die Standardeinstellung.

- **Drei Tasten:** bisherige Steuerung für links, rechts und springen; beide Richtungstasten 2,5 Sekunden halten setzt den Level zurück.
- **Zwei Hälften:** Die drei Steuerungstasten sind vollständig ausgeblendet. Die erste Berührung der linken bzw. rechten Bildschirmhälfte bewegt die Kugel in die entsprechende Richtung. Ein zweiter Finger auf der anderen Hälfte löst genau einen Sprung aus, ohne die Bewegungsrichtung des ersten Fingers zu verändern. Nach dem Loslassen des zweiten Fingers kann ein erneutes Aufsetzen wieder springen. Ebenfalls springt die Kugel beim Loslassen und erneuten Berühren derselben Hälfte innerhalb von 260 ms. Ein Richtungswechsel durch Wischen allein löst keinen Sprung aus.
- **Menü:** Das Spiel pausiert während der Auswahl; der Level kann hier auch neu gestartet werden. Die Funktionalität der Sprungketten und ihre Physik bleiben erhalten. Das Einstellungsmenü ist die einzige eingeblendete Schaltfläche im Zwei-Hälften-Modus.

Die berührungsbasierte Steuerlogik wird mit einem zusätzlichen reinen Java-Test geprüft. Ein realer Test der Bedienung auf unterschiedlichen Android-Geräten bleibt zusätzlich erforderlich.

## Produktregeln

Drei Spieltasten: links, rechts, springen. Metallische Kugel mit leichter Trägheit und Kamera-Scrollen in beiden Achsen. Wiederverwendbare Farb- und Gravitations-Items. Die Farb-Sperrwände reichen lückenlos vom Boden bis zur Decke. Ein passendes Farb-Item lässt sie passieren.

**NEU: Ein Level ist erst beendet, wenn jede einzelne Punktmarke (1/5/10) eingesammelt wurde.** Der Level-Exit besitzt keine niedrigere Punktschwelle mehr. Er zeigt `NOCH X MARKEN` und dann `OFFEN`. Das HUD zeigt eingesammelte/gesamte Marken sowie den zugehörigen Punktestand. Einzelne Marken bleiben nach Einsammeln sichtbar, wechseln von schwarz/weiß zu halbtransparent weiß/schwarz und zählen nur einmal. Punktetüren innerhalb der Welten können weiterhin Mindestpunktzahlen verlangen, ohne Punkte zu verbrauchen.

**NEU: Normale Öffnungen sind in beide Richtungen durchlässig** (sofern die Kugel die Öffnung mit Sprüngen, umgekehrter Gravitation oder Helium erreichen kann). Der Spieler sieht an den Kanten nur dezentes Türkis, ohne Text oder Pfeile. Tödliche Spalten sind rot mit Zacken, ebenfalls ohne Text. Es gibt derzeit **keine** Einbahnöffnung; eine spätere Einbahnmechanik müsste bewusst separat entwickelt und anders gekennzeichnet werden.

## Individuelle Level statt wiederholter Etagenvorlagen

| Level | Aufbau | Rätselidee |
|---|---|---|
| 1 | Kurzer Einstieg | Alle Marken einsammeln |
| 2 | Eine Farbbarriere | Farbwechsel und Vollständigkeit |
| 3 | Zwei Ebenen | Auf- und Abstieg, erste Gravitationswechsel |
| 4 – Die Höhlengabelung | Kompakte Höhlenschleifen auf 3 Ebenen | Farbpfade, Einsturzöffnungen, Sammelstellen auf Vorsprüngen |
| 5 – Das Treppenwerk | 4 Ebenen, absichtlich fehlende Stufen | Sprungketten und 18-Punkte-Zwischentür |
| 6 – Die zwei Reiche | 4 Ebenen, getrennter rechter und linker Bereich | 20-Punkte-Zwischentüren auf allen Etagen, später Rückweg zum Ausgang |
| 7 – Die Umkehrkammer | 4 Ebenen, lange tödliche Bodenunterbrechung | **Gravitation nach oben ist die vorgesehene Lösung** zur sicheren Deckenquerung; Rückschalter hinter der Gefahr |
| 8 – Die Heliumfabrik | 5 Ebenen, geschlossener vertikaler Doppelschacht | **Helium ist für die Punktmarke oben im Schacht vorgesehen und baulich nötig**, weil es dort keine Zwischenlandefläche gibt |
| 9 – Die Schlüsselfarben | 5 Ebenen, drei verschiedene Farbschlüssel | Farbkombinationen, Rückwege, Punkte-Zwischentür, ohne unnötige Spezialmechanik |
| 10 – Das Resonanzlabyrinth | 6 Ebenen, zwei voneinander getrennte Sonderpassagen | Gravitation über einen langen gefährlichen Boden und Helium in einem zweiten, mehrstöckigen Schacht |

Alle Level ab 4 haben **eigene, ausdrücklich entworfene Grundrisse** und sind deutlich kompakter als die früheren gestreckten Karten. Die ersten Welten haben 44, 49 und 53 Spalten; die größten späteren Karten 72 statt 125. Funktionale Elemente, Felsen, Plattformen, Stufen, Schlitze und Kreuzungen sind eng genug verteilt, um ereignislose Laufstrecken zu begrenzen. Manche Zwischenebenen sind vollständig unterbrochen.

**Spezialmechaniken nur, wenn sie eine Aufgabe erfüllen:**
- In Level 4–6 und 9 kommen weder Helium- noch Gravitations-Items vor.
- In Level 7 und 10 führt umgekehrte Gravitation über eine sonst tödliche, durchgehende breite Bodengefahr zu einer Decken-Punktmarke und anschließend zu einem Rückschalter.
- In Level 8 und 10 ermöglichen gezielt platzierte HE+/HE−-Apparaturen den Aufstieg durch baulich abgeschlossene vertikale Schächte. Die Dächer und Wände verhindern gewöhnliches Betreten, Zwischenlandeflächen fehlen und ein 250-%-Stapelsprung kann die zwei Ebenen nicht auf einmal überwinden.
- Ein versehentlich aktivierter Gravitationszustand kann dank jeweils vorhandener Gegenstation rückgängig gemacht werden; Apparaturen bleiben wiederverwendbar.

Die Ballphysik mit normalem Hüpfer und stapelbaren Sprüngen (100/150/200/250 % Höhe) bleibt erhalten. Weitere Kettensprünge bei 250 % sind erlaubt, ein Deckenkontakt setzt die Sprungkraft sofort auf 100 % zurück.

## Testsystem

GitHub Actions verwendet Java 17, Gradle 8.10.2 und Android SDK 35. Vor dem Android-Build laufen die Java-Tests der Karten- und Hüpferlogik. Die Karten-Tests überprüfen: Punkte-Gesamtsumme = Exit-Ziel, Anzahl der Sammelmarken, dünne Durchgangsgeometrie, vollständige Sperrwände, Gate-Voraussetzungen, bauliche Schachtbarrieren, zu jedem Spezialmechanismus vorhandene Gegenstationen und keine langen völlig ereignislosen Korridore.

**Grenze dieser Tests:** Es gibt noch keinen vollständigen physikbasierten Spiel-Durchlauf. Insbesondere schwierige Wege zu allen Marken, das Timing der Helium-Apparaturen und mögliche Sackgassen müssen am Android-Gerät geprüft und gegebenenfalls korrigiert werden. Die Strukturprüfung ist keine Garantie, dass jeder Level tatsächlich komplett lösbar ist.

Debug-Build per GitHub Actions und nach erfolgreichem Workflow als direkte Datei unter GitHub Releases (Vorabversion `v0.10.0-preview`).

## Technische und rechtliche Angaben

Der vorläufige App-Name lautet **ORBIFLUX** (Markenprüfung vor Veröffentlichung erforderlich). Das Chrome-Türkis-Launcher-Icon bleibt ohne Schrift. Die App-ID ist weiterhin `de.sschermer.kugelspiel`, `versionCode 10`. Für den Test dieser umfassend geänderten Karten startet ein eigener Level-Fortschritt unter `level_008`; ältere gespeicherte Fortschrittsdaten bleiben unabhängig davon im App-Speicher erhalten.

Alle Spielgrafiken sind selbst mit Android Canvas gezeichnet. Das Launcher-Icon ist ein eigens für dieses Projekt generiertes Chrom-Türkis-Motiv und als WebP eingebunden; Herkunft siehe `THIRD_PARTY_NOTICES.md`. Es wurden keine unklar lizenzierten Fremdassets oder Audiodateien übernommen. Die Rechte am eigenen Code und an der Grafik bleiben bis zur ausdrücklichen Lizenzentscheidung vorbehalten. Ein öffentliches GitHub-Repository ist nicht automatisch Open Source. Weitere Angaben: `THIRD_PARTY_NOTICES.md`.

Die Debug-Signierung wechselt zwischen GitHub-Actions-Builds. Falls ein Android-Update nicht installiert werden kann, kann eine Deinstallation der vorherigen Testversion erforderlich sein; dabei können lokale Daten verloren gehen. Ein beständiger privater Signierschlüssel darf nie ins öffentliche Repository.
