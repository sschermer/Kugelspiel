package de.sschermer.kugelspiel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic, tile-based, multi-storey puzzle maps. Everything visual is functional:
 * stone blocks movement, safe turquoise shafts connect floors, red pits kill the ball.
 */
final class LevelData {
    static final int NONE=0, BLUE=1, ORANGE=2, VIOLET=3;
    static final int DOWN=1, UP=-1;
    static final int COLOR=1, GRAVITY=2, POINT=3, HELIUM_FILL=4, HELIUM_DRAIN=5;
    static final int TILE=58;
    static final int DIVIDER_THICKNESS=19; // Actual collision & visible thickness
    static final class Item {
        final float x,y; final int type,value;
        Item(float x,float y,int type,int value){this.x=x;this.y=y;this.type=type;this.value=value;}
    }
    static final class Gate {
        final float x,top,bottom; final int color;
        Gate(float x,float top,float bottom,int color){this.x=x;this.top=top;this.bottom=bottom;this.color=color;}
    }
    /** A score door checks total collected points; points are NEVER spent. */
    static final class ScoreGate {
        final float x,top,bottom;final int threshold;
        ScoreGate(float x,float top,float bottom,int points){
            this.x=x;this.top=top;this.bottom=bottom;this.threshold=points;
        }
        boolean blocks(int points){return points<threshold;}
    }
    static final class Shaft {
        final float x1,x2,top,bottom;final boolean safe;
        Shaft(float a,float b,float y1,float y2,boolean s){x1=a;x2=b;top=y1;bottom=y2;safe=s;}
    }
    static final class Level {
        final int columns, rows, stories, target;
        final String name,tip;
        final char[][] tiles;
        final List<Item> items;
        final List<Gate> gates;
        final List<ScoreGate> scoreGates;
        final List<Shaft> shafts;
        final List<float[]> checkpoints;
        final float startX,startY,exitX,exitY;
        Level(Builder b){
            columns=b.width;stories=b.stories;rows=b.rows;
            int total=0;for(Item item:b.items)if(item.type==POINT)total+=item.value;
            target=total; // NEVER a lower threshold: all collectible points are mandatory.
            name=b.name;tip=b.tip;
            tiles=new char[rows][columns];
            for(int r=0;r<rows;r++)System.arraycopy(b.grid[r],0,tiles[r],0,columns);
            items=Collections.unmodifiableList(new ArrayList<>(b.items));
            gates=Collections.unmodifiableList(new ArrayList<>(b.gates));
            scoreGates=Collections.unmodifiableList(new ArrayList<>(b.scoreGates));
            shafts=Collections.unmodifiableList(new ArrayList<>(b.shafts));
            checkpoints=Collections.unmodifiableList(new ArrayList<>(b.checkpoints));
            startX=b.startX;startY=b.startY;exitX=b.exitX;exitY=b.exitY;
        }
        boolean solid(int col,int row){
            if(col<0||col>=columns||row<0)return true;
            if(row>=rows)return false;
            return tiles[row][col]=='#' || tiles[row][col]=='X';
        }
        int collectibleCount(){int n=0;for(Item item:items)if(item.type==POINT)n++;return n;}
        int pointsAvailable(){
            int sum=0;for(Item item:items)if(item.type==POINT)sum+=item.value;
            return sum;
        }
    }
    static final class Builder {
        final int width,stories,rows,target;
        final String name,tip;
        final char[][] grid;
        final ArrayList<Item> items=new ArrayList<>();
        final ArrayList<Gate> gates=new ArrayList<>();
        final ArrayList<ScoreGate> scoreGates=new ArrayList<>();
        final ArrayList<Shaft> shafts=new ArrayList<>();
        final ArrayList<float[]> checkpoints=new ArrayList<>();
        final float startX,startY,exitX,exitY;
        Builder(int width,int stories,int target,String name,String tip){
            this.width=width;this.stories=stories;this.target=target;this.name=name;this.tip=tip;
            rows=floor(stories-1)+4;grid=new char[rows][width];
            for(int r=0;r<rows;r++){
                for(int col=0;col<width;col++)grid[r][col]='.';
                grid[r][0]='#';grid[r][1]='#';
                grid[r][width-2]='#';grid[r][width-1]='#';
            }
            for(int lane=0;lane<stories;lane++){
                for(int col=2;col<width-2;col++){
                    grid[roof(lane)][col]='#';
                    grid[floor(lane)][col]='#';
                }
            }
            startX=4.5f*TILE;
            startY=floor(0)*TILE-15;
            int endCol=(stories-1)%2==0?width-5:4;
            exitX=(endCol+.5f)*TILE;
            exitY=floor(stories-1)*TILE-92;
        }
        int floor(int lane){return 6+5*lane;} // Adjacent storeys share one divider row
        int roof(int lane){return floor(lane)-5;}
        float floorY(int lane){return floor(lane)*TILE;}
        float ceilingY(int lane){return lane==0?(roof(lane)+1)*TILE : floorY(lane-1)+DIVIDER_THICKNESS;}
        float pathX(int lane,float fraction){
            float from=5.5f, to=width-6.5f;
            return (lane%2==0?from+(to-from)*fraction:to-(to-from)*fraction)*TILE;
        }
        void point(float x,float y,int val){items.add(new Item(x,y,POINT,val));}
        void color(float x,float y,int val){items.add(new Item(x,y,COLOR,val));}
        void gravity(float x,float y,int direction){items.add(new Item(x,y,GRAVITY,direction));}
        void floorItem(int lane,float pos,int type,int value){
            float x=pathX(lane,pos),y=floorY(lane)-32;
            if(type==POINT)point(x,y,value);
            if(type==COLOR)color(x,y,value);
            if(type==GRAVITY)gravity(x,y,value);
        }
        void roofItem(int lane,float pos,int type,int value){
            float x=pathX(lane,pos),y=ceilingY(lane)+33;
            if(type==POINT)point(x,y,value);
            if(type==COLOR)color(x,y,value);
            if(type==GRAVITY)gravity(x,y,value);
        }
        void gate(int lane,float fraction,int color){
            float x=pathX(lane,fraction);
            gates.add(new Gate(x,ceilingY(lane),floorY(lane),color)); // full unobstructed room height
        }
        void scoreGate(int lane,float fraction,int threshold) {
            float x=pathX(lane,fraction);
            scoreGates.add(new ScoreGate(x,ceilingY(lane),floorY(lane),threshold));
        }
        void heliumStation(int lane,float x,boolean fill){
            float y=fill?floorY(lane)-15:ceilingY(lane)+15;
            items.add(new Item(x,y,fill?HELIUM_FILL:HELIUM_DRAIN,fill?Math.max(6,lane*6):0));
        }
        /** Three staggered functional platforms; missing middle step changes route choice. */
        void brokenStairs(int lane,float fraction,boolean ascending){
            int c=(int)(pathX(lane,fraction)/TILE);
            int dir=lane%2==0?1:-1;
            for(int step=0;step<3;step++){
                if(step==1)continue; // intentional missing step
                int col=c+dir*step;
                if(col<=2||col>=width-3)continue;
                int height=step==2?3:1;
                int row=floor(lane)-height;
                grid[row][col]='#';
            }
            float rewardX=(c+dir*2+.5f)*TILE;
            point(rewardX,(floor(lane)-3)*TILE-22,10);
        }
        void platform(int lane,float fraction,int value) {
            float x=pathX(lane,fraction);
            int from=Math.round(x/TILE)-2;
            int row=floor(lane)-2;
            for(int i=from;i<from+5;i++)
                if(i>=2&&i<width-2)grid[row][i]='#';
            point((from+2.5f)*TILE,row*TILE-29,value);
        }
        void shaft(int lane,int center,boolean safe){
            if(lane>=stories-1)return;
            int left=Math.max(3,Math.min(width-5,center-1));
            int right=left+2; // 3 tiles wide, instead of 5
            // Both structural barrier rows must be open.
            for(int col=left;col<=right;col++){
                grid[floor(lane)][col]='.';
                grid[roof(lane+1)][col]='.';
            }
            float top=floor(lane)*TILE;
            float bottom=ceilingY(lane+1); // same thin 19-unit divider
            shafts.add(new Shaft(left*TILE,(right+1)*TILE,top,bottom,safe));
        }
        void lethalPit(int lane,int col){
            int left=col-1;
            if(left<3||left+2>=width-3)return;
            for(int i=left;i<=left+2;i++) {
                grid[floor(lane)][i]='.';
                if(lane<stories-1)grid[roof(lane+1)][i]='.';
            }
            float y=floorY(lane);
            shafts.add(new Shaft(left*TILE,(left+3)*TILE,y,y+DIVIDER_THICKNESS,false));
        }
        void checkpoint(int lane,float fraction){
            checkpoints.add(new float[]{pathX(lane,fraction),floorY(lane)-22});
        }
        Level build(){return new Level(this);}
    }
    static final List<Level> LEVELS=make();

