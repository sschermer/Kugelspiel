package de.sschermer.kugelspiel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Curated data for ten increasingly long puzzle levels.
 * Coordinates are world units, not screen pixels.
 *
 * Gravity/color switches are persistent and repeatable.
 * Point tokens score only once per level; their inactive shells remain visible.
 */
final class LevelData {
    static final int NONE=0, BLUE=1, AMBER=2, VIOLET=3;
    static final int UP=-1, DOWN=1;
    static final int CEILING=-1, MID=0, FLOOR=1, BOTH=2;
    static final int COLOR=1, GRAVITY=2, POINT=3;
    static final float TOP=170f, BOTTOM=670f;

    static final class Item {
        final float x, y;
        final int lane, type, value;
        Item(float x, float y, int lane, int type, int value) {
            this.x=x; this.y=y; this.lane=lane; this.type=type; this.value=value;
        }
        boolean once() {return type==POINT;}
    }
    static final class Gate {
        final float x;
        final int lane,color;
        Gate(float x,int lane,int color) {this.x=x;this.lane=lane;this.color=color;}
        boolean blocks(int currentColor) {return currentColor!=color;}
    }
    static final class Segment {
        final float from,to,y;
        Segment(float from,float to,float y){this.from=from;this.to=to;this.y=y;}
    }
    static final class Hazard {
        final float x,base;
        Hazard(float x,float base){this.x=x;this.base=base;}
    }
    static final class Branch {
        final float x;
        final int lower,upper;
        final boolean gated,challenging;
        Branch(float x,int lower,int upper,boolean gated,boolean challenging){
            this.x=x;this.lower=lower;this.upper=upper;this.gated=gated;this.challenging=challenging;
        }
    }
    static final class Level {
        final String name,tip;
        final float finish;
        final int target;
        final List<Item> items;
        final List<Gate> gates;
        final List<Segment> gaps,ledges;
        final List<Hazard> spikes;
        final List<Float> checkpoints;
        final List<Branch> branches;
        Level(Builder b){
            name=b.name; tip=b.tip; finish=b.finish; target=b.target;
            items=Collections.unmodifiableList(new ArrayList<>(b.items));
            gates=Collections.unmodifiableList(new ArrayList<>(b.gates));
            gaps=Collections.unmodifiableList(new ArrayList<>(b.gaps));
            ledges=Collections.unmodifiableList(new ArrayList<>(b.ledges));
            spikes=Collections.unmodifiableList(new ArrayList<>(b.spikes));
            checkpoints=Collections.unmodifiableList(new ArrayList<>(b.checkpoints));
            branches=Collections.unmodifiableList(new ArrayList<>(b.branches));
        }
        int availablePoints(){
            int score=0;for(Item item:items)if(item.type==POINT)score+=item.value;
            return score;
        }
    }
    static final class Builder {
        final String name,tip;
        final float finish;
        final int target;
        final List<Item> items=new ArrayList<>();
        final List<Gate> gates=new ArrayList<>();
        final List<Segment> gaps=new ArrayList<>(),ledges=new ArrayList<>();
        final List<Hazard> spikes=new ArrayList<>();
        final List<Float> checkpoints=new ArrayList<>();
        final List<Branch> branches=new ArrayList<>();
        Builder(String name,String tip,float finish,int target){
            this.name=name;this.tip=tip;this.finish=finish;this.target=target;
        }
        Builder point(float x,int lane,int value){
            float y=lane==FLOOR?BOTTOM-40:lane==CEILING?TOP+40:BOTTOM-122;
            items.add(new Item(x,y,lane,POINT,value));return this;
        }
        Builder switchColor(float x,int lane,int value){
            items.add(new Item(x,lane==CEILING?TOP+44:BOTTOM-44,lane,COLOR,value));return this;
        }
        Builder switchGravity(float x,int lane,int value){
            items.add(new Item(x,lane==CEILING?TOP+44:BOTTOM-44,lane,GRAVITY,value));return this;
        }
        Builder gate(float x,int lane,int color) {
            gates.add(new Gate(x,lane,color));return this;
        }
        Builder pit(float from,float to) {
            gaps.add(new Segment(from,to,BOTTOM));return this;
        }
        Builder ledge(float from,float to,float top) {
            ledges.add(new Segment(from,to,top));return this;
        }
        Builder spike(float x) {
            spikes.add(new Hazard(x,BOTTOM));return this;
        }
        Builder checkpoint(float x) {
            checkpoints.add(x);return this;
        }
        Builder branch(float x,boolean advanced,boolean hazard,boolean upperRich,int variant) {
            int topReward=upperRich?10:5, bottomReward=upperRich?5:10;
            branches.add(new Branch(x,bottomReward,topReward,advanced,hazard));
            // The floor gravity switch can be bypassed with a generous jump.
            // This permits two genuinely different paths through the room.
            switchGravity(x+95,FLOOR,UP);
            if(advanced) {
                int floorColor=variant%3==0?AMBER:variant%3==1?VIOLET:BLUE;
                int roofColor=variant%3==0?BLUE:variant%3==1?AMBER:VIOLET;
                switchColor(x+310,FLOOR,floorColor);
                switchColor(x+390,CEILING,roofColor);
                gate(x+610,FLOOR,floorColor);
                gate(x+610,CEILING,roofColor);
            }
            point(x+475,FLOOR,bottomReward);
            point(x+485,CEILING,topReward);
            // On the upper route the explicit down switch joins both paths.
            switchGravity(x+705,CEILING,DOWN);
            if(hazard) {
                // Short, forgiving gap, plus reachable optional bonus ledge.
                pit(x+548,x+613);
                ledge(x+496,x+677,BOTTOM-91);
                point(x+605,MID,5);
                spike(x+778);
            }
            checkpoint(x+915);
            point(x+992,FLOOR,1);
            return this;
        }
        Level build(){return new Level(this);}
    }

