package de.sschermer.kugelspiel;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Pure-Java structural and authored-design regression suite.
 * It is NOT a physics playthrough; dynamic puzzle solvability still needs QA.
 */
public final class LevelRouteTest {
    private static final float T=LevelData.TILE;
    private static void ok(boolean condition,String details) {
        if(!condition)throw new AssertionError(details);
    }
    private static int col(float x){return (int)Math.floor(x/T);}
    private static int row(float y){return (int)Math.floor(y/T);}
    private static boolean close(float a,float b){return Math.abs(a-b)<.01f;}
    private static float floor(int lane){return (6+5*lane)*T;}
    private static float ceil(int lane){
        return lane==0?2*T:floor(lane-1)+LevelData.DIVIDER_THICKNESS;
    }
    private static int lane(float y,LevelData.Level w) {
        for(int n=0;n<w.stories;n++)
            if(y>=ceil(n)-12 && y<=floor(n)+10)return n;
        return -1;
    }
    private static boolean[][] flood(LevelData.Level w) {
        boolean[][] seen=new boolean[w.rows][w.columns];
        Queue<int[]> q=new ArrayDeque<>();
        q.add(new int[]{col(w.startX),row(w.startY)});
        while(!q.isEmpty()){
            int[] a=q.remove();
            int x=a[0],y=a[1];
            if(x<0||y<0||x>=w.columns||y>=w.rows ||
                    seen[y][x]||w.solid(x,y))continue;
            seen[y][x]=true;
            q.add(new int[]{x+1,y});
            q.add(new int[]{x-1,y});
            q.add(new int[]{x,y+1});
            q.add(new int[]{x,y-1});
        }
        return seen;
    }
    private static boolean inPassable(boolean[][] seen,LevelData.Level w,float x,float y){
        int c=col(x),r=row(y);
        if(c<0||c>=w.columns||r<0||r>=w.rows)return false;
        if(seen[r][c])return true;
        // Center of a ball on the open underside of a thin shared floor.
        if(r>=6&&(r-6)%5==0&&y>=r*T+LevelData.DIVIDER_THICKNESS)
            return r+1<w.rows&&seen[r+1][c];
        return false;
    }
    private static boolean closesRoom(float top,float bottom,LevelData.Level w){
        for(int n=0;n<w.stories;n++)
            if(close(top,ceil(n))&&close(bottom,floor(n)))return true;
        return false;
    }
    private static void requireQuietSpaceLimited(LevelData.Level w,int stage){
        // Any floor corridor spanning more than 12 consecutive tiles without
        // an actual decision, collectible, platform, mechanism, door, drop or
        // danger is likely dead space in a puzzle level.
        for(int lane=0;lane<w.stories;lane++){
            boolean[] event=new boolean[w.columns];
            for(LevelData.Item i:w.items) {
                if(lane(i.y,w)!=lane)continue;
                event[col(i.x)]=true;
            }
            for(LevelData.Gate g:w.gates)
                if(close(g.top,ceil(lane)))event[col(g.x)]=true;
            for(LevelData.ScoreGate g:w.scoreGates)
                if(close(g.top,ceil(lane)))event[col(g.x)]=true;
            for(LevelData.Shaft shaft:w.shafts)
                if(close(shaft.top,floor(lane))) {
                    int a=Math.max(0,col(shaft.x1)),z=Math.min(w.columns-1,col(shaft.x2-1));
                    for(int c=a;c<=z;c++)event[c]=true;
                }
            for(int c=3;c<w.columns-3;c++){
                for(int r=(int)(ceil(lane)/T);r<=w.rows-1&&r<=w.stories*5+6;r++){
                    if(r>=w.rows)break;
                    if((w.tiles[r][c]=='X' || (w.tiles[r][c]=='#' &&
                            r!=6+lane*5 && r!=1+lane*5)) &&
                            r>= (int)(ceil(lane)/T) && r<=6+lane*5){
                        event[c]=true;break;
                    }
                }
            }
            int max=0,cur=0;
            for(int c=4;c<w.columns-4;c++){
                if(event[c])cur=0;
                else {cur++;max=Math.max(max,cur);}
            }
            ok(max<=13,"Level "+stage+" storey "+lane+" has "+max+
                    " consecutive empty tiles; replace flat corridor with puzzle.");
        }
    }

