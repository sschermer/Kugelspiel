package de.sschermer.kugelspiel;

/**
 * Three individually designed worlds, not repetitions of the stage template.
 * All positions are authored in tile coordinates. Their distinct layouts are
 * described by separate explicit methods so future changes stay deliberate.
 */
final class HandcraftedLevels {
    private static final float T=LevelData.TILE;

    private static float x(float c){return c*T;}
    private static float floor(LevelData.Builder b,int lane){return b.floorY(lane)-33;}
    private static float roof(LevelData.Builder b,int lane){return b.ceilingY(lane)+33;}

    private static void token(LevelData.Builder b,int lane,float column,int value){
        b.point(x(column),floor(b,lane),value);
    }
    private static void ceilingToken(LevelData.Builder b,int lane,float column,int value){
        b.point(x(column),roof(b,lane),value);
    }
    private static void pigment(LevelData.Builder b,int lane,float column,int color){
        b.color(x(column),floor(b,lane),color);
    }
    private static void polarity(LevelData.Builder b,int lane,float column,int gravity){
        b.gravity(x(column),floor(b,lane),gravity);
    }
    private static void gate(LevelData.Builder b,int lane,float column,int color){
        b.gates.add(new LevelData.Gate(x(column),b.ceilingY(lane),b.floorY(lane),color));
    }
    private static void pointsDoor(LevelData.Builder b,int lane,float column,int minPoints){
        b.scoreGates.add(new LevelData.ScoreGate(x(column),b.ceilingY(lane),
                b.floorY(lane),minPoints));
    }
    private static void ledge(LevelData.Builder b,int lane,int firstCol,int length,int height){
        int row=b.floor(lane)-height;
        for(int c=firstCol;c<firstCol+length;c++)
            if(c>2 && c<b.width-2)b.grid[row][c]='#';
    }
    /**
     * A wide naturally collapsed ceiling/floor, intentionally not the same
     * size as the default three-tile connectors. Marks exactly the opening
     * rather than drawing a falsely thick tunnel.
     */
    private static void gallery(LevelData.Builder b,int lane,int first,int last) {
        if(lane>=b.stories-1 || last<first)throw new IllegalArgumentException("gallery");
        for(int c=first;c<=last;c++) {
            b.grid[b.floor(lane)][c]='.';
            b.grid[b.roof(lane+1)][c]='.';
        }
        float top=b.floorY(lane);
        b.shafts.add(new LevelData.Shaft(first*T,(last+1)*T,top,
                b.ceilingY(lane+1),true));
    }
    /** A genuine full-height stone obstacle, not a decorative 19-unit ledge. */
    private static void rock(LevelData.Builder b,int lane,int column,int heightTiles){
        for(int h=1;h<=heightTiles;h++)
            b.grid[b.floor(lane)-h][column]='X';
    }
    private static void checkpoint(LevelData.Builder b,int lane,float column){
        b.checkpoints.add(new float[]{x(column),b.floorY(lane)-22});
    }

    /**
     * 4. CAVES: 3 floors, looping entrances and unexpected side corridors.
     * The early descent gives a very different puzzle than staying topside
     * to unlock the orange gate and the point-gated chamber.
     */
    static LevelData.Level caves(){
        LevelData.Builder b=new LevelData.Builder(56,3,29,
                "Verzweigte Höhlen",
                "Zwei Wege in die Tiefe. Orange öffnet den oberen Felsgang.");
        // Three nonuniform alternatives connect rooms 0 -> 1.
        b.shaft(0,13,true);
        b.shaft(0,40,true);
        b.shaft(0,49,true);
        // The middle level has an independent short route to the exit floor.
        b.shaft(1,11,true);
        b.shaft(1,27,true);
        b.shaft(1,47,true);
        gallery(b,0,31,35);  // wide upper cave drop, not a narrow shaft
        gallery(b,1,34,41);  // broad lower cavern changes the floor plan

        token(b,0,7.5f,1);
        token(b,0,11.5f,5);
        pigment(b,0,18.5f,LevelData.ORANGE);
        token(b,0,22.5f,10);
        rock(b,0,24,1);  // physical rock step can be jumped
        gate(b,0,29.5f,LevelData.ORANGE);
        token(b,0,36.0f,5);
        pointsDoor(b,0,37.5f,16); // alternative: descend before the door
        token(b,0,45.5f,1);
        checkpoint(b,0,44.5f);

        token(b,1,8.5f,5);
        token(b,1,17.5f,5);
        rock(b,1,19,1);
        ledge(b,1,20,4,2);
        ceilingToken(b,1,22.5f,5);
        token(b,1,25.5f,10);
        pigment(b,1,30.5f,LevelData.BLUE);
        gate(b,1,32.5f,LevelData.BLUE);
        token(b,1,42.5f,1);
        checkpoint(b,1,39.5f);

        token(b,2,13.5f,1);
        ledge(b,2,17,3,2);
        token(b,2,23.5f,5);
        rock(b,2,31,2); // 2-tile rock, boost jump or detour
        polarity(b,2,36.5f,LevelData.UP);
        ceilingToken(b,2,38.5f,10);
        b.gravity(x(40.5f),roof(b,2),LevelData.DOWN);
        token(b,2,47.5f,5);
        checkpoint(b,2,44.5f);
        return b.build();
    }

