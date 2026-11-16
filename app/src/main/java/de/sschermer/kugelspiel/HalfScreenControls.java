package de.sschermer.kugelspiel;

import java.util.HashMap;

/**
 * State machine for invisible left/right touch zones. Never changes game physics.
 * The first held pointer determines movement; an opposite-side new pointer jumps.
 * A quick release followed by another touch on the same side also jumps.
 */
final class HalfScreenControls {
    static final int LEFT=1,RIGHT=2;
    private static final long RETAP_WINDOW_MS=260L;
    private final HashMap<Integer,Integer> sides=new HashMap<>();
    private int lead=-1,lastSide=0;
    private long lastUp=-1;

    boolean press(int pointer,int side,long eventTime){
        if(side!=LEFT&&side!=RIGHT)return false;
        boolean bothBefore=bothSides();
        boolean retap=sides.isEmpty()&&lastUp>=0&&lastSide==side
                &&eventTime>=lastUp&&eventTime-lastUp<=RETAP_WINDOW_MS;
        sides.put(pointer,side);
        if(lead<0)lead=pointer;
        lastUp=-1;lastSide=0;
        return retap||(!bothBefore&&bothSides());
    }
    void move(int pointer,int side){
        if((side==LEFT||side==RIGHT)&&sides.containsKey(pointer))sides.put(pointer,side);
    }
    void release(int pointer,long eventTime){
        Integer side=sides.remove(pointer);
        if(side==null)return;
        if(sides.isEmpty()){
            lead=-1;lastSide=side;lastUp=eventTime;
        }else if(lead==pointer){
            lead=sides.keySet().iterator().next();
            lastSide=0;lastUp=-1;
        }
    }
    int direction(){
        Integer side=sides.get(lead);
        return side==null?0:side;
    }
    private boolean bothSides(){
        return sides.containsValue(LEFT)&&sides.containsValue(RIGHT);
    }
    void reset(){
        sides.clear();lead=-1;lastSide=0;lastUp=-1;
    }
}
