package de.sschermer.kugelspiel;

public final class HalfScreenControlsTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static void close(float actual,float expected,String message){
        if(Math.abs(actual-expected)>0.0001f)
            throw new AssertionError(message+" ("+actual+" vs "+expected+")");
    }
    private static float step(HalfScreenControls c,long now,float velocity,boolean surface){
        // UI direction constants (LEFT=1, RIGHT=2) are not signed physics commands.
        // Mirror GameView.syncHalfMovement before calling the physics helper.
        int side=c.direction(now);
        int command=side==HalfScreenControls.LEFT?-1:
                side==HalfScreenControls.RIGHT?1:0;
        return HorizontalMotion.step(velocity,command,surface,
                c.blocksHorizontal(now),1f/120f,532f);
    }
    public static void main(String[] args){
        HalfScreenControls c=new HalfScreenControls();

        // First touch from rest has exactly 200 ms of movement delay.
        check(!c.press(1,HalfScreenControls.RIGHT,1000,true),"first touch only steers");
        check(c.direction(1000)==0&&c.direction(1199)==0,"start delay through 199 ms");
        close(step(c,1199,35f,true),0f,"start delay cancels residual drift");
        check(c.direction(1200)==HalfScreenControls.RIGHT,"movement begins at 200 ms");
        c.reset();

        // Initial opposite-side contact within 200ms is a vertical jump.
        c.press(2,HalfScreenControls.RIGHT,2000,true);
        check(c.press(3,HalfScreenControls.LEFT,2199,false),"199 ms second finger jumps");
        check(c.isVerticalPair(),"199 ms becomes vertical pair");
        for(int i=0;i<360;i++){
            close(step(c,2200+i*8L,532f,false),0f,
                    "vertical pair remains motionless throughout jump");
        }
        // Release the original right finger while airborne. Wait exactly 200 ms.
        c.release(2,2300);
        check(!c.isVerticalPair(),"release ends vertical lock");
        check(c.direction(2300)==0&&c.direction(2499)==0,
                "left steering blocked 200 ms after release");
        close(step(c,2499,532f,false),0f,"post-release window cancels velocity");
        check(c.direction(2500)==HalfScreenControls.LEFT,
                "remaining left finger takes over exactly at 200 ms");
        check(step(c,2500,0f,false)<0f,"remaining finger accelerates left in air");
        c.reset();

        // Releasing the secondary finger leaves the original right finger.
        c.press(4,HalfScreenControls.RIGHT,3000,true);
        check(c.press(5,HalfScreenControls.LEFT,3050,false),"fast right/left pair");
        c.release(5,3090);
        check(c.direction(3289)==0,"right steering still delayed at 199 ms");
        check(c.direction(3290)==HalfScreenControls.RIGHT,"right starts at 200 ms");
        check(step(c,3290,0f,false)>0f,"rightward acceleration midair");
        c.reset();

        // Inclusive 200 ms edge is vertical; 201 ms is directional.
        c.press(6,HalfScreenControls.LEFT,4000,true);
        check(c.press(7,HalfScreenControls.RIGHT,4200,false),"200 ms second finger jumps");
        check(c.isVerticalPair(),"200 ms vertical boundary");
        c.reset();
        c.press(8,HalfScreenControls.LEFT,5000,true);
        check(c.press(9,HalfScreenControls.RIGHT,5201,false),"201 ms second finger jumps");
        check(!c.isVerticalPair(),"201 ms stays directional");
        check(c.direction(5201)==HalfScreenControls.LEFT,"first finger keeps steering");
        c.reset();

        // Moving and delayed second-finger jumping are completely unbraked.
        c.press(10,HalfScreenControls.RIGHT,6000,false);
        check(c.direction(6000)==HalfScreenControls.RIGHT,"moving first touch has no delay");
        check(c.press(11,HalfScreenControls.LEFT,6070,false),
                "already moving ball jumps even under 200ms");
        check(!c.isVerticalPair()&&c.direction(6070)==HalfScreenControls.RIGHT,
                "moving jump retains steering");
        float vx=420f;
        for(int i=0;i<500;i++){
            float old=vx;
            vx=step(c,6070+i*8L,vx,false);
            check(vx>=old,"holding second finger does not slow the rolling ball");
        }
        close(vx,532f,"maximal speed maintained with both fingers down");
        c.release(11,11000);
        check(c.direction(11000)==HalfScreenControls.RIGHT,
                "regular movement jump has no post-release delay");
        c.reset();
        c.press(12,HalfScreenControls.LEFT,12000,false);
        check(c.press(13,HalfScreenControls.RIGHT,12400,false),"left directional jump");
        check(step(c,12400,-400f,false)<-400f,"left momentum is preserved");
        c.reset();

        // Releasing both fingers clears delayed release state.
        c.press(14,HalfScreenControls.RIGHT,13000,true);
        c.press(15,HalfScreenControls.LEFT,13060,false);
        c.release(15,13100);
        c.release(14,13120);
        check(c.direction(13140)==0,"no pointers => no steering");
        check(!c.blocksHorizontal(14000),"empty display clears blocking");
        c.reset();

        // Same-side extras, dragging, quick retaps.
        c.press(16,HalfScreenControls.LEFT,15000,false);
        check(!c.press(17,HalfScreenControls.LEFT,15020,false),"same half not jump");
        c.move(17,HalfScreenControls.RIGHT);
        check(!c.isVerticalPair(),"drag does not create a vertical-start pair");
        check(c.direction(15030)==HalfScreenControls.LEFT,"lead keeps direction");
        c.release(17,15100);c.release(16,15110);
        check(c.press(18,HalfScreenControls.LEFT,15200,true),"quick retap jumps");
        c.release(18,15210);
        check(!c.press(19,HalfScreenControls.LEFT,15500,true),"slow retap is not jump");
        c.reset();

        System.out.println("HalfScreenControlsTest OK: 200ms start, 200ms airborne release,"
                +" straight jump, unbraked running jump, boundary and retap");
    }
}