    /**
     * 5. TOWER: 4 floors with offset vertical links; repeated small stone
     * steps, a missing stair, and optional helium ascent form a vertical plan.
     * The primary path remains available without helium or precision jumps.
     */
    static LevelData.Level tower(){
        LevelData.Builder b=new LevelData.Builder(66,4,36,
                "Der Sprungturm",
                "Mehrere Etagen. Über fehlende Stufen führt die Abkürzung.");
        b.shaft(0,15,true);
        b.shaft(0,52,true);
        b.shaft(1,22,true);
        b.shaft(1,47,true);
        b.shaft(1,58,true);
        b.shaft(2,11,true);
        b.shaft(2,41,true);
        b.shaft(2,55,true);
        gallery(b,0,44,48); // optional long drop behind colored gate
        gallery(b,1,31,34); // separate open balcony
        gallery(b,2,30,35); // tower mid-floor is discontinuous

        token(b,0,7.5f,1);
        token(b,0,11.5f,5);
        ledge(b,0,18,2,1);
        ledge(b,0,22,2,2);
        // Deliberately absent step at col 24.
        ledge(b,0,26,2,3);
        b.point(x(27.0f),b.floorY(0)-3*T-24,10);
        pigment(b,0,31.5f,LevelData.VIOLET);
        gate(b,0,39.5f,LevelData.VIOLET);
        token(b,0,58.5f,5);

        token(b,1,14.5f,1);
        token(b,1,19.5f,5);
        ledge(b,1,28,5,2);
        ceilingToken(b,1,30.5f,10);
        pointsDoor(b,1,36.5f,18);
        token(b,1,42.5f,1);
        checkpoint(b,1,43.5f);
        polarity(b,1,51.5f,LevelData.UP);
        b.gravity(x(54.5f),roof(b,1),LevelData.DOWN);

        token(b,2,8.5f,1);
        token(b,2,17.5f,5);
        rock(b,2,24,2);
        ledge(b,2,32,3,2);
        token(b,2,38.5f,10);
        token(b,2,48.5f,5);
        checkpoint(b,2,46.5f);
        // Optional alternative ascent, carefully aligned with the 1->2 shaft.
        b.heliumStation(2,x(47.5f),true);   // requires 12 previously collected points
        b.items.add(new LevelData.Item(x(47.5f),roof(b,1),
                LevelData.HELIUM_DRAIN,0));

        token(b,3,12.5f,5);
        token(b,3,21.5f,1);
        ledge(b,3,33,2,1);
        ledge(b,3,36,2,3);
        b.point(x(37.5f),b.floorY(3)-3*T-24,10);
        pigment(b,3,47.5f,LevelData.BLUE);
        gate(b,3,53.5f,LevelData.BLUE);
        token(b,3,58.5f,5);
        checkpoint(b,3,54.5f);
        return b.build();
    }

    /**
     * 6. TWO WORLDS: a vertical partition in every floor. The entire
     * right-hand section is locked at 28 collected points; a minimum 48
     * required to finish makes exploring the right half indispensable.
     * Return to the left exit after scoring in the right half.
     */
    static LevelData.Level twoWorlds(){
        LevelData.Builder b=new LevelData.Builder(78,4,48,
                "Zwei Welten",
                "Links 28 Punkte suchen, rechts weitere sammeln, links zum Ziel.");
        for(int lane=0;lane<4;lane++)
            pointsDoor(b,lane,39.5f,28);

        // Left and right sides each have independent vertical loops.
        b.shaft(0,12,true);
        b.shaft(0,31,true);
        b.shaft(0,58,true);
        b.shaft(1,13,true);
        b.shaft(1,33,true);
        b.shaft(1,64,true);
        b.shaft(2,10,true);
        b.shaft(2,31,true);
        b.shaft(2,61,true);
        gallery(b,1,24,27);  // left-side collapsed balcony
        gallery(b,2,67,70);  // right-side high bonus route

        // Left section: 37 collectible points, insufficient to finish.
        token(b,0,7.5f,1);
        token(b,0,14.5f,5);
        token(b,0,21.5f,5);
        token(b,0,27.5f,10);
        ledge(b,0,24,2,2);
        token(b,1,10.5f,1);
        token(b,1,19.5f,5);
        token(b,1,30.5f,1);
        rock(b,1,23,1);
        token(b,2,9.5f,1);
        token(b,2,17.5f,5);
        token(b,2,29.5f,1);
        token(b,3,16.5f,1);
        token(b,3,25.5f,1);

        // Right section: distinct color puzzle, risk and helium detour.
        token(b,0,45.5f,1);
        pigment(b,0,49.5f,LevelData.BLUE);
        gate(b,0,57.5f,LevelData.BLUE);
        token(b,0,63.5f,10);
        token(b,1,46.5f,5);
        b.lethalPit(1,53);
        token(b,1,67.5f,1);
        token(b,2,47.5f,10);
        ledge(b,2,57,4,2);
        token(b,2,65.5f,5);
        token(b,3,50.5f,5);
        token(b,3,69.5f,1);
        token(b,3,61.5f,10); // high-value return route on the far side
        checkpoint(b,1,62.5f);
        checkpoint(b,3,34.5f);
        b.heliumStation(2,x(60.5f),true);
        b.items.add(new LevelData.Item(x(60.5f),roof(b,1),
                LevelData.HELIUM_DRAIN,0));

        return b.build();
    }

    private HandcraftedLevels(){}
}
