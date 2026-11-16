package de.sschermer.kugelspiel;

public final class HalfScreenControlsTest {
    private static void check(boolean valid,String message){
        if(!valid)throw new AssertionError(message);
    }
    private static void close(float got,float expected,String message){
        if(Math.abs(got-expected)>0.0001f)
            throw new AssertionError(message+": "+got+" != "+expected);
    }
    private static float step(HalfScreenControls c,long now,float velocity,boolean onSurface){
        int side=c.direction(now);
        int command=side==HalfScreenControls.LEFT?-1:
                    side==HalfScreenControls.RIGHT?1:0;
        return HorizontalMotion.step(velocity,command,onSurface,
                c.blocksHorizontal(now),1f/120f,399f);
    }
    private static void freeze(HalfScreenControls c,long t,int frames,float initialVx,
                                String message){
        float x=0f,vx=initialVx;
        for(int i=0;i<frames;i++){
            vx=step(c,t+i*8L,vx,false);
            x+=vx/120f;
            close(vx,0f,message+" velocity step "+i);
            close(x,0f,message+" position step "+i);
        }
    }
    public static void main(String[] args){
        HalfScreenControls c=new HalfScreenControls();

        // Regression of the actual failure: the ball's ground/velocity state
        // used to bypass the start-delay window. No such parameter exists anymore.
        check(!c.press(1,HalfScreenControls.LEFT,1000),"initial contact, no jump");
        check(c.pointerCount()==1,"one finger tracked");
        check(c.blocksHorizontal(1000),"first contact ALWAYS locked at t=0");
        check(c.blocksHorizontal(1099),"still locked at 99 ms");
        freeze(c,1030,8,399f,"mandatory first-touch freeze");
        check(c.direction(1100)==HalfScreenControls.LEFT,"movement starts at 100ms");
        c.reset();

        // A second touch at the 100ms inclusive boundary is vertical.
        c.press(2,HalfScreenControls.RIGHT,2000);
        check(c.press(3,HalfScreenControls.LEFT,2100),"opposite touch jumps");
        check(c.isVerticalPair(),"100ms is vertical even after start waiting");
        check(c.debugStatus(2250).contains("SENKRECHT"),"diagnosis shows vertical mode");
        freeze(c,2300,480,399f,"vertical pair held four seconds");
        c.release(2,6500);
        check(!c.isVerticalPair(),"one finger release ends vertical pair");
        check(c.blocksHorizontal(6599),"post-release 100ms delay at 99");
        close(step(c,6599,80f,false),0f,"release freeze cancels momentum");
        check(c.direction(6600)==HalfScreenControls.LEFT,"remaining left finger steers");
        check(step(c,6600,0f,false)<0f,"left accelerates in midair after 100ms");
        c.reset();

        // Releasing the opposite finger also resumes the lead right direction.
        c.press(4,HalfScreenControls.RIGHT,7000);
        c.press(5,HalfScreenControls.LEFT,7010);
        c.release(5,7100);
        check(c.direction(7199)==0,"right release delay still active at 99ms");
        check(c.direction(7200)==HalfScreenControls.RIGHT,"right acceleration after 100ms");
        check(step(c,7200,0f,false)>0f,"right midair acceleration");
        c.reset();

        // The old false rest/ground state must have no impact on recognition.
        c.press(6,HalfScreenControls.LEFT,8000);
        check(c.direction(8099)==0,"no early left motion");
        check(c.press(7,HalfScreenControls.RIGHT,8070),"70ms pair produces jump");
        check(c.isVerticalPair(),"70ms pair vertical even without rest flag");
        freeze(c,8071,300,-300f,"vertical with no surface");
        c.reset();

        // 101ms is NOT vertical and must never slow a directional jump.
        c.press(8,HalfScreenControls.LEFT,9000);
        check(!c.blocksHorizontal(9101),"start window ends after 100ms");
        check(c.press(9,HalfScreenControls.RIGHT,9101),"late second finger jumps");
        check(!c.isVerticalPair(),"101ms is directional");
        check(c.direction(9101)==HalfScreenControls.LEFT,"lead left steering persists");
        float vx=-330f;
        for(int i=0;i<360;i++){
            float previous=vx;
            vx=step(c,9101+i*8L,vx,false);
            check(vx<=previous,"late finger must never brake left motion");
        }
        close(vx,-399f,"left stays at max speed under second finger");
        c.release(9,12100);
        check(c.direction(12100)==HalfScreenControls.LEFT,
                "directional jump has no 100ms release delay");
        c.reset();

        // Unbraked right side is symmetrical.
        c.press(10,HalfScreenControls.RIGHT,13000);
        c.press(11,HalfScreenControls.LEFT,13400);
        check(!c.isVerticalPair(),"400ms pair is directional");
        check(c.direction(13400)==HalfScreenControls.RIGHT,"right steering maintained");
        check(step(c,13400,340f,false)>340f,"right movement accelerates");
        c.reset();

        // Same side two touches never jump, and a dragged second pointer
        // cannot retroactively classify as an initial simultaneous pair.
        c.press(12,HalfScreenControls.LEFT,14000);
        check(!c.press(13,HalfScreenControls.LEFT,14020),"same side no jump");
        c.move(13,HalfScreenControls.RIGHT);
        check(!c.isVerticalPair(),"drag not vertical start");
        c.release(13,14100);c.release(12,14110);
        check(c.press(14,HalfScreenControls.LEFT,14200),"quick retap still jumps");
        c.release(14,14230);
        check(!c.press(15,HalfScreenControls.LEFT,14600),"slow retap no jump");
        c.reset();

        // Additional 100ms boundary assertions, including both halves in either order.
        c.press(21,HalfScreenControls.RIGHT,17000);
        check(c.direction(17099)==0,"right start remains blocked through 99ms");
        check(c.press(22,HalfScreenControls.LEFT,17100),"second finger at exactly 100ms jumps");
        check(c.isVerticalPair(),"100ms pair remains vertical");
        c.release(22,17200);
        check(c.direction(17299)==0,"release wait lasts 99ms");
        check(c.direction(17300)==HalfScreenControls.RIGHT,"release wait ends at 100ms");
        c.reset();

        System.out.println("HalfScreenControlsTest OK: guaranteed first-touch lock, "
                +"vertical pair, 100ms release and unbraked directional jump");
    }
}
