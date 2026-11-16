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
    private static boolean[][] reachable(LevelData.Level level){
        boolean[][] seen=new boolean[level.rows][level.columns];
        Queue<int[]> queue=new ArrayDeque<>();
        queue.add(new int[]{col(level.startX),row(level.startY)});
        while(!queue.isEmpty()){
            int[] n=queue.remove(),x=n[0],r=n[1];
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
        int all=0,ones=0,fives=0,tens=0;
        for(LevelData.Item item:w.items){
            int c=col(item.x),r=row(item.y);
            require(c>=0&&r>=0&&c<w.columns&&r<w.rows,"Item outside map "+i);
            require(open[r][c],"Item unreachable in tile geometry "+i+" at "+c+","+r);
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
        if(i>=4)require(tens>=w.stories,"Advanced stages require optional ten-pointers "+i);
        int[][] count=new int[w.rows][w.columns];
        for(LevelData.Shaft s:w.shafts){
            int lane=(row(s.top+1)-6)/6;
            if(lane>=0&&lane<w.stories-1 && s.safe)count[lane][0]++;
        }
        if(i>=4)for(int lane=0;lane<w.stories-1;lane++)
            require(count[lane][0]>=2,"Non-branching floor "+lane+" in stage "+i);
        System.out.println("Stage "+i+" | "+w.columns+" x "+w.rows+
            " tiles | "+w.stories+" floors | safe shafts "+w.shafts.stream().filter(s->s.safe).count()+
            " | lethal pits "+w.shafts.stream().filter(s->!s.safe).count()+
            " | available "+all+" / required "+w.target);
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
