package de.sschermer.kugelspiel;

import java.util.*;

/**
 * Symbolic reachability regression tests.
 * Each horizontal track is connected via an explicit gravity item.
 * Jump timing is deliberately not needed to solve any puzzle.
 * The solver models persistent color/gravity state and repeatable contact items.
 */
public final class LevelRouteTest {
    private static final class Node {
        final float x;
        final LevelData.Item item;
        final LevelData.Gate gate;
        Node(float x,LevelData.Item item,LevelData.Gate gate){this.x=x;this.item=item;this.gate=gate;}
    }
    private static final class State {
        final int pos,color,gravity;
        final boolean back;
        State(int pos,int color,int gravity,boolean back) {
            this.pos=pos;this.color=color;this.gravity=gravity;this.back=back;
        }
        @Override public int hashCode(){return Objects.hash(pos,color,gravity,back);}
        @Override public boolean equals(Object other) {
            if(!(other instanceof State))return false;
            State s=(State)other;
            return pos==s.pos&&color==s.color&&gravity==s.gravity&&back==s.back;
        }
    }
    private static void check(LevelData.Level level,int index) {
        ArrayList<Node> nodes=new ArrayList<>();
        nodes.add(new Node(100,null,null));
        for(LevelData.Item i:level.items)nodes.add(new Node(i.x,i,null));
        for(LevelData.Gate g:level.gates)nodes.add(new Node(g.x,null,g));
        nodes.add(new Node(level.finish,null,null));
        nodes.sort(Comparator.comparingDouble(n->n.x));
        HashSet<Float> unique=new HashSet<>();
        for(Node n:nodes){
            if(!unique.add(n.x))throw new AssertionError("Two objects at x="+n.x);
            if(n.x<100||n.x>level.finish)throw new AssertionError("Object outside level");
        }
        Queue<State> queue=new ArrayDeque<>();
        HashSet<State> visited=new HashSet<>();
        queue.add(new State(0,LevelData.NONE,LevelData.DOWN,false));
        boolean found=false,foundWithBacktrack=false;
        int iterations=0;
        while(!queue.isEmpty()) {
            State state=queue.remove();
            if(!visited.add(state))continue;
            if(++iterations>20000)throw new AssertionError("Solver runaway");
            if(state.pos==nodes.size()-1) {
                found=true;
                if(state.back)foundWithBacktrack=true;
                continue;
            }
            for(int delta:new int[]{-1,1}) {
                int newPos=state.pos+delta;
                if(newPos<0||newPos>=nodes.size())continue;
                Node here=nodes.get(state.pos),there=nodes.get(newPos);
                // A colored gate applies to both directions and cannot be crossed
                // without its matching color, except on a different track.
                LevelData.Gate gate=there.gate!=null?there.gate:here.gate;
                if(gate!=null&&gate.blocks(state.color,state.gravity))continue;
                int c=state.color,g=state.gravity;
                if(there.item!=null&&there.item.lane==g) {
                    if(there.item.type==LevelData.COLOR_ITEM)c=there.item.value;
                    if(there.item.type==LevelData.GRAVITY_ITEM)g=there.item.value;
                }
                queue.add(new State(newPos,c,g,state.back||delta<0));
            }
        }
        if(!found)throw new AssertionError("No route for "+(index+1)+" "+level.title);
        if(index>=4&&!foundWithBacktrack)throw new AssertionError("Expected backtrack: "+level.title);
        System.out.println("Level "+(index+1)+": "+level.title+" / reachable; explored "+iterations+" states");
    }
    public static void main(String[] args) {
        if(LevelData.LEVELS.size()!=10)throw new AssertionError("Expected 10 levels");
        for(int i=0;i<LevelData.LEVELS.size();i++)check(LevelData.LEVELS.get(i),i);
        System.out.println("Ten curated levels pass symbolic state/path checks.");
    }
}
