package de.sschermer.kugelspiel;

import java.util.HashMap;

/**
 * Invisible two-half steering and gesture recognition.
 *
 * From a finger-free screen and a stationary ball (judged by actual velocities,
 * not a potentially flickering ground-contact flag), the first contact has a
 * 200 ms movement delay. An opposite-side second contact within that window
 * triggers a vertical jump. After either finger is lifted, the remaining
 * finger starts steering after another 200 ms, even if the ball is airborne.
 *
 * When steering has already begun, an opposite-side second finger jumps
 * without braking or overriding the first finger's direction.
 */
final class HalfScreenControls {
    static final int LEFT=1,RIGHT=2;
    static final long START_DELAY_MS=200L;
    static final long AFTER_VERTICAL_RELEASE_DELAY_MS=200L;
    private static final long RETAP_WINDOW_MS=260L;
    private final HashMap<Integer,Integer> sides=new HashMap<>();
    private int lead=-1,lastSide=0;
    private long lastUp=-1,firstDown=-1,releaseDelayUntil=-1;
    private boolean pendingStandstill=false,verticalPair=false;

    boolean press(int pointer,int side,long eventTime,boolean startingAtRest){
        if(side!=LEFT&&side!=RIGHT)return false;
        boolean empty=sides.isEmpty();
        boolean opposite=!empty&&!bothSides()&&!sides.containsValue(side);
        boolean retap=empty&&lastUp>=0&&lastSide==side
                &&eventTime>=lastUp&&eventTime-lastUp<=RETAP_WINDOW_MS;
        if(empty){
            firstDown=eventTime;
            pendingStandstill=startingAtRest;
            verticalPair=false;
            releaseDelayUntil=-1;
        }
        sides.put(pointer,side);
        if(lead<0)lead=pointer;
        if(opposite&&pendingStandstill&&eventTime>=firstDown
                &&eventTime-firstDown<=START_DELAY_MS){
            verticalPair=true;
            pendingStandstill=false;
        }
        lastUp=-1;lastSide=0;
        return retap||opposite;
    }
    void move(int pointer,int side){
        if((side==LEFT||side==RIGHT)&&sides.containsKey(pointer)){
            sides.put(pointer,side);
            if(!bothSides())verticalPair=false;
        }
    }
    void release(int pointer,long eventTime){
        boolean wasVerticalPair=isVerticalPair();
        Integer side=sides.remove(pointer);
        if(side==null)return;
        if(sides.isEmpty()){
            lead=-1;lastSide=side;lastUp=eventTime;
            firstDown=-1;releaseDelayUntil=-1;
            pendingStandstill=false;verticalPair=false;
        }else{
            if(lead==pointer)lead=sides.keySet().iterator().next();
            lastSide=0;lastUp=-1;
            if(wasVerticalPair){
                // The delay applies only after a vertical start jump.
                releaseDelayUntil=eventTime+AFTER_VERTICAL_RELEASE_DELAY_MS;
                pendingStandstill=false;
            }
            if(!bothSides())verticalPair=false;
        }
    }
    private boolean initialWait(long now){
        return pendingStandstill&&firstDown>=0
                &&now>=firstDown&&now-firstDown<START_DELAY_MS;
    }
    private boolean releaseWait(long now){
        return !sides.isEmpty()&&releaseDelayUntil>=0
                &&now<releaseDelayUntil;
    }
    boolean blocksHorizontal(long now){
        // Unlike v0.13, a late second finger is NOT a movement lock.
        return isVerticalPair()||initialWait(now)||releaseWait(now);
    }
    int direction(long now){
        Integer side=sides.get(lead);
        return side==null||blocksHorizontal(now)?0:side;
    }
    boolean isVerticalPair(){return verticalPair&&bothSides();}
    private boolean bothSides(){
        return sides.containsValue(LEFT)&&sides.containsValue(RIGHT);
    }
    void reset(){
        sides.clear();lead=-1;lastSide=0;lastUp=-1;firstDown=-1;
        releaseDelayUntil=-1;pendingStandstill=false;verticalPair=false;
    }
}
