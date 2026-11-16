package de.sschermer.kugelspiel;

import java.util.HashMap;

/**
 * Invisible two-half touch control.
 *
 * Every FIRST finger of a new gesture starts a guaranteed 200ms horizontal
 * movement lock. No velocity or ground-contact flags may bypass it.
 *
 * A second finger on the opposite half within 200ms produces a straight-up
 * jump and keeps horizontal motion frozen while the initial pair remains.
 * Releasing either finger starts a separate 200ms lock, then the remaining
 * finger accelerates in its half's direction, including in midair.
 *
 * If the second finger arrives after 200ms, the first finger is already
 * steering and the new jump must NOT brake or stop horizontal movement.
 */
final class HalfScreenControls {
    static final int LEFT=1,RIGHT=2;
    static final long START_DELAY_MS=200L;
    static final long AFTER_VERTICAL_RELEASE_DELAY_MS=200L;
    private static final long RETAP_WINDOW_MS=260L;

    private final HashMap<Integer,Integer> sides=new HashMap<>();
    private int lead=-1,lastSide=0;
    private long firstDown=-1,lastUp=-1,releaseDelayUntil=-1;
    private boolean pendingFirstContact=false,verticalPair=false;
    private String recentGesture="KEINE";

    boolean press(int pointer,int side,long eventTime){
        if(side!=LEFT&&side!=RIGHT)return false;
        boolean empty=sides.isEmpty();
        boolean opposite=!empty&&!sides.containsValue(side);
        boolean retap=empty&&lastUp>=0&&lastSide==side
                &&eventTime>=lastUp&&eventTime-lastUp<=RETAP_WINDOW_MS;

        if(empty){
            firstDown=eventTime;
            pendingFirstContact=true; // always, regardless of GameView physics
            verticalPair=false;
            releaseDelayUntil=-1;
            recentGesture="ERSTER FINGER";
        }
        sides.put(pointer,side);
        if(lead<0)lead=pointer;

        if(opposite){
            if(pendingFirstContact&&eventTime>=firstDown
                    &&eventTime-firstDown<=START_DELAY_MS){
                verticalPair=true;
                pendingFirstContact=false;
                recentGesture="SENKRECHTER SPRUNG";
            }else{
                recentGesture="BEWEGUNGSSPRUNG";
            }
        }else if(retap){
            recentGesture="SCHNELLER DOPPELTIPP";
        }

        lastUp=-1;lastSide=0;
        return opposite||retap;
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
            lead=-1;
            lastSide=side;lastUp=eventTime;
            firstDown=-1;pendingFirstContact=false;
            verticalPair=false;releaseDelayUntil=-1;
        }else{
            if(lead==pointer)lead=sides.keySet().iterator().next();
            lastSide=0;lastUp=-1;
            if(wasVerticalPair){
                releaseDelayUntil=eventTime+AFTER_VERTICAL_RELEASE_DELAY_MS;
                recentGesture="FREIGABE NACH SENKRECHT";
            }
            if(!bothSides())verticalPair=false;
        }
    }
    private boolean initialWait(long now){
        return !sides.isEmpty()&&pendingFirstContact&&firstDown>=0
                &&now>=firstDown&&now-firstDown<START_DELAY_MS;
    }
    private boolean releaseWait(long now){
        return !sides.isEmpty()&&releaseDelayUntil>=0
                &&now<releaseDelayUntil;
    }
    boolean blocksHorizontal(long now){
        return isVerticalPair()||initialWait(now)||releaseWait(now);
    }
    int direction(long now){
        Integer side=sides.get(lead);
        return side==null||blocksHorizontal(now)?0:side;
    }
    boolean isVerticalPair(){return verticalPair&&bothSides();}
    int pointerCount(){return sides.size();}
    String debugStatus(long now){
        if(sides.isEmpty())return "FINGER 0 | "+recentGesture;
        if(isVerticalPair())return "FINGER "+sides.size()+" | SENKRECHT | VX 0";
        if(initialWait(now)){
            long remaining=Math.max(0,START_DELAY_MS-(now-firstDown));
            return "FINGER "+sides.size()+" | ANLAUFSPERRE "+remaining+" ms";
        }
        if(releaseWait(now)){
            long remaining=Math.max(0,releaseDelayUntil-now);
            return "FINGER "+sides.size()+" | FREIGABE "+remaining+" ms";
        }
        return "FINGER "+sides.size()+" | "+
                (direction(now)==LEFT?"LINKS":"RECHTS")+" | "+recentGesture;
    }
    private boolean bothSides(){
        return sides.containsValue(LEFT)&&sides.containsValue(RIGHT);
    }
    void reset(){
        sides.clear();lead=-1;lastSide=0;lastUp=-1;firstDown=-1;
        releaseDelayUntil=-1;pendingFirstContact=false;verticalPair=false;
        recentGesture="RESET";
    }
}
