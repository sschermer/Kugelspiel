# ORBIFLUX

Eigenständiger nativer Android-2D-Puzzle-Platformer, **Iteration 017 / Version 0.17.0**.

## Iteration 017: Beide Berührungsverzögerungen auf 100 ms reduziert

Nach erfolgreichem Praxistest der zuvor grundlegend reparierten Zwei-Hälften-Steuerung wurden ausschließlich die beiden Zeitfenster verkürzt: **100 ms** für die anfängliche Berührung auf einem zuvor fingerfreien Bildschirm und **100 ms** für die Wiederfreigabe der Bewegungssteuerung nach dem Loslassen eines Fingers beim senkrechten Startsprung.

Die zuverlässige Erkennung des ersten Kontakts ohne Bodenkontakt- oder Geschwindigkeitsprüfung bleibt unverändert. Beim beidseitigen Kontakt innerhalb von einschließlich 100 ms entsteht der senkrechte Sprung; während beide Finger aufliegen, ist die seitliche Bewegung blockiert. Beim Loslassen eines Fingers wartet die Kugel 100 ms, dann beschleunigt sie in Richtung des verbliebenen Fingers, auch in der Luft. Nach 100 ms wird ein zusätzlicher Finger als ungebremster Bewegungssprung interpretiert. Maximalgeschwindigkeit, Sprungphysik, Drei-Tasten-Modus und Touch-Diagnose bleiben unverändert.

Die automatisierten Steuerungstests prüfen die Grenzen bei 99, 100 und 101 ms sowie die Freigabe nach dem senkrechten Sprung.

## Iteration 016: Unbedingtes 200-ms-Startfenster und sichtbare Touch-Diagnose

Der Gerätetest ergab weiterhin unmittelbare seitliche Bewegung statt des vorgesehenen senkrechten Sprungs aus dem Stand. Die bisherigen Reparaturen waren unzuverlässig, weil die Entscheidung für die Startverzögerung von der internen Bodenkontakt- bzw. Geschwindigkeitsmessung abhängig war.

**Neue verbindliche Implementierung:** Sobald in der Zwei-Hälften-Steuerung der Bildschirm von **null auf einen Finger** wechselt, beginnt **immer** ein 200-ms-Startfenster. Diese Sperre kann weder durch `surface` noch durch `vx` oder `vy` übersprungen werden. Der erste Finger setzt den horizontalen Impuls zurück; während des Fensters bleibt horizontale Bewegung gesperrt. Trifft der zweite Finger innerhalb von einschließlich 200 ms auf der gegenüberliegenden Hälfte ein, wird eine senkrechte Sprungbewegung ausgelöst. Solange beide Finger liegen bleiben, bleibt die horizontale Bewegung vollständig gesperrt.

**Nach dem senkrechten Sprung:** Wird einer der beiden Finger losgelassen, beginnt ein weiteres, eigenständiges 200-ms-Fenster. Erst danach beschleunigt die Kugel in Richtung des verbleibenden Fingers, gegebenenfalls in der Luft.

**Bewegungssprung:** Kommt der zweite Finger später als 200 ms nach dem ersten Kontakt hinzu, springt die Kugel bei unverminderter seitlicher Geschwindigkeit und weiterhin wirkender Bewegungsbeschleunigung. Für diesen Sprung gibt es auch beim Loslassen des zweiten Fingers keine Wartezeit.

**Diagnose für den Handytest:** In Debug-Builds wird im Zwei-Hälften-Modus nahe der oberen Bildschirmkante ein kleiner Hinweis mit Fingerzahl und erkanntem Modus angezeigt: `ANLAUFSPERRE`, `SENKRECHT`, `FREIGABE` oder `BEWEGUNGSSPRUNG`. Damit lassen sich aus der tatsächlichen Touch-Folge auf dem Gerät konkrete Fehler lokalisieren. Die Anzeige ist eine funktionale Testhilfe, keine dauerhafte Spielgrafik; Release-Builds zeigen sie nicht.

Der bewusst eingegangene Kompromiss ist, dass auch ein neuer erster Finger während einer bereits laufenden Ballbewegung zunächst eine 200-ms-Bewegungssperre auslöst. Das verhindert nach mehreren fehlgeschlagenen Gerätests eine unbemerkte Umgehung des senkrechten Startsprungs. Die genauen Gesten können nach dem Praxistest weiter verfeinert werden.

Die reinen Java-Tests simulieren Fingerkontakte an den Zeitgrenzen, unterschiedlich lange Zwei-Finger-Haltephasen, die 200-ms-Freigabe und ungebremste Bewegungssprünge. Die App-ID bleibt unverändert.

## Iteration 015: Stillstandserkennung für senkrechten Startsprung korrigiert

Fehleranalyse von Version 0.14.0: Die Aktivierung der anfänglichen 200-ms-Eingabesperre war an die Bedingung `surface && abs(vx)<20` gekoppelt. Meldete die Kollisionsphysik kurz keinen direkten Bodenkontakt, obwohl die Kugel sichtbar beinahe ruhte, entfiel das gesamte Startzeitfenster. Eine einzelne Berührung löste sofort seitliche Beschleunigung aus.

