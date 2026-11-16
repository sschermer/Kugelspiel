package de.sschermer.kugelspiel;

public final class HalfScreenControlsTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        HalfScreenControls touch=new HalfScreenControls();

        // Pressing from standstill stays motionless for 100 ms, then steers.
        check(!touch.press(5,HalfScreenControls.RIGHT,1000,true),"first touch only steers");
        check(touch.direction(1000)==0,"0 ms: delay");
        check(touch.direction(1099)==0,"99 ms: delay");
        check(touch.direction(1100)==HalfScreenControls.RIGHT,"100 ms: steering starts");
        touch.release(5,1110);
        touch.reset();

        // Opposite finger within window jumps straight up while both remain held.
        check(!touch.press(5,HalfScreenControls.RIGHT,2000,true),"first contact");
        check(touch.press(8,HalfScreenControls.LEFT,2099,true),"99 ms opposite triggers jump");
        check(touch.isVerticalPair(),"recognized vertical-pair gesture");
        check(touch.direction(2100)==0,"both held: vertical jump, no horizontal steering");
        check(touch.direction(2600)==0,"neutral pair persists until one finger released");
        touch.release(8,2610);
        check(touch.direction(2610)==HalfScreenControls.RIGHT,"remaining lead finger steers immediately");
        check(!touch.isVerticalPair(),"released second finger unfreezes movement");
        check(touch.press(9,HalfScreenControls.LEFT,2640,false),"another opposite tap jumps");
        check(touch.direction(2640)==HalfScreenControls.RIGHT,"late second finger retains lead direction");
        touch.release(5,2650);
        check(touch.direction(2650)==HalfScreenControls.LEFT,"remaining finger takes over");
        touch.release(9,2660);
        check(touch.direction(2700)==0,"no fingers stops steering");

        // Up to 180 ms counts as a near-simultaneous stationary pair.
        // Movement itself still starts after only 100 ms.
        touch.reset();
        touch.press(1,HalfScreenControls.LEFT,3000,true);
        check(touch.press(2,HalfScreenControls.RIGHT,3100,false),"100 ms triggers jump");
        check(touch.direction(3101)==0,"100 ms remains neutral");
        touch.reset();
        touch.press(1,HalfScreenControls.LEFT,4000,true);
        check(touch.direction(4101)==HalfScreenControls.LEFT,"101 ms: first touch already moves");
        check(touch.press(2,HalfScreenControls.RIGHT,4180,false),"180 ms still jumps");
        check(touch.isVerticalPair(),"180 ms accepted as vertical jump");
        check(touch.direction(4180)==0,"180 ms freezes movement");
        touch.reset();
        touch.press(1,HalfScreenControls.LEFT,4500,true);
        check(touch.press(2,HalfScreenControls.RIGHT,4681,false),"181 ms still jumps");
        check(!touch.isVerticalPair(),"181 ms remains a rolling jump");
        check(touch.direction(4681)==HalfScreenControls.LEFT,"181 ms retains lead steering");
        touch.reset();

        // Already moving: no start-up delay, opposite finger jumps while steering.
        touch.press(11,HalfScreenControls.RIGHT,5000,false);
        check(touch.direction(5000)==HalfScreenControls.RIGHT,"moving contact is immediate");
        check(touch.press(12,HalfScreenControls.LEFT,5030,false),"opposite contact jumps while moving");
        check(touch.direction(5030)==HalfScreenControls.RIGHT,"moving jump preserves direction");
        touch.release(12,5040);
        touch.release(11,5050);

        // Quick retap remains a jump; slow retap is merely movement.
        check(touch.press(13,HalfScreenControls.RIGHT,5290,true),"quick same-side retap jumps");
        touch.release(13,5300);
        check(!touch.press(14,HalfScreenControls.RIGHT,5600,true),"slow retap not a jump");
        check(touch.direction(5600)==0,"slow retap from rest still honors delay");
        touch.move(14,HalfScreenControls.LEFT);
        check(touch.direction(5700)==HalfScreenControls.LEFT,"drag changes direction");
        touch.reset();
        check(touch.direction(5750)==0,"reset clears movement");
        check(!touch.press(15,HalfScreenControls.RIGHT,5800,false),"reset clears retap history");
        check(!touch.press(16,HalfScreenControls.RIGHT,5810,false),"same-side second touch does not jump");
        System.out.println("HalfScreenControlsTest OK");
    }
}
