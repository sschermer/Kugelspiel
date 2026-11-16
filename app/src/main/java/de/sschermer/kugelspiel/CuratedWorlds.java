package de.sschermer.kugelspiel;

/**
 * Individually authored stages 7-10. None is made by repeating a lane template.
 * Special mechanics are placed at specific puzzle obstacles, not at intervals.
 */
final class CuratedWorlds {
    private static final float T=LevelData.TILE;
    private static float x(float c){return c*T;}
    private static float floor(LevelData.Builder b,int lane){return b.floorY(lane)-33;}
    private static float roof(LevelData.Builder b,int lane){return b.ceilingY(lane)+33;}
    private static void p(LevelData.Builder b,int lane,float c,int value){
        b.point(x(c),floor(b,lane),value);
    }
    private static void high(LevelData.Builder b,int lane,float c,int value){
        b.point(x(c),roof(b,lane),value);
    }
    private static void dye(LevelData.Builder b,int lane,float c,int color){
        b.color(x(c),floor(b,lane),color);
    }
    private static void gate(LevelData.Builder b,int lane,float c,int color){
        b.gates.add(new LevelData.Gate(x(c),b.ceilingY(lane),b.floorY(lane),color));
    }
    private static void door(LevelData.Builder b,int lane,float c,int minimum){
        b.scoreGates.add(new LevelData.ScoreGate(x(c),b.ceilingY(lane),b.floorY(lane),minimum));
    }
    private static void ledge(LevelData.Builder b,int lane,int col,int width,int height,int reward){
        int row=b.floor(lane)-height;
        for(int j=col;j<col+width;j++)b.grid[row][j]='#';
        if(reward>0)b.point(x(col+width*.5f),row*T-29,reward);
    }
    private static void block(LevelData.Builder b,int lane,int col,int height){
        for(int h=1;h<=height;h++)b.grid[b.floor(lane)-h][col]='X';
    }
    private static void gallery(LevelData.Builder b,int lane,int first,int last){
        if(lane>=b.stories-1)throw new IllegalArgumentException("last floor gallery");
        for(int j=first;j<=last;j++) {
            b.grid[b.floor(lane)][j]='.';
            b.grid[b.roof(lane+1)][j]='.';
        }
        b.shafts.add(new LevelData.Shaft(first*T,(last+1)*T,
                b.floorY(lane),b.ceilingY(lane+1),true));
    }
    private static void save(LevelData.Builder b,int lane,float c){
        b.checkpoints.add(new float[]{x(c),b.floorY(lane)-22});
    }
    private static void gravityUp(LevelData.Builder b,int lane,float c){
        b.gravity(x(c),floor(b,lane),LevelData.UP);
    }
    private static void gravityDownCeiling(LevelData.Builder b,int lane,float c){
        b.gravity(x(c),roof(b,lane),LevelData.DOWN);
    }
    private static void helium(LevelData.Builder b,int lowLane,float c,float drainX,int highLane){
        b.heliumStation(lowLane,x(c),true);
        b.items.add(new LevelData.Item(x(drainX),roof(b,highLane),
                LevelData.HELIUM_DRAIN,0));
    }

    /** 7: long broken hazardous floor; reverse gravity is the safe ceiling route. */
    static LevelData.Level gravityCorridor(){
        LevelData.Builder b=new LevelData.Builder(56,4,1,
                "Die Umkehrkammer","Der Boden ist unterbrochen. Der sichere Weg liegt oben.");
        b.shaft(0,13,true);b.shaft(0,44,true);gallery(b,0,34,38);
        b.shaft(1,10,true);b.shaft(1,48,true);
        b.shaft(2,15,true);b.shaft(2,45,true);gallery(b,2,24,28);

        p(b,0,6.5f,1);p(b,0,10.5f,5);
        ledge(b,0,18,3,2,10);
        dye(b,0,26.5f,LevelData.ORANGE);gate(b,0,32.5f,LevelData.ORANGE);
        p(b,0,41.5f,5);block(b,0,46,2);p(b,0,49.5f,1);

        p(b,1,5.5f,1);gravityUp(b,1,15.5f);
        // The first ceiling entrance is BEHIND the inversion switch.
        // Twelve adjacent lethal tiles cannot be crossed by a 250% jump.
        for(int c=23;c<=32;c+=3)b.lethalPit(1,c);
        high(b,1,27.5f,10);high(b,1,32.5f,5);
        gravityDownCeiling(b,1,39.5f);
        p(b,1,40.5f,5);block(b,1,43,1);p(b,1,46.5f,1);save(b,1,43.5f);

        p(b,2,8.5f,1);p(b,2,16.5f,5);
        ledge(b,2,20,3,2,10);
        block(b,2,30,2);
        dye(b,2,33.5f,LevelData.BLUE);gate(b,2,40.5f,LevelData.BLUE);
        p(b,2,47.5f,5);save(b,2,35.5f);

        p(b,3,8.5f,5);ledge(b,3,14,2,2,10);
        p(b,3,25.5f,1);block(b,3,30,1);
        p(b,3,35.5f,5);p(b,3,46.5f,1);p(b,3,50.5f,5);
        return b.build();
    }