    private static void check(LevelData.Level w,int stage) {
        ok(w.columns>=25&&w.columns<=72,"Excessively stretched level "+stage);
        ok(w.rows>=10&&w.stories>=1,"Bad size in "+stage);
        ok(w.target==w.pointsAvailable(),"Exit not locked to ALL points in "+stage);
        ok(w.collectibleCount()>0,"No collectibles in "+stage);
        ok(!w.shafts.isEmpty()||stage<=2,"Advanced map lacks passages "+stage);
        int points=0,ones=0,fives=0,tens=0,ups=0,downs=0,fills=0,drains=0;
        for(LevelData.Item item:w.items) {
            if(item.type==LevelData.POINT) {
                points+=item.value;
                if(item.value==1)ones++;
                else if(item.value==5)fives++;
                else if(item.value==10)tens++;
                else ok(false,"Invalid point token in "+stage);
            } else if(item.type==LevelData.GRAVITY){
                if(item.value==LevelData.UP)ups++;else if(item.value==LevelData.DOWN)downs++;
                else ok(false,"Invalid gravity state");
            } else if(item.type==LevelData.HELIUM_FILL){
                fills++;
                ok(item.value>0&&item.value<w.target,"Helium score threshold unachievable");
            } else if(item.type==LevelData.HELIUM_DRAIN)drains++;
            else ok(item.type==LevelData.COLOR,"Invalid item type in "+stage);
        }
        ok(points==w.target,"Point total mismatch "+stage);
        ok(ones>0&&fives>0,"All maps need low and mid-value point tokens "+stage);
        if(stage>=4)ok(tens>=2,"Advanced maps need worthwhile rewards "+stage);
        ok(ups==downs,"No gravity recovery item in "+stage);
        ok(fills==drains,"Helium machine lacks a reverse socket in "+stage);

        if(stage>=4&&stage<=6 || stage==9){
            ok(ups==0&&fills==0,"Stage "+stage+" has unneeded helium/gravity");
        }
        if(stage==7){
            ok(ups==1&&downs==1 && fills==0,"Gravity corridor needs a single purposeful reversal pair");
            ok(w.shafts.stream().filter(s->!s.safe).count()>=4,
                    "Gravity corridor missing lengthy deadly floor");
            ok(w.items.stream().anyMatch(i->i.type==LevelData.POINT &&
                    i.y<floor(1)-2*T && lane(i.y,w)==1),
                    "Ceiling traversal has no necessary points");
        }
        if(stage==8){
            ok(ups==0&&fills==1&&drains==1,"Helium factory has an irrelevant mechanic");
            verifyHeliumChimney(w,2,35,49,36,48,1);
            ok(w.items.stream().anyMatch(i->i.type==LevelData.POINT &&
                    lane(i.y,w)==1 && i.y<floor(1)-2*T),
                    "Helium loft missing a collectible");
        }
        if(stage==10){
            ok(ups==1&&downs==1&&fills==1&&drains==1,
                    "Final maze mechanics need a specific puzzle role");
            verifyHeliumChimney(w,4,41,50,42,49,3);
        }
        if(stage>=4)requireQuietSpaceLimited(w,stage);

        // Validate exact 19-unit divider geometry, bidirectional openings.
        for(LevelData.Shaft shaft:w.shafts){
            ok(shaft.x1<shaft.x2&&close(shaft.bottom-shaft.top,
                    LevelData.DIVIDER_THICKNESS),"Bad thin opening in "+stage);
            ok(shaft.x1>2*T && shaft.x2<(w.columns-2)*T,
                    "Passage breaks outer wall in "+stage);
            int c0=col(shaft.x1+1),c1=col(shaft.x2-1),r=row(shaft.top+2);
            for(int c=c0;c<=c1;c++) {
                ok(!w.solid(c,r),"Passage obstructed from above "+stage);
                ok(!w.solid(c,r+1),"Passage obstructed from below "+stage);
            }
        }
        // Gate geometry must completely separate floor and ceiling.
        for(LevelData.Gate g:w.gates)
            ok(closesRoom(g.top,g.bottom,w),"Color gate can be jumped "+stage);
        for(LevelData.ScoreGate g:w.scoreGates){
            ok(closesRoom(g.top,g.bottom,w),"Point gate can be bypassed vertically "+stage);
            ok(g.threshold>0&&g.threshold<w.target,
                    "Point gate cannot be opened before finishing whole level "+stage);
        }
        boolean[][] open=flood(w);
        ok(inPassable(open,w,w.exitX,w.exitY),"Exit disconnected in structural maze "+stage);
        for(LevelData.Item item:w.items)
            ok(inPassable(open,w,item.x,item.y),
                    "Collectible or mechanic outside traversable floor in "+
                    stage+" at "+item.x/T+","+item.y/T);
        ok(w.columns > 2*w.stories+16,"Layout too cramped for intentional paths");
        if(stage>=4) {
            Set<String> positions=new HashSet<>();
            for(LevelData.Item item:w.items) {
                if(item.type!=LevelData.POINT)continue;
                String key=Math.round(item.x/T*10)+"_"+Math.round(item.y/T*10);
                ok(positions.add(key),"Overlapping point rewards in "+stage);
            }
        }
        System.out.println("Level "+stage+": "+w.name+", "+w.columns+"x"+w.rows+
                ", points "+w.target+"/"+w.collectibleCount()+
                " marks, openings "+w.shafts.size()+", score doors "+w.scoreGates.size()+
                ", gravity "+ups+"/"+downs+", helium "+fills+"/"+drains);
    }
    /**
     * The ascent region is a two-level, enclosed vertical chimney. With
     * the full middle floor removed, a 250%-height jump alone cannot reach
     * the high reward; the reachable HE+ machine is the designed solution.
     */
    private static void verifyHeliumChimney(LevelData.Level w,int gapLane,
            int leftWall,int rightWall,int leftGap,int rightGap,int topLane) {
        for(int lane=gapLane-1;lane<=gapLane;lane++){
            int row=(int)(floor(lane)/T);
            for(int h=1;h<=4;h++) {
                ok(w.tiles[row-h][leftWall]=='X'&&w.tiles[row-h][rightWall]=='X',
                        "Helium chimney must have full-height side walls");
            }
        }
        int floorRow=(int)(floor(gapLane)/T);
        for(int col=leftGap;col<=rightGap;col++) {
            ok(w.tiles[floorRow][col]=='.',
                    "Helium chimney has an intermediate landing ledge");
        }
        int upperRow=(int)(floor(topLane)/T);
        boolean upperOpen=false;
        for(int col=leftGap;col<=rightGap;col++)
            if(w.tiles[upperRow][col]=='.')upperOpen=true;
        ok(upperOpen,"Helium chimney upper chamber has no opening");
        boolean prize=false;
        for(LevelData.Item i:w.items)
            if(i.type==LevelData.POINT &&
                    lane(i.y,w)==topLane &&
                    i.y<floor(topLane)-2*T && i.x>=leftGap*T && i.x<=rightGap*T)
                prize=true;
        ok(prize,"Helium-only ceiling chamber has no functional point reward");
    }

