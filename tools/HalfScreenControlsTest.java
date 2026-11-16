package de.sschermer.kugelspiel;

public final class HalfScreenControlsTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void close(float actual,float expected,String message){
        if(Math.abs(actual-expected)>0.0001f)
            throw new AssertionError(message+": "+actual+" != "+expected);
    }
    private static void frozenForFrames(HalfScreenControls c,int frames,long startTime,
                                        float initialSpeed,String message){
        float x=0,velocity=initialSpeed;
        for(int i=0;i<frames;i++){
            int direction=c.direction(startTime+i*8L);
            velocity=HorizontalMotion.step(velocity,direction,true,c.bothSidesHeld(),
                    1f/120f,532f);
            x+=velocity/120f;
            close(velocity,0f,message+" velocity frame "+i);
            close(x,0f,message+" displacement frame "+i);
        }
    }
    public static void main(String[] args){
        HalfScreenControls c=new HalfScreenControls();
        check(!c.press(1,HalfScreenControls.RIGHT,1000,true),"initial press only steers");
        check(c.direction(1099)==0,"100 ms starting delay remains active");
        check(c.direction(1100)==HalfScreenControls.RIGHT,"movement starts at 100 ms");

        // A fast second touch triggers jump and blocks sideways movement indefinitely.
        check(c.press(2,HalfScreenControls.LEFT,1090,false),"fast opposite touch jumps");
        check(c.bothSidesHeld(),"both zones detected");
        check(c.isVerticalPair(),"fast pair gets correction");
        frozenForFrames(c,360,1100,532f,"fast pair three-second hold");
        c.release(2,4300);
        check(!c.bothSidesHeld(),"release unlocks");
        check(c.direction(4300)==HalfScreenControls.RIGHT,"direction resumes");
        c.reset();

        // Previously broken case: second finger arrives far after 180 ms.
        check(!c.press(10,HalfScreenControls.LEFT,5000,true),"initial slow press");
        check(c.direction(5500)==HalfScreenControls.LEFT,"one finger steers");
        check(c.press(11,HalfScreenControls.RIGHT,5700,false),"late opposite press jumps");
        check(!c.isVerticalPair(),"late pair has no takeoff rewind");
        check(c.bothSidesHeld(),"late pair still locks movement");
        frozenForFrames(c,600,5710,-532f,"late pair five-second hold");
        c.release(10,10750);
        check(!c.bothSidesHeld(),"releasing lead unlocks");
        check(c.direction(10750)==HalfScreenControls.RIGHT,"remaining side takes control");
        c.reset();

        // Moving ball gets stopped by a second finger; it never accelerates under a hold.
        c.press(20,HalfScreenControls.RIGHT,12000,false);
        check(c.direction(12000)==HalfScreenControls.RIGHT,"moving input immediate");
        check(c.press(21,HalfScreenControls.LEFT,12300,false),"moving opposite touch jumps");
        frozenForFrames(c,720,12300,532f,"moving ball six-second freeze");
        c.release(21,19000);
        check(c.direction(19000)==HalfScreenControls.RIGHT,"movement resumes only after release");
        c.reset();

        // 100ms movement delay and 180ms takeoff correction are independent.
        c.press(30,HalfScreenControls.LEFT,20000,true);
        check(c.direction(20099)==0,"99 ms delay");
        check(c.direction(20100)==HalfScreenControls.LEFT,"100 ms motion");
        check(c.press(31,HalfScreenControls.RIGHT,20180,false),"180 ms jump");
        check(c.isVerticalPair(),"180 ms takeoff position corrected");
        frozenForFrames(c,90,20180,100f,"180 ms pair freeze");
        c.reset();
        c.press(40,HalfScreenControls.LEFT,21000,true);
        check(c.press(41,HalfScreenControls.RIGHT,21181,false),"181 ms jump");
        check(!c.isVerticalPair(),"181 ms no rewind");
        check(c.direction(21200)==0,"181 ms nevertheless blocks movement");
        c.reset();

        // Two fingers on the same side should not jump or freeze; dragging changes zones.
        c.press(50,HalfScreenControls.LEFT,22000,false);
        check(!c.press(51,HalfScreenControls.LEFT,22030,false),"same-side extra finger no jump");
        check(!c.bothSidesHeld(),"same side is not a dual-zone lock");
        check(c.direction(22050)==HalfScreenControls.LEFT,"same-side steering");
        c.move(51,HalfScreenControls.RIGHT);
        check(c.bothSidesHeld(),"crossing middle activates dual-zone lock");
        frozenForFrames(c,240,22100,90f,"drag-created pair freeze");
        c.release(51,24300);
        check(c.direction(24300)==HalfScreenControls.LEFT,"dragged pointer release unlocks");
        c.reset();

        // Existing quick same-side retap still triggers a jump.
        c.press(60,HalfScreenControls.RIGHT,25000,false);
        c.release(60,25050);
        check(c.press(61,HalfScreenControls.RIGHT,25200,true),"quick retap jumps");
        c.release(61,25210);
        check(!c.press(62,HalfScreenControls.RIGHT,25600,true),"slow retap only steers");
        c.reset();
        check(!c.press(63,HalfScreenControls.LEFT,25800,false),"reset clears tap history");

        System.out.println("HalfScreenControlsTest OK: movement delay, 2-finger jump, long hold, "
                +"late arrival, momentum cancellation, release, drag, retap");
    }
}
