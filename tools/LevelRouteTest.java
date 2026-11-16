package de.sschermer.kugelspiel;

import java.util.List;

/**
 * Deterministic design checks. The solver enumerates one-path-per-branch
 * combinations; it does not simulate Android touch input or precise physics.
 */
public final class LevelRouteTest {
    private static void require(boolean condition,String problem) {
        if(!condition)throw new AssertionError(problem);
    }
    private static boolean near(float a,float b){return Math.abs(a-b)<0.15f;}

    private static LevelData.Item item(LevelData.Level level,int type,int lane,float x) {
        for(LevelData.Item i:level.items) {
            if(i.type==type && i.lane==lane && near(i.x,x))return i;
        }
        throw new AssertionError(level.name+": item missing at "+x);
    }
    private static LevelData.Gate gate(LevelData.Level level,int lane,float x) {
        for(LevelData.Gate g:level.gates) {
            if(g.lane==lane&&near(g.x,x))return g;
        }
        throw new AssertionError(level.name+": gate missing at "+x);
    }
    private static void structural(LevelData.Level lvl,int number) {
        require(lvl.finish>900,"Level "+number+": too short");
        require(lvl.availablePoints()>=lvl.target,
                "Level "+number+": cannot collect sufficient points");
        require(lvl.target>0,"Level "+number+": target must be positive");
        for(LevelData.Item i:lvl.items) {
            require(i.x>130 && i.x<lvl.finish-90,
                    "Level "+number+": inaccessible item at "+i.x);
            if(i.once())require(i.value==1||i.value==5||i.value==10,
                    "Level "+number+": point denomination");
        }
        for(LevelData.Gate g:lvl.gates)
            require(g.x>150&&g.x<lvl.finish-70,"Level "+number+": gate outside bounds");
        for(LevelData.Segment gap:lvl.gaps)
            require(gap.to-gap.from<=85 && gap.to>gap.from,
                    "Level "+number+": too large/invalid gap");
        for(LevelData.Branch b:lvl.branches) {
            require(b.lower!=b.upper,"Level "+number+": different path scores required");
            require(b.x+1020<lvl.finish,"Level "+number+": branch not inside world");
            require(item(lvl,LevelData.GRAVITY,LevelData.FLOOR,b.x+95).value==LevelData.UP,
                    "Upper path entry missing");
            require(item(lvl,LevelData.GRAVITY,LevelData.CEILING,b.x+705).value==LevelData.DOWN,
                    "Upper path rejoin missing");
            require(item(lvl,LevelData.POINT,LevelData.FLOOR,b.x+475).value==b.lower,
                    "Lower branch reward wrong");
            require(item(lvl,LevelData.POINT,LevelData.CEILING,b.x+485).value==b.upper,
                    "Upper branch reward wrong");
            require(item(lvl,LevelData.POINT,LevelData.FLOOR,b.x+992).value==1,
                    "Branch reunion reward missing");
            require(lvl.checkpoints.stream().anyMatch(x->near(x,b.x+915)),
                    "Branch checkpoint missing");
            if(b.gated) {
                int floorColor=item(lvl,LevelData.COLOR,LevelData.FLOOR,b.x+310).value;
                int roofColor=item(lvl,LevelData.COLOR,LevelData.CEILING,b.x+390).value;
                require(gate(lvl,LevelData.FLOOR,b.x+610).color==floorColor,
                        "Lower route gate has no matching item");
                require(gate(lvl,LevelData.CEILING,b.x+610).color==roofColor,
                        "Upper route gate has no matching item");
            }
            if(b.challenging){
                require(lvl.gaps.stream().anyMatch(g->near(g.from,b.x+548)),
                        "Expected small jump gap");
                require(lvl.ledges.stream().anyMatch(g->near(g.from,b.x+496)),
                        "Optional ledge missing");
                require(item(lvl,LevelData.POINT,LevelData.MID,b.x+605).value==5,
                        "Optional skill reward missing");
                require(lvl.spikes.stream().anyMatch(h->near(h.x,b.x+778)),
                        "Expected mild spike hazard");
            }
        }
    }
    private static void scoreRoutes(LevelData.Level lvl,int number) {
        List<LevelData.Branch> branches=lvl.branches;
        if(branches.isEmpty()){
            require(lvl.availablePoints()>=lvl.target,"Intro route cannot score enough");
            System.out.println("Level "+number+": "+lvl.name+" | intro score "
                    +lvl.availablePoints()+"/"+lvl.target);
            return;
        }
        int paths=1<<branches.size(),feasible=0,min=Integer.MAX_VALUE,max=0;
        for(int mask=0;mask<paths;mask++){
            // One starting +1 and one reunion +1 per branch.
            int points=1+branches.size();
            for(int b=0;b<branches.size();b++){
                LevelData.Branch branch=branches.get(b);
                points+=((mask&(1<<b))!=0)?branch.upper:branch.lower;
            }
            min=Math.min(min,points);max=Math.max(max,points);
            if(points>=lvl.target)feasible++;
        }
        require(max>=lvl.target,
                "Level "+number+": threshold unreachable without optional risky tokens: max="+max);
        require(min<lvl.target,
                "Level "+number+": point choices are inconsequential: min="+min);
        if(number>=5)require(feasible>=2,
                "Level "+number+": multiple viable point routes required");
        System.out.println("Level "+number+": "+lvl.name
                +" | "+feasible+"/"+paths+" simple routes open exit"
                +" | score "+min+".."+max+" / "+lvl.target
                +" | optional tokens: "+(lvl.availablePoints()-1-branches.size()*16));
    }

    public static void main(String[] args) {
        require(LevelData.LEVELS.size()==10,"Expected 10 curated levels");
        float prev=0;int previousBranches=-1;
        for(int i=0;i<LevelData.LEVELS.size();i++) {
            LevelData.Level lvl=LevelData.LEVELS.get(i);
            require(lvl.finish>prev,"Levels must grow steadily in horizontal length");
            require(lvl.branches.size()>=previousBranches,"Branch count must not decrease");
            structural(lvl,i+1);
            scoreRoutes(lvl,i+1);
            prev=lvl.finish;previousBranches=lvl.branches.size();
        }
        System.out.println("All ten levels pass geometry and score-route design checks.");
    }
}