    private static void authoredStories() {
        LevelData.Level caves=LevelData.LEVELS.get(3);
        LevelData.Level tower=LevelData.LEVELS.get(4);
        LevelData.Level split=LevelData.LEVELS.get(5);
        ok(caves.columns==44&&caves.stories==3,"Cave map should be compact and branch");
        ok(tower.columns==49&&tower.stories==4,"Tower map should be vertical");
        ok(split.columns==53&&split.stories==4,"Split world must have distinct regions");
        ok(split.scoreGates.size()==4,"All split levels must be gated");
        int left=0,right=0;
        for(LevelData.Item item:split.items){
            if(item.type!=LevelData.POINT)continue;
            if(item.x<27.5f*T)left+=item.value;else right+=item.value;
        }
        ok(left>=20 && left<split.target && right>0,
                "Split world's first half must unlock second but cannot finish");
        ok(split.exitX<27.5f*T,"Split exit must require returning from right");
        for(LevelData.ScoreGate g:split.scoreGates)
            ok(g.threshold==20&&close(g.x,27.5f*T),
                    "A score door allows bypassing the split-world lock");
    }
    public static void main(String[] args) {
        ok(LevelData.LEVELS.size()==10,"Expected 10 levels");
        int lastWidth=0;
        for(int i=0;i<10;i++) {
            LevelData.Level w=LevelData.LEVELS.get(i);
            ok(w.columns>lastWidth,"Map sizes must rise slowly, never be stretched");
            check(w,i+1);
            lastWidth=w.columns;
        }
        authoredStories();
        System.out.println("All structural and purposeful-mechanic checks passed. Device QA still needed.");
    }
}