Jetzt wird der Stillstand anhand der tatsächlichen Geschwindigkeit erkannt: **horizontale Geschwindigkeit unter 45** und **vertikale Geschwindigkeit unter 90 Welteinheiten/Sekunde**. Die (gelegentlich flackernde) interne Bodenkontakt-Variable wird für diese Entscheidung nicht mehr benötigt. Sobald die erste Berührung bei nahezu ruhender Kugel beginnt, setzt die Logik die geringe Restgeschwindigkeit zurück und sperrt **200 ms lang jede horizontale Bewegung**. Ein zweiter Kontakt auf der gegenüberliegenden Hälfte innerhalb dieses Fensters löst einen senkrechten Sprung aus, der bei zwei gehaltenen Fingern keine Seitwärtsbewegung erlaubt. Die zweite **200-ms-Verzögerung nach Loslassen eines Fingers** bleibt erhalten. Ein zweiter Finger während bereits laufender Bewegung springt weiterhin ohne Abbremsen.

Die Physikabfrage verwendet jetzt außerdem denselben monotonic-clock-Zeitstempel für Eingaberichtung und Bewegungsblockade, sodass an der 200-ms-Grenze kein inkonsistenter Zustand entstehen kann. Neue Regressionstests prüfen die Erkennung geringer Restbewegung ohne `surface`, eine volle 200-ms-Anlaufblockade und den anschließenden vertikalen Sprung ohne horizontalen Versatz.

## Iteration 014: Zwei getrennte Eingabeverzögerungen von jeweils 200 ms

Die neue Zwei-Hälften-Steuerung unterscheidet zwei Sprungarten. Beide Verzögerungen wurden auf **200 ms** festgelegt:

- **Senkrechter Sprung aus der Ruhe:** Vor der ersten Berührung liegt kein Finger auf dem Display und die Kugel ruht. Die erste Berührung löst zunächst 200 ms lang keine Rollbewegung aus. Kommt der zweite Finger in dieser Zeit auf die gegenüberliegende Hälfte, wird ein senkrechter Sprung ausgelöst. Beide Finger gemeinsam gehalten sperren jede horizontale Bewegung. Nach dem Loslassen eines Fingers vergeht eine **zweite, separate Verzögerung von 200 ms**, erst danach beginnt die Kugel – auch in der Luft – in Richtung des verbleibenden Fingers zu beschleunigen. Bei Wegnahme des anderen Fingers gilt dieselbe Logik spiegelverkehrt.
- **Ungebremster Bewegungssprung:** Liegt ein Finger schon länger an und steuert die Kugel (oder die Kugel ist bereits in Bewegung), löst ein zusätzlicher Finger auf der Gegenseite einen Sprung aus, **ohne die Geschwindigkeit zu verändern oder das Weiterbeschleunigen zu blockieren**. Nach Wegnahme des zweiten Fingers gibt es hier **keine zusätzliche Verzögerung**.
- Bei genau 200 ms Abstand gilt die Geste noch als Startsprung; ab 201 ms als Bewegungssprung. Die anfängliche 200-ms-Bewegungsverzögerung greift nur beim Start aus dem Stand, nicht beim Betreten des Displays während die Kugel rollt. Die bisherige schnelle Loslassen-und-Wiederantippen-Geste bleibt erhalten.

Die bisherige Standstill-Erkennungsgrenze (Bodenkontakt und unter 20 Welteinheiten/Sekunde horizontale Geschwindigkeit), die doppelte Maximalgeschwindigkeit 532 und die Drei-Tasten-Steuerung bleiben erhalten. Reine Java-Tests prüfen beide 200-ms-Grenzen, das Warten nach Fingerfreigabe im Flug, sowie ungebremste Bewegungssprünge.

## Iteration 013: Zwei-Finger-Sperre für die horizontale Bewegung

**Fehler aus dem Gerätetest behoben:** Bis einschließlich Version 0.12.0 wurde die Seitwärtsbewegung bei zwei gedrückten Bildschirmhälften nur blockiert, wenn der zweite Kontakt innerhalb des 180-ms-Fensters eintraf. Bei späterer Berührung konnte die Kugel weiter beschleunigen.

Ab 0.13.0 gilt ohne zeitliche Ausnahme: **Sobald mindestens ein Finger links und mindestens ein Finger rechts gehalten wird, ist die horizontale Geschwindigkeit auf null gesetzt und bleibt auf null.** Der zweite Finger löst beim ersten Wechsel von einer auf beide Seiten genau einen Sprung aus. Wird eine Hälfte losgelassen, darf die verbleibende Seite die Kugel wieder steuern. Für einen nahezu gleichzeitig begonnenen Sprung aus dem Stand bleibt die Korrektur des kleinen Anlaufversatzes innerhalb von 180 ms bestehen. Die Bewegungsverzögerung für den ersten Finger bleibt 100 ms. Ein neuer zweiter Finger kann auch während der normalen Rollbewegung einen Sprung auslösen, blockiert dabei jedoch bis zum Loslassen jede horizontale Bewegung.