    static final List<Level> LEVELS=create();

    private static List<Level> create() {
        List<Level> a=new ArrayList<>();
        a.add(new Builder("Punktelandung",
                "Sammle mindestens 6 Punkte. Jedes Punkte-Item zählt nur einmal.",1100,6)
                .point(270,FLOOR,1).point(595,FLOOR,5).point(860,FLOOR,1).build());

        a.add(new Builder("Farbiges Tor",
                "Eine starke Farbe öffnet das passende Tor. Punkte öffnen den Ausgang.",1380,6)
                .switchColor(255,FLOOR,BLUE).point(385,FLOOR,1)
                .gate(650,BOTH,BLUE).point(860,FLOOR,5)
                .point(1130,FLOOR,1).build());

        a.add(new Builder("Oben oder unten?",
                "Am Bodenschalter geht es zur Decke. Überspringen führt unten weiter.",1780,10)
                .point(250,FLOOR,1)
                .branch(440,false,false,true,0).build());

        a.add(new Builder("Zwei Spuren",
                "Beide Wege haben eigene Farb-Items und Tore.",2050,10)
                .point(250,FLOOR,1)
                .branch(500,true,false,true,1).build());

        a.add(new Builder("Der doppelte Weg",
                "Entscheide bei jeder Abzweigung neu; Sammelwege lohnen sich.",3000,18)
                .point(240,FLOOR,1)
                .branch(370,true,false,true,2)
                .branch(1580,true,false,false,3).build());

        a.add(new Builder("Schlucht und Steg",
                "Ein Sprung über die Schlucht erreicht zusätzliche Punkte.",3230,21)
                .point(245,FLOOR,1)
                .branch(370,true,false,true,4)
                .branch(1640,true,true,false,5).build());

        a.add(new Builder("Drei Entscheidungen",
                "Sichere Wege sind möglich, aber nicht immer punktreich.",4100,28)
                .point(240,FLOOR,1)
                .branch(355,true,false,false,6)
                .branch(1540,true,false,true,7)
                .branch(2725,true,true,true,8).build());

        a.add(new Builder("Umkehrpunkte",
                "Nutze die unveränderten Schalter, um fehlende Punkte nachzuholen.",4390,30)
                .point(235,FLOOR,1)
                .branch(350,true,true,true,9)
                .branch(1625,true,false,false,10)
                .branch(2890,true,true,true,11).build());

        a.add(new Builder("Vier Routen",
                "Verschiedene Wege liefern verschieden viele Punkte.",5490,37)
                .point(245,FLOOR,1)
                .branch(350,true,false,true,12)
                .branch(1590,true,true,false,13)
                .branch(2840,true,false,true,14)
                .branch(4100,true,true,false,15).build());

        a.add(new Builder("Der große Irrgarten",
                "Erkunde die Wege, wähle deine Farben und erreiche 42 Punkte.",6030,42)
                .point(245,FLOOR,1)
                .branch(350,true,true,false,16)
                .branch(1720,true,false,true,17)
                .branch(3070,true,true,false,18)
                .branch(4430,true,true,true,19).build());

        return Collections.unmodifiableList(a);
    }
    private LevelData(){}
}
