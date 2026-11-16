package de.sschermer.kugelspiel;

/** No Android dependencies: regression tests for timing and stack rules. */
public final class BounceChainTest {
    static void eq(boolean cond,String label){if(!cond)throw new AssertionError(label);}
    static void close(float a,float b,String name) {eq(Math.abs(a-b)<.0005f,name+" expected "+b+" got "+a);}
    public static void main(String[] args) {
        BounceChain b=new BounceChain();
        close(b.jump(true),1,"Initial jump speed");
        eq(b.landed(),"First landing creates intermediate hop");
        close(b.jump(false),(float)Math.sqrt(1.5),"First boost = 150% height");
        eq(b.landed(),"Second hop");
        close(b.jump(false),(float)Math.sqrt(2),"Second boost = 200% height");
        eq(b.landed(),"Third hop");
        close(b.jump(false),(float)Math.sqrt(2.5),"Third boost = 250% height");
        eq(b.boosts==3,"Three boosts cap");
        eq(b.landed(),"Fourth intermediate hop available");
        close(b.jump(false),(float)Math.sqrt(2.5),"Unlimited chains stay capped at 250%");
        eq(b.boosts==3,"No tier 4");
        b.ceilingContact();
        eq(b.boosts==0,"Ceiling hit instantly resets to tier 0 (100% height)");
        close(b.currentHeight(),1f,"Normal jump height after ceiling contact");
        eq(b.landed(),"Can start a new chain after ceiling collision landing");
        b.update(.40f);
        eq(!b.mayBoost(),"Missed timing must close boost window");
        eq(!b.landed(),"Expired intermediate hop lands without restarting chain");
        eq(b.boosts==0,"Missed hop resets");
        b.reset();close(b.jump(true),1f,"New ordinary jump after reset");
        System.out.println("BounceChain tests pass: 100/150/200/250%, unlimited chains, ceiling reset.");
    }
}
