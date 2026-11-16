package de.sschermer.kugelspiel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java, hand-authored puzzle rules. No Android dependency: the routes can
 * be checked by the command-line test in tools/LevelRouteTest.java.
 */
final class LevelData {
    static final int NONE = 0;
    static final int BLUE = 1;
    static final int AMBER = 2;
    static final int VIOLET = 3;
    static final int DOWN = 1;
    static final int UP = -1;
    static final int BOTH = 0;
    static final int FLOOR = 1;
    static final int CEILING = -1;
    static final int COLOR_ITEM = 1;
    static final int GRAVITY_ITEM = 2;

    static final class Item {
        final float x;
        final int lane, type, value;
        Item(float x, int lane, int type, int value) {
            this.x = x; this.lane = lane; this.type = type; this.value = value;
        }
    }
    static final class Gate {
        final float x;
        final int lane, color;
        Gate(float x, int lane, int color) {
            this.x = x; this.lane = lane; this.color = color;
        }
        boolean blocks(int currentColor, int gravity) {
            return currentColor != color && (lane == BOTH || lane == gravity);
        }
    }
    static final class Level {
        final String title, clue;
        final float finish;
        final List<Item> items;
        final List<Gate> gates;
        Level(String title, String clue, float finish, List<Item> items, List<Gate> gates) {
            this.title=title; this.clue=clue; this.finish=finish;
            this.items=Collections.unmodifiableList(new ArrayList<>(items));
            this.gates=Collections.unmodifiableList(new ArrayList<>(gates));
        }
    }
    private static final class Builder {
        final String title, clue;
        final float finish;
        final List<Item> items=new ArrayList<>();
        final List<Gate> gates=new ArrayList<>();
        Builder(String name, String hint, float end) {title=name;clue=hint;finish=end;}
        Builder color(float x, int lane, int color) {items.add(new Item(x,lane,COLOR_ITEM,color));return this;}
        Builder grav(float x,int lane,int direction) {items.add(new Item(x,lane,GRAVITY_ITEM,direction));return this;}
        Builder gate(float x,int lane,int color) {gates.add(new Gate(x,lane,color));return this;}
        Level build() {return new Level(title,clue,finish,items,gates);}
    }
    static final List<Level> LEVELS = build();

    private static List<Level> build() {
        ArrayList<Level> result=new ArrayList<>();
        result.add(new Builder("Die erste Farbe", "BLAU faerbt die Kugel. Blaues Metall geht durch blaue Tore.", 980)
            .color(290,FLOOR,BLUE).gate(625,BOTH,BLUE).build());
        result.add(new Builder("Zwei Schluessel", "Farbe bleibt, bis du ein anderes Farb-Item beruehrst.", 1290)
            .color(255,FLOOR,BLUE).gate(545,BOTH,BLUE)
            .color(780,FLOOR,AMBER).gate(1050,BOTH,AMBER).build());
        result.add(new Builder("Oben ist unten", "Pfeile setzen die Schwerkraft dauerhaft nach oben oder unten.", 1160)
            .grav(260,FLOOR,UP).color(540,CEILING,BLUE)
            .gate(845,BOTH,BLUE).build());
        result.add(new Builder("Seitenwechsel", "Auf der Decke gelangst du an andere Gegenstaende.", 1540)
            .grav(255,FLOOR,UP).color(540,CEILING,BLUE).gate(770,BOTH,BLUE)
            .grav(905,CEILING,DOWN).color(1100,FLOOR,AMBER)
            .gate(1335,BOTH,AMBER).build());
        result.add(new Builder("Der Rueckweg", "Nicht jedes nuetzliche Item liegt in Laufrichtung.", 1460)
            .color(280,FLOOR,AMBER).gate(595,FLOOR,AMBER)
            .grav(760,FLOOR,UP).color(360,CEILING,BLUE)
            .gate(1180,BOTH,BLUE).build());
        result.add(new Builder("Verpasst?", "Wechsle die Seite und hole nach, was unten lag.", 1460)
            .grav(250,FLOOR,UP).grav(860,CEILING,DOWN)
            .color(520,FLOOR,AMBER).gate(1200,BOTH,AMBER).build());
        result.add(new Builder("Zwei Wege", "Ein Tor kann nur eine Seite des Raums sperren.", 1730)
            .color(255,FLOOR,AMBER).gate(580,FLOOR,AMBER)
            .grav(750,FLOOR,UP).color(370,CEILING,BLUE)
            .gate(1050,CEILING,BLUE).grav(1170,CEILING,DOWN)
            .color(1320,FLOOR,VIOLET).gate(1530,BOTH,VIOLET).build());
        result.add(new Builder("Noch einmal zurueck", "Das Farb-Item auf dem Boden bleibt erreichbar.", 1630)
            .grav(265,FLOOR,UP).color(525,CEILING,BLUE)
            .gate(790,CEILING,BLUE).grav(900,CEILING,DOWN)
            .color(605,FLOOR,AMBER).gate(1370,BOTH,AMBER).build());
        result.add(new Builder("Kreuzweg", "Farbtor und Gravitation blockieren verschiedene Spuren.", 1770)
            .color(270,FLOOR,AMBER).gate(575,FLOOR,AMBER)
            .grav(735,FLOOR,UP).color(370,CEILING,BLUE)
            .gate(1030,CEILING,BLUE).grav(1150,CEILING,DOWN)
            .color(860,FLOOR,VIOLET).gate(1510,BOTH,VIOLET).build());
        result.add(new Builder("Die grosse Schleife", "Plane den Rueckweg zweimal. Nichts verschwindet.", 2050)
            .color(235,FLOOR,AMBER).gate(550,FLOOR,AMBER)
            .grav(675,FLOOR,UP).color(360,CEILING,BLUE)
            .gate(945,CEILING,BLUE).grav(1080,CEILING,DOWN)
            .color(795,FLOOR,VIOLET).gate(1390,FLOOR,VIOLET)
            .grav(1500,FLOOR,UP).color(1200,CEILING,BLUE)
            .gate(1805,BOTH,BLUE).build());
        return Collections.unmodifiableList(result);
    }
    private LevelData() {}
}