Die physikalische Sperre befindet sich nun zusätzlich zentral in der Update-Funktion für horizontale Geschwindigkeit. Mehrere Regressionstests simulieren u. a. fünf Sekunden langen Zwei-Finger-Halt mit vorheriger maximaler Geschwindigkeit in beide Richtungen. Die Menüschaltfläche reagiert im Spiel nur auf den ersten Finger der Berührungssequenz, damit der zweite Finger keinen Menüwechsel auslösen kann. Die für beide Steuerungsarten erhöhte Höchstgeschwindigkeit von 532 bleibt bestehen.

## Iteration 012: Korrektur des senkrechten Sprungs aus dem Stand

Die reine 100-ms-Verzögerung konnte bei zwei nicht exakt synchronen Daumenberührungen zu leichtem seitlichem Versatz führen. Deshalb bleibt die **Anlaufverzögerung bei 100 ms**, die Erkennung eines nahezu gleichzeitigen zweiten Fingers aus dem Stand toleriert jetzt jedoch **bis zu 180 ms**. Sobald ein solches Fingerpaar erkannt wird, werden die anfängliche Mini-Bewegung bis zum Ausgangspunkt zurückgenommen und die horizontale Geschwindigkeit auf **null** gesetzt. Solange beide Finger auf entgegengesetzten Hälften liegen, bleibt die horizontale Bewegung gesperrt. Bei einem normalen Bewegungssprung wird die Rollgeschwindigkeit nicht zurückgesetzt. Ein Restbewegungswert unter 20 Welteinheiten/Sekunde zählt aus der Bodenposition als Standstill und wird beim ersten Kontakt angehalten.

Die zwei Zeitfenster haben verschiedene Aufgaben: 100 ms schützen den Anlauf vor Fehlbewegung; zusätzliche 80 ms erkennen ungleichzeitig aufgesetzte Daumen noch als senkrechten Sprung. Bei späteren zweiten Berührungen entsteht bewusst ein Bewegungssprung. Der Android-Gerätetest bleibt notwendig.

## Iteration 011: 100-ms-Gleichzeitigkeit und doppelte Rollgeschwindigkeit

In der Steuerungsart **ZWEI HÄLFTEN** wartet die Kugel bei der ersten Bildschirmberührung aus dem horizontalen Stillstand (stehend auf einer Oberfläche, geringe Restgeschwindigkeit) 100 Millisekunden, bevor sie losrollt. Wird während dieses Fensters die gegenüberliegende Hälfte mit einem zweiten Finger berührt, entsteht ein **senkrechter Sprung aus dem Stand**; solange beide Finger gedrückt bleiben, erfolgt keine horizontale Bewegung. Wird einer losgelassen, steuert der verbleibende Finger sofort. Bei bereits rollender Kugel erfolgen Bewegung und Sprung weiter ohne Verzögerung. Die 260-ms-Schnellwiederholung für einen Sprung bleibt erhalten.

Die horizontale Spitzengeschwindigkeit wurde für **beide** Steuerungsarten von 266 auf **532 Welteinheiten/Sekunde** erhöht. Beschleunigung, Reibung, Schwerkraft und Sprungstärke bleiben für diesen Vergleich zunächst unverändert. Die Änderungen werden zusätzlich durch Java-Eingabetests und den Android-CI-Build überprüft; subjektives Handling und Level-Balance sind auf dem Smartphone zu testen.

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

Debug-Build per GitHub Actions und nach erfolgreichem Workflow als direkte Datei unter GitHub Releases (Vorabversion `v0.17.0-preview`).

## Technische und rechtliche Angaben

Der vorläufige App-Name lautet **ORBIFLUX** (Markenprüfung vor Veröffentlichung erforderlich). Das Chrome-Türkis-Launcher-Icon bleibt ohne Schrift. Die App-ID ist weiterhin `de.sschermer.kugelspiel`, `versionCode 17`. Für den Test dieser umfassend geänderten Karten startet ein eigener Level-Fortschritt unter `level_008`; ältere gespeicherte Fortschrittsdaten bleiben unabhängig davon im App-Speicher erhalten.

Alle Spielgrafiken sind selbst mit Android Canvas gezeichnet. Das Launcher-Icon ist ein eigens für dieses Projekt generiertes Chrom-Türkis-Motiv und als WebP eingebunden; Herkunft siehe `THIRD_PARTY_NOTICES.md`. Es wurden keine unklar lizenzierten Fremdassets oder Audiodateien übernommen. Die Rechte am eigenen Code und an der Grafik bleiben bis zur ausdrücklichen Lizenzentscheidung vorbehalten. Ein öffentliches GitHub-Repository ist nicht automatisch Open Source. Weitere Angaben: `THIRD_PARTY_NOTICES.md`.

Die Debug-Signierung wechselt zwischen GitHub-Actions-Builds. Falls ein Android-Update nicht installiert werden kann, kann eine Deinstallation der vorherigen Testversion erforderlich sein; dabei können lokale Daten verloren gehen. Ein beständiger privater Signierschlüssel darf nie ins öffentliche Repository.
