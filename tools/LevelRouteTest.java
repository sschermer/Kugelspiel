package de.sschermer.kugelspiel;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Static safety checks for a physical map, run in CI before Android compilation.
 * Flood fill checks walkable void connectivity; it does not claim to simulate
 * gravity, player timing, or whether the game is enjoyable on a phone.
 */
public final class LevelRouteTest {
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private static int col(float x){return (int)Math.floor(x/LevelData.TILE);}
    private static int row(float y){return (int)Math.floor(y/LevelData.TILE);}
    private static float TILE(){return LevelData.TILE;}
    private static boolean close(float a,float b){return Math.abs(a-b)<0.01f;}
    private static float roomFloor(int lane){return (6+5*lane)*LevelData.TILE;}
    private static float roomCeiling(int lane){
        return lane==0?2*LevelData.TILE:roomFloor(lane-1)+LevelData.DIVIDER_THICKNESS;
    }
    private static boolean gateSealsFullRoom(float top,float bottom,LevelData.Level level){
        for(int lane=0;lane<level.stories;lane++)
            if(close(top,roomCeiling(lane)) && close(bottom,roomFloor(lane)))return true;
        return false;
    }
    private static boolean inWalkableArea(boolean[][] open,LevelData.Level w,int c,int r,float y){
        if(open[r][c])return true;
        // A shared floor/ceiling occupies only 19 units of a 58-unit
        // tile; the space below its underside is physically open.
        if(r>=6 && (r-6)%5==0 && y>=r*LevelData.TILE+LevelData.DIVIDER_THICKNESS){
            return r+1<w.rows && open[r+1][c];
        }
        return false;
    }
    private static boolean[][] reachable(LevelData.Level level){
        boolean[][] seen=new boolean[level.rows][level.columns];
        Queue<int[]> queue=new ArrayDeque<>();
        queue.add(new int[]{col(level.startX),row(level.startY)});
        while(!queue.isEmpty()){
            int[] n=queue.remove();
            int x=n[0],r=n[1];
            if(x<0||r<0||x>=level.columns||r>=level.rows||
                seen[r][x]||level.solid(x,r))continue;
            seen[r][x]=true;
            queue.add(new int[]{x+1,r});queue.add(new int[]{x-1,r});
            queue.add(new int[]{x,r+1});queue.add(new int[]{x,r-1});
        }
        return seen;
    }
    private static void check(LevelData.Level w,int i){
        require(w.columns>=25 && w.rows>=10,"Dimensions "+i);
        require(w.pointsAvailable()>=w.target,"Target exceeds available points "+i);
        require(w.target>0,"Target is zero "+i);
        require(w.items.size()>=w.stories*3,"Sparse item distribution "+i);
        require(w.shafts.stream().filter(s->s.safe).count()>=(w.stories-1),
            "Missing main floor connectors "+i);
        if(i>=4)require(w.shafts.stream().filter(s->s.safe).count()>=(w.stories-1)*2,
            "Advanced maze must offer alternate routes "+i);
        if(i>=6)require(w.shafts.stream().anyMatch(s->!s.safe),
            "Later levels should include warned lethal pits "+i);
        for(LevelData.Shaft s:w.shafts){
            require(s.x2>s.x1 && s.bottom>s.top,"Malformed shaft "+i);
            require(close(s.bottom-s.top,LevelData.DIVIDER_THICKNESS),
                    "Shaft is thicker than the shared divider in level "+i);
            float opening=s.x2-s.x1;
            if(i>=4 && i<=6 && s.safe) {
                require(opening>=3f*LevelData.TILE && opening<=9f*LevelData.TILE,
                    "Handcrafted opening outside 3..9 tile range in level "+i);
            }else{
                require(close(opening,3f*LevelData.TILE),
                    "Standard 3-tile shaft width changed in level "+i);
            }
            require(s.x1>2*LevelData.TILE && s.x2<(w.columns-2)*LevelData.TILE,
                "Shaft intersects outer wall "+i);
            int a=col(s.x1+2),b=col(s.x2-2),r=row(s.top+2);
            for(int c=a;c<=b;c++){
                require(!w.solid(c,r),"Shaft floor blocked at "+i);
                if(s.safe)require(!w.solid(c,r+1),"Safe shaft ceiling blocked at "+i);
            }
        }
        boolean[][] open=reachable(w);
        require(open[row(w.exitY)][col(w.exitX)],"Exit not connected in tile maze "+i);
        for(LevelData.Gate gate:w.gates){
            require(gateSealsFullRoom(gate.top,gate.bottom,w),
                    "Colored gate can be jumped over in level "+i+" at "+gate.x);
        }
        for(LevelData.ScoreGate gate:w.scoreGates){
            require(gate.threshold>0 && gate.threshold<=w.pointsAvailable(),
                    "Inter-room point gate requires unobtainable points at "+i);
            require(gateSealsFullRoom(gate.top,gate.bottom,w),
                    "Point door can be jumped over in level "+i+" at "+gate.x);
        }
        int fills=0,drains=0,stairs=0;
        int all=0,ones=0,fives=0,tens=0;
        for(LevelData.Item item:w.items){
            int c=col(item.x),r=row(item.y);
            require(c>=0&&r>=0&&c<w.columns&&r<w.rows,"Item outside map "+i);
            require(inWalkableArea(open,w,c,r,item.y),
                    "Item unreachable in physical thin-tile geometry "+i+" at "+c+","+r);
            if(item.type==LevelData.HELIUM_FILL){
                fills++;
                require(item.value>=6 && item.value<=7*(w.stories-1),
                        "Helium must only unlock after enough early-room points");
            } else if(item.type==LevelData.HELIUM_DRAIN){
                drains++;
            }
            if(item.type==LevelData.POINT){
                require(item.value==1||item.value==5||item.value==10,
                    "Invalid score item "+i);
                all+=item.value;
                if(item.value==1)ones++;
                if(item.value==5)fives++;
                if(item.value==10)tens++;
            }
        }
        require(all>=w.target && ones>0 && fives>0,"Point distribution insufficient "+i);
        require(fills==drains,"Each helium filler needs a return-to-normal drain");
        if(i>=6) {
            require(fills>=1,"Later mazes should contain a gated helium alternative");
            require(w.scoreGates.size()>=1,"Later mazes should contain score doors");
        }
        if(i>=4)require(tens>=w.stories,"Advanced stages require optional ten-pointers "+i);
        if(i>=4 && i<=6) {
            // Distinct authored levels must contain at least one real full-block
            // rock and one collapsed gallery wider than regular passage holes.
            int rock=0,slabs=0;
            for(int r=0;r<w.rows;r++)for(int c=2;c<w.columns-2;c++){
                if(w.tiles[r][c]=='X')rock++;
                if(w.tiles[r][c]=='#' && r>1)slabs++;
            }
            require(rock>=1,"Handcrafted level has no physical rock "+i);
            require(w.shafts.stream().anyMatch(s->s.safe && s.x2-s.x1>=4*TILE()),
                "Handcrafted level has no broad partial-floor opening "+i);
            require(slabs>=2,"Handcrafted level has no functional intermediate platforms "+i);
        }
        int[][] count=new int[w.rows][w.columns];
        for(LevelData.Shaft s:w.shafts){
            int lane=(row(s.top+1)-6)/5;
            if(lane>=0&&lane<w.stories-1 && s.safe)count[lane][0]++;
        }
        if(i>=4)for(int lane=0;lane<w.stories-1;lane++)
            require(count[lane][0]>=2,"Non-branching floor "+lane+" in stage "+i);
        for(int lane=1;lane<w.stories;lane++){
            require(close(roomCeiling(lane)-roomFloor(lane-1),
                    LevelData.DIVIDER_THICKNESS),
                    "Storey has a hidden thick gap in level "+i);
        }
        if(i==4 || i==5 || i==6)checkAuthoredPuzzle(w,i);
        System.out.println("Stage "+i+" | "+w.columns+" x "+w.rows+
            " tiles | "+w.stories+" floors | safe shafts "+w.shafts.stream().filter(s->s.safe).count()+
            " | lethal pits "+w.shafts.stream().filter(s->!s.safe).count()+
            " | available "+all+" / required "+w.target);
    }
    private static void checkAuthoredPuzzle(LevelData.Level w,int stage) {
        if(stage==4){
            require(w.columns==56 && w.stories==3 && w.shafts.size()>=8,
                    "Cave must have three floors and varied cross-connecting routes");
            require(w.name.equals("Verzweigte Höhlen"),"Cave design name changed");
            require(w.scoreGates.stream().anyMatch(g->g.threshold==16),
                    "Cave must include a 16-point chamber");
            int pre=0;
            for(LevelData.Item item:w.items)
                if(item.type==LevelData.POINT && item.x<37.5f*TILE()
                        && item.y<roomFloor(0))pre+=item.value;
            require(pre>=16,"Cave first room cannot open its score gate");
        }else if(stage==5) {
            require(w.columns==66 && w.stories==4,"Tower requires four vertical floors");
            require(w.name.equals("Der Sprungturm"),"Tower design name changed");
            require(w.items.stream().anyMatch(t->t.type==LevelData.HELIUM_FILL),
                    "Tower needs optional helium ascent");
            require(w.scoreGates.stream().anyMatch(g->g.threshold==18),
                    "Tower needs an 18-point middle gate");
            // The 26->28 platform is intentionally disconnected by a missing step.
            int r=(6-3);
            require(w.tiles[r][26]=='#' && w.tiles[r][24]=='.',
                    "Tower's missing stair has become continuous");
        }else {
            require(w.columns==78 && w.stories==4,"Two-world layout changed");
            require(w.name.equals("Zwei Welten"),"Split world name changed");
            require(w.scoreGates.size()==4,"Both worlds must be separated across all floors");
            int gateX=col(w.scoreGates.get(0).x);
            for(LevelData.ScoreGate door:w.scoreGates){
                require(col(door.x)==gateX && door.threshold==28,
                        "A door allows bypassing the 28-point partition");
                require(gateSealsFullRoom(door.top,door.bottom,w),
                        "A split-world door does not reach floor or ceiling");
            }
            int left=0,right=0;
            for(LevelData.Item token:w.items){
                if(token.type!=LevelData.POINT)continue;
                if(token.x<gateX*TILE())left+=token.value;
                else right+=token.value;
            }
            require(left>=28 && left<w.target,
                    "Split-world left side must open door but cannot finish alone");
            require(left+right>=w.target && right>=10,
                    "Split-world right side must supply meaningful points");
            require(w.exitX<gateX*TILE(),
                    "Split-world player should need to return to left exit");
        }
    }

    public static void main(String[] args){
        require(LevelData.LEVELS.size()==10,"10 levels required");
        int previousWidth=0,previousStories=0;
        for(int i=0;i<10;i++){
            LevelData.Level w=LevelData.LEVELS.get(i);
            require(w.columns>previousWidth,"Levels must increase in width");
            require(w.stories>=previousStories,"Levels must not shrink vertically");
            if(i>=3)require(w.stories>=3,"No real labyrinth after tutorial");
            check(w,i+1);previousWidth=w.columns;previousStories=w.stories;
        }
        System.out.println("All structural maze checks passed. Device playtesting still required.");
    }
}
