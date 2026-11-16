package de.sschermer.kugelspiel;

import java.util.HashMap;

/**
 * Invisible two-zone touch control. The first held finger steers; an additional
 * finger on the opposite side triggers one jump. As long as pointers occupy
 * opposite halves, horizontal input MUST remain blocked regardless of timing.
 * A 100 ms start delay and a 180 ms takeoff-position correction are separate.
 */
final class HalfScreenControls {
    static final int LEFT=1,RIGHT=2;
    // First touch starts movement only after 100 ms when the ball is at rest.
    static final long MOVEMENT_DELAY_MS=100L;
    // Human two-thumb contacts can differ slightly more than the delay.
    // A recent opposite touch still becomes a vertical jump, with x/vx
    // restored to the original takeoff position by GameView.
    static final long VERTICAL_PAIR_GRACE_MS=180L;
    private static final long RETAP_WINDOW_MS=260L;
    private final HashMap<Integer,Integer> sides=new HashMap<>();
    private int lead=-1,lastSide=0;
    private long lastUp=-1,firstDown=-1;
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
        }
        sides.put(pointer,side);
        if(lead<0)lead=pointer;
        if(opposite&&pendingStandstill&&eventTime>=firstDown
                &&eventTime-firstDown<=VERTICAL_PAIR_GRACE_MS){
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
        Integer side=sides.remove(pointer);
        if(side==null)return;
        if(sides.isEmpty()){
            lead=-1;lastSide=side;lastUp=eventTime;
            firstDown=-1;pendingStandstill=false;verticalPair=false;
        }else{
            if(lead==pointer)lead=sides.keySet().iterator().next();
            lastSide=0;lastUp=-1;
            if(!bothSides())verticalPair=false;
        }
    }
    int direction(long now){
        Integer side=sides.get(lead);
        if(side==null)return 0;
        // Never allow the lead finger to accelerate while both halves are held.
        // This lock is independent of the 180-ms takeoff-correction window.
        if(bothSides())return 0;
        if(pendingStandstill&&now>=firstDown
                &&now-firstDown<MOVEMENT_DELAY_MS)return 0;
        return side;
    }
    /** Hard movement lock whenever pointers occupy both half-screen zones. */
    boolean bothSidesHeld(){return bothSides();}
    /** Only used to undo the short start drift before a near-simultaneous jump. */
    boolean isVerticalPair(){return verticalPair&&bothSides();}
    private boolean bothSides(){
        return sides.containsValue(LEFT)&&sides.containsValue(RIGHT);
    }
    void reset(){
        sides.clear();lead=-1;lastSide=0;lastUp=-1;firstDown=-1;
        pendingStandstill=false;verticalPair=false;
    }
}