    /**
     * 8: two-storey ascent through an aligned, narrow lift shaft.
     * The HE+ socket is on the third lower floor, a ceiling reward
     * and HE- socket are in the high room. No decorative gravity switches.
     */
    static LevelData.Level heliumWorks(){
        LevelData.Builder b=new LevelData.Builder(62,5,1,
                "Die Heliumfabrik","Bringe die Kugel über zwei Etagen zum Deckenraum.");
        b.shaft(0,12,true);b.shaft(0,54,true);gallery(b,0,24,29);
        b.shaft(1,19,true);b.shaft(1,40,true);b.shaft(1,54,true);
        b.shaft(2,14,true);gallery(b,2,36,48);b.shaft(2,52,true);
        // A sealed two-storey machine chimney: no intermediary landing
        // can be used to stack-jump through its >250%-jump height.
        for(int l=1;l<=2;l++){block(b,l,35,4);block(b,l,49,4);}
        b.shaft(3,11,true);b.shaft(3,50,true);gallery(b,3,23,27);

        p(b,0,6.5f,1);p(b,0,15.5f,5);
        ledge(b,0,19,3,2,10);
        p(b,0,32.5f,5);dye(b,0,35.5f,LevelData.BLUE);
        block(b,0,39,2);
        gate(b,0,46.5f,LevelData.BLUE);p(b,0,54.5f,5);

        p(b,1,7.5f,1);p(b,1,16.5f,5);
        ledge(b,1,27,3,2,10);
        p(b,1,36.5f,1);
        // Mandatory helium loft point: over the hazardous floor gap.
        b.lethalPit(1,44);
        high(b,1,44.5f,10);p(b,1,50.5f,5);p(b,1,56.5f,1);
        save(b,1,32.5f);

        p(b,2,8.5f,5);p(b,2,17.5f,1);
        ledge(b,2,24,3,2,10);
        dye(b,2,28.5f,LevelData.ORANGE);
        gate(b,2,54.5f,LevelData.ORANGE);
        p(b,2,52.5f,5);p(b,2,57.5f,1);

        p(b,3,8.5f,5);p(b,3,18.5f,1);
        block(b,3,21,2);
        ledge(b,3,30,3,2,10);
        p(b,3,36.5f,5);
        helium(b,3,40.5f,47.5f,1); // climbs via aligned shafts at c40
        p(b,3,45.5f,1);p(b,3,55.5f,5);
        save(b,3,34.5f);

        p(b,4,7.5f,1);p(b,4,15.5f,5);
        ledge(b,4,21,3,2,10);
        p(b,4,34.5f,5);block(b,4,38,1);
        p(b,4,44.5f,1);p(b,4,54.5f,5);
        return b.build();
    }

    /** 9: three different color keys, with loops and score doors. No helium/gravity. */
    static LevelData.Level colorLocks(){
        LevelData.Builder b=new LevelData.Builder(66,5,1,
                "Die Schlüsselfarben","Farbwahl und Rückwege bestimmen den Zugang.");
        b.shaft(0,11,true);b.shaft(0,54,true);gallery(b,0,35,39);
        b.shaft(1,15,true);b.shaft(1,47,true);gallery(b,1,25,29);
        b.shaft(2,10,true);b.shaft(2,51,true);
        b.shaft(3,20,true);b.shaft(3,56,true);gallery(b,3,36,41);

        p(b,0,6.5f,1);dye(b,0,13.5f,LevelData.BLUE);
        ledge(b,0,20,3,2,10);gate(b,0,30.5f,LevelData.BLUE);
        p(b,0,42.5f,5);block(b,0,45,4);p(b,0,55.5f,1);
        save(b,0,46.5f);

        p(b,1,8.5f,5);dye(b,1,19.5f,LevelData.ORANGE);
        p(b,1,23.5f,1);gate(b,1,35.5f,LevelData.ORANGE);
        ledge(b,1,40,3,2,10);block(b,1,52,3);p(b,1,56.5f,5);

        p(b,2,7.5f,1);block(b,2,12,4);p(b,2,16.5f,5);
        dye(b,2,21.5f,LevelData.VIOLET);
        door(b,2,32.5f,18);p(b,2,38.5f,10);
        gate(b,2,45.5f,LevelData.VIOLET);
        p(b,2,56.5f,5);save(b,2,48.5f);

        p(b,3,8.5f,1);dye(b,3,14.5f,LevelData.BLUE);
        gate(b,3,24.5f,LevelData.BLUE);
        ledge(b,3,29,2,3,10);
        block(b,3,43,2);
        p(b,3,45.5f,5);p(b,3,57.5f,1);
        b.lethalPit(3,51);

        p(b,4,7.5f,5);dye(b,4,17.5f,LevelData.ORANGE);
        ledge(b,4,25,3,2,10);
        block(b,4,34,2);
        gate(b,4,39.5f,LevelData.ORANGE);
        p(b,4,46.5f,5);block(b,4,52,1);
        p(b,4,57.5f,1);p(b,4,60.5f,5);
        return b.build();
    }

