package de.sschermer.kugelspiel;

/**
 * Compact, purpose-authored worlds for stages 4, 5 and 6.
 * Every bonus-point item is mandatory for completion. No random special
 * mechanics: no gravity or helium apparatus appears in these three maps.
 */
final class HandcraftedLevels {
    private static final float T=LevelData.TILE;
    private static float x(float c){return c*T;}
    private static float floor(LevelData.Builder b,int lane){return b.floorY(lane)-33;}
    private static void p(LevelData.Builder b,int lane,float c,int value){
        b.point(x(c),floor(b,lane),value);
    }
    private static void dye(LevelData.Builder b,int lane,float c,int color){
        b.color(x(c),floor(b,lane),color);
    }
    private static void gate(LevelData.Builder b,int lane,float c,int color){
        b.gates.add(new LevelData.Gate(x(c),b.ceilingY(lane),b.floorY(lane),color));
    }
    private static void door(LevelData.Builder b,int lane,float c,int min){
        b.scoreGates.add(new LevelData.ScoreGate(x(c),b.ceilingY(lane),b.floorY(lane),min));
    }
    private static void ledge(LevelData.Builder b,int lane,int col,int width,int height,int reward){
        int row=b.floor(lane)-height;
        for(int j=col;j<col+width;j++)b.grid[row][j]='#';
        b.point(x(col+width*.5f),row*T-29,reward);
    }
    private static void rock(LevelData.Builder b,int lane,int col,int height){
        for(int h=1;h<=height;h++)b.grid[b.floor(lane)-h][col]='X';
    }
    private static void gallery(LevelData.Builder b,int lane,int start,int last){
        for(int c=start;c<=last;c++){
            b.grid[b.floor(lane)][c]='.';
            b.grid[b.roof(lane+1)][c]='.';
        }
        b.shafts.add(new LevelData.Shaft(start*T,(last+1)*T,b.floorY(lane),
                b.ceilingY(lane+1),true));
    }
    private static void save(LevelData.Builder b,int lane,float c){
        b.checkpoints.add(new float[]{x(c),b.floorY(lane)-22});
    }
    /** Cave loops: some items can be reached by dropping, others by doubling back. */
    static LevelData.Level caves(){
        LevelData.Builder b=new LevelData.Builder(44,3,1,
                "Die Höhlengabelung","Drei Höhlenwege. Die Farbsteine sichern Durchgänge.");
        b.shaft(0,11,true);b.shaft(0,30,true);gallery(b,0,19,23);
        b.shaft(1,9,true);b.shaft(1,27,true);gallery(b,1,33,37);
        p(b,0,6.5f,1);p(b,0,9.5f,5);
        dye(b,0,15.5f,LevelData.ORANGE);
        // Full-height fork. The upper prize chamber is reached from below
        // via the adjacent gallery, not by a long level corridor.
        rock(b,0,16,4);
        ledge(b,0,17,2,2,10);
        gate(b,0,26.5f,LevelData.ORANGE);
        p(b,0,33.5f,5);p(b,0,39.5f,1);
        p(b,1,6.5f,5);p(b,1,12.5f,1);
        rock(b,1,16,1);ledge(b,1,18,3,2,10);
        rock(b,1,23,2);
        dye(b,1,24.5f,LevelData.BLUE);
        gate(b,1,31.5f,LevelData.BLUE);
        p(b,1,39.5f,5);save(b,1,36.5f);
        p(b,2,6.5f,1);p(b,2,13.5f,5);
        ledge(b,2,16,3,3,10);
        p(b,2,24.5f,1);rock(b,2,29,2);
        p(b,2,35.5f,5);p(b,2,39.5f,1);
        return b.build();
    }
    /** Tower stairs and stack jumps, intentionally without unrelated helium/gravity. */
    static LevelData.Level tower(){
        LevelData.Builder b=new LevelData.Builder(49,4,1,
                "Das Treppenwerk","Fehlende Stufen belohnen eine gute Sprungkette.");
        b.shaft(0,14,true);b.shaft(0,38,true);gallery(b,0,28,31);
        b.shaft(1,10,true);b.shaft(1,32,true);b.shaft(1,43,true);
        b.shaft(2,13,true);b.shaft(2,36,true);gallery(b,2,23,27);

        p(b,0,6.5f,1);p(b,0,12.5f,5);
        ledge(b,0,18,2,1,1);
        ledge(b,0,22,2,2,5);
        ledge(b,0,26,2,3,10); // deliberately missing steps between ledges
        dye(b,0,33.5f,LevelData.VIOLET);gate(b,0,36.5f,LevelData.VIOLET);
        p(b,0,43.5f,5);

        p(b,1,7.5f,5);p(b,1,15.5f,1);
        ledge(b,1,20,3,2,10);
        rock(b,1,26,3);
        door(b,1,29.5f,18);
        p(b,1,34.5f,5);p(b,1,41.5f,1);save(b,1,38.5f);

        p(b,2,7.5f,1);p(b,2,17.5f,5);
        rock(b,2,20,4); // go around via two different vertical links
        ledge(b,2,28,3,3,10);
        p(b,2,34.5f,5);rock(b,2,39,1);p(b,2,44.5f,1);

        p(b,3,8.5f,5);p(b,3,18.5f,1);
        ledge(b,3,23,2,2,10);
        p(b,3,32.5f,5);rock(b,3,36,1);p(b,3,40.5f,1);p(b,3,44.5f,5);
        return b.build();
    }
    /** Split map: left has enough to open score doors; right has mandatory prizes. */
    static LevelData.Level twoWorlds(){
        LevelData.Builder b=new LevelData.Builder(53,4,1,
                "Die zwei Reiche","Erst links sammeln, dann rechts suchen und zurück.");
        for(int lane=0;lane<4;lane++)door(b,lane,27.5f,20);
        b.shaft(0,10,true);b.shaft(0,20,true);
        b.shaft(0,35,true);b.shaft(0,46,true);gallery(b,0,40,42);
        b.shaft(1,12,true);b.shaft(1,21,true);
        b.shaft(1,38,true);b.shaft(1,47,true);
        b.shaft(2,9,true);b.shaft(2,19,true);
        b.shaft(2,35,true);b.shaft(2,45,true);gallery(b,2,14,17);

        p(b,0,6.5f,1);p(b,0,13.5f,5);
        ledge(b,0,16,3,2,10);rock(b,0,21,2);p(b,0,23.5f,5);
        p(b,0,31.5f,1);dye(b,0,35.5f,LevelData.BLUE);
        gate(b,0,40.5f,LevelData.BLUE);p(b,0,46.5f,5);

        p(b,1,8.5f,1);p(b,1,16.5f,5);p(b,1,23.5f,1);rock(b,1,25,2);
        p(b,1,32.5f,5);ledge(b,1,38,3,2,10);
        b.lethalPit(1,43);p(b,1,47.5f,1);

        p(b,2,7.5f,5);p(b,2,19.5f,1);p(b,2,24.5f,5);rock(b,2,30,2);
        p(b,2,32.5f,1);p(b,2,41.5f,5);p(b,2,47.5f,1);
        save(b,2,34.5f);

        p(b,3,8.5f,1);p(b,3,15.5f,5);p(b,3,23.5f,1);rock(b,3,30,1);
        p(b,3,33.5f,5);ledge(b,3,39,3,2,10);
        p(b,3,47.5f,5);
        return b.build();
    }
    private HandcraftedLevels(){}
}
