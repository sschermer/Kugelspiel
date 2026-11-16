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
                c.blocksHorizontal(now),1f/120f,532f);
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
        // used to bypass the 200ms window. No such parameter exists anymore.
        check(!c.press(1,HalfScreenControls.LEFT,1000),"initial contact, no jump");
        check(c.pointerCount()==1,"one finger tracked");
        check(c.blocksHorizontal(1000),"first contact ALWAYS locked at t=0");
        check(c.blocksHorizontal(1199),"still locked at 199 ms");
        freeze(c,1100,12,532f,"mandatory first-touch freeze");
        check(c.direction(1200)==HalfScreenControls.LEFT,"movement starts at 200ms");
        c.reset();

        // A second touch at the 200ms inclusive boundary is vertical.
        c.press(2,HalfScreenControls.RIGHT,2000);
        check(c.press(3,HalfScreenControls.LEFT,2200),"opposite touch jumps");
        check(c.isVerticalPair(),"200ms is vertical even after start waiting");
        check(c.debugStatus(2250).contains("SENKRECHT"),"diagnosis shows vertical mode");
        freeze(c,2300,480,532f,"vertical pair held four seconds");
        c.release(2,6500);
        check(!c.isVerticalPair(),"one finger release ends vertical pair");
        check(c.blocksHorizontal(6699),"post-release 200ms delay at 199");
        close(step(c,6699,80f,false),0f,"release freeze cancels momentum");
        check(c.direction(6700)==HalfScreenControls.LEFT,"remaining left finger steers");
        check(step(c,6700,0f,false)<0f,"left accelerates in midair after 200ms");
        c.reset();

        // Releasing the opposite finger also resumes the lead right direction.
        c.press(4,HalfScreenControls.RIGHT,7000);
        c.press(5,HalfScreenControls.LEFT,7010);
        c.release(5,7100);
        check(c.direction(7299)==0,"right release delay still active at 199ms");
        check(c.direction(7300)==HalfScreenControls.RIGHT,"right acceleration after 200ms");
        check(step(c,7300,0f,false)>0f,"right midair acceleration");
        c.reset();

        // The old false rest/ground state must have no impact on recognition.
        c.press(6,HalfScreenControls.LEFT,8000);
        check(c.direction(8199)==0,"no early left motion");
        check(c.press(7,HalfScreenControls.RIGHT,8170),"170ms pair produces jump");
        check(c.isVerticalPair(),"170ms pair vertical even without rest flag");
        freeze(c,8171,300,-300f,"vertical with no surface");
        c.reset();

        // 201ms is NOT vertical and must never slow a directional jump.
        c.press(8,HalfScreenControls.LEFT,9000);
        check(!c.blocksHorizontal(9201),"start window ends after 200ms");
        check(c.press(9,HalfScreenControls.RIGHT,9201),"late second finger jumps");
        check(!c.isVerticalPair(),"201ms is directional");
        check(c.direction(9201)==HalfScreenControls.LEFT,"lead left steering persists");
        float vx=-420f;
        for(int i=0;i<360;i++){
            float previous=vx;
            vx=step(c,9201+i*8L,vx,false);
            check(vx<=previous,"late finger must never brake left motion");
        }
        close(vx,-532f,"left stays at max speed under second finger");
        c.release(9,12100);
        check(c.direction(12100)==HalfScreenControls.LEFT,
                "directional jump has no 200ms release delay");
        c.reset();

        // Unbraked right side is symmetrical.
        c.press(10,HalfScreenControls.RIGHT,13000);
        c.press(11,HalfScreenControls.LEFT,13400);
        check(!c.isVerticalPair(),"400ms pair is directional");
        check(c.direction(13400)==HalfScreenControls.RIGHT,"right steering maintained");
        check(step(c,13400,430f,false)>430f,"right movement accelerates");
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

        System.out.println("HalfScreenControlsTest OK: guaranteed first-touch lock, "
                +"vertical pair, 200ms release and unbraked directional jump");
    }
}