    /**
     * 10: final interconnected maze with a purposeful ceiling traverse
     * and a separate two-story helium ascent for a ceiling reward.
     */
    static LevelData.Level finalLabyrinth(){
        LevelData.Builder b=new LevelData.Builder(72,6,1,
                "Das Resonanzlabyrinth",
                "Jede Marke zählt. Finde den Weg zwischen zwei Maschinen.");
        b.shaft(0,12,true);b.shaft(0,59,true);gallery(b,0,21,25);
        b.shaft(1,15,true);b.shaft(1,54,true);
        // No intermediate roof opening across the later ceiling-travel puzzle.
        b.shaft(2,12,true);b.shaft(2,59,true);
        b.shaft(3,18,true);b.shaft(3,45,true);b.shaft(3,62,true);
        b.shaft(4,13,true);gallery(b,4,42,49);gallery(b,4,25,30);
        // Another enclosed two-level helium chimney. Ordinary 250%
        // jumps cannot bridge both stories without a landing ledge.
        for(int l=3;l<=4;l++){block(b,l,41,4);block(b,l,50,4);}

        p(b,0,6.5f,1);p(b,0,13.5f,5);
        ledge(b,0,17,3,2,10);
        dye(b,0,28.5f,LevelData.BLUE);gate(b,0,35.5f,LevelData.BLUE);
        p(b,0,44.5f,5);block(b,0,48,2);p(b,0,55.5f,1);p(b,0,64.5f,5);

        p(b,1,7.5f,5);
        ledge(b,1,20,3,2,10);
        block(b,1,24,3);
        dye(b,1,28.5f,LevelData.ORANGE);
        gate(b,1,36.5f,LevelData.ORANGE);
        p(b,1,40.5f,1);p(b,1,50.5f,5);p(b,1,63.5f,1);

        p(b,2,7.5f,1);
        gravityUp(b,2,18.5f);
        block(b,2,21,2); // rise before crossing the stone obstruction
        // Invert after the rear shaft; cross a twelve-tile deadly floor
        // entirely on the underside, then find the reverse switch.
        for(int c=27;c<=36;c+=3)b.lethalPit(2,c);
        high(b,2,30.5f,10);high(b,2,35.5f,5);
        gravityDownCeiling(b,2,47.5f);
        p(b,2,44.5f,1);p(b,2,53.5f,5);
        save(b,2,47.5f);

        p(b,3,8.5f,5);p(b,3,15.5f,1);
        ledge(b,3,24,3,2,10);
        block(b,3,28,2);
        door(b,3,36.5f,25);
        p(b,3,40.5f,1);p(b,3,51.5f,5);p(b,3,64.5f,5);
        save(b,3,56.5f);

        p(b,4,7.5f,1);p(b,4,18.5f,5);
        block(b,4,21,2);
        ledge(b,4,34,3,2,10);
        p(b,4,40.5f,1);p(b,4,54.5f,5);p(b,4,59.5f,1);

        p(b,5,8.5f,5);p(b,5,16.5f,1);
        ledge(b,5,26,3,2,10);
        block(b,5,33,2);
        p(b,5,38.5f,5);
        helium(b,5,45.5f,49.5f,3);
        // Extra ceiling mark, reached while rising through lane 4 into lane 3.
        high(b,3,47.5f,10);
        p(b,5,54.5f,1);p(b,5,64.5f,5);
        return b.build();
    }
    private CuratedWorlds(){}
}