    private static List<Level> make(){
        ArrayList<Level> result=new ArrayList<>();
        int[] widths={27,35,43,56,66,78,89,100,111,125};
        int[] stories={1,1,2,3,3,4,4,5,5,6};
        String[] names={
            "Punkte und Ausgang","Farben und Tore","Durch die Öffnung",
            "Erstes Labyrinth","Der verschlungene Pfad","Unter der Oberfläche",
            "Vier Etagen","Die Umwege","Mehrere Ebenen","Der große Irrgarten"
        };
        String[] tips={
            "Schwarze Punkte sammeln. Weißlich = bereits gesammelt.",
            "Der Farbwechsler öffnet das gleichfarbige Tor.",
            "Türkis markiert einen sicheren Durchgang in die Tiefe.",
            "Mehrere Ebenen und Wege. Erkunde die Karte.",
            "Farben, Ebenen und Punkte verbinden.",
            "Rot markierte Löcher sind tödlich. Türkis ist sicher.",
            "Achte auf Wege über und unter dir.",
            "Zwischenebenen bieten zusätzliche Punkte.",
            "Suche die ergiebigen Wege zum Ausgang.",
            "Wähle deine Route und sammle genügend Punkte."
        };
        for(int index=0;index<10;index++){
            // Levels 4-6 have intentionally hand-authored, non-template geometry.
            if(index==3){result.add(HandcraftedLevels.caves());continue;}
            if(index==4){result.add(HandcraftedLevels.tower());continue;}
            if(index==5){result.add(HandcraftedLevels.twoWorlds());continue;}
            if(index==6){result.add(CuratedWorlds.gravityCorridor());continue;}
            if(index==7){result.add(CuratedWorlds.heliumWorks());continue;}
            if(index==8){result.add(CuratedWorlds.colorLocks());continue;}
            if(index==9){result.add(CuratedWorlds.finalLabyrinth());continue;}
            int height=stories[index];
            int target=(index<2?6:(height*7)+Math.max(0,height-2)*4);
            Builder b=new Builder(widths[index],height,target,names[index],tips[index]);

            for(int lane=0;lane<height;lane++){
                // Always achievable floor-route score (1+5+1).
                b.floorItem(lane,.13f,POINT,1);
                b.floorItem(lane,.43f,POINT,5);
                b.floorItem(lane,.73f,POINT,1);
                if(index>=1){
                    int color=(index+lane)%3+1;
                    b.floorItem(lane,.24f,COLOR,color);
                    b.gate(lane,.61f,color);
                }
                if(index>=2){
                    // Gravity-shift detour to ceiling; can be skipped with jump.
                    b.floorItem(lane,.31f,GRAVITY,UP);
                    b.roofItem(lane,.47f,POINT,5);
                    b.roofItem(lane,.54f,GRAVITY,DOWN);
                }
                if(index>=3) {
                    // An optional intermediate platform: a controllable jump earns 10.
                    if(index%2==1 || lane%2==0)b.brokenStairs(lane,.35f,index%2==0);
                    else b.platform(lane,.78f,10);
                }
                if(lane<height-1){
                    int mainCenter=(lane%2==0?b.width-7:6);
                    b.shaft(lane,mainCenter,true);
                    if(index>=3){
                        // Alternative connector: skip part of the corridor or return to it.
                        b.shaft(lane,b.width/2+(lane%2==0?-3:3),true);
                        // Reversible vertical exploration from the lower floor.
                        float optionalX=(b.width/2+(lane%2==0?-3:3))*TILE;
                        b.gravity(optionalX+4*TILE,b.floorY(lane+1)-35,UP);
                        b.gravity(optionalX+5*TILE,b.ceilingY(lane)+35,DOWN);
                    }
                    if(index>=5 && lane==height-2) {
                        // Bonus ascent: after collecting early points, a second way
                        // back to the upper floor is available through a helium socket.
                        // Both apparatuses require careful centering and low speed.
                        float socketX=(mainCenter+.5f)*TILE;
                        // Helium ascent is gated by points from previous rooms.
                        b.heliumStation(lane+1,socketX,true);
                        b.heliumStation(lane,socketX+3*TILE,false);
                    }
                }
                if(index>=5 && lane%2==0 && lane<height-1) {
                    // Rare, short lethal pit with physically separate red warning.
                    int col=lane%2==0?b.width/3:b.width*2/3;
                    boolean intersects=false;
                    for(Shaft s:b.shafts)
                        if(s.safe&&s.top==b.floorY(lane) && col*TILE>s.x1-3*TILE && col*TILE<s.x2+3*TILE)
                            intersects=true;
                    if(!intersects)b.lethalPit(lane,col);
                }
                if(index>=4)b.checkpoint(lane,.84f);
                    if(lane==0 || (index>=6 && lane%2==1)) {
                        // Early 1+5 points allow the inter-room door.
                        // The threshold is a permission, not a deduction.
                        b.scoreGate(lane,.55f,6);
                    }
            }
            result.add(b.build());
        }
        return Collections.unmodifiableList(result);
    }
    private LevelData(){}
}
