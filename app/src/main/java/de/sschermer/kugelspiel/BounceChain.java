package de.sschermer.kugelspiel;

/**
 * Pure-Java bounce-chain controller.
 * Tier 0 = 100%, tier 1 = 150%, tier 2 = 200%, tier 3 = 250% jump HEIGHT.
 * Physics consumes speedScale() = sqrt(heightScale), since height ~ v^2.
 */
final class BounceChain {
    static final int MAX_BOOSTS = 3;
    static final float WINDOW_SECONDS = 0.36f;
    int boosts = 0;
    private boolean fromJump = false;
    private boolean inAutoHop = false;
    private float window = 0f;

    void reset() { boosts=0;fromJump=false;inAutoHop=false;window=0; }
    void update(float dt) {window=Math.max(0,window-dt);}
    boolean mayBoost(){return inAutoHop && window>0;}
    float jump(boolean canStartNormal) {
        if(mayBoost()){
            boosts=Math.min(MAX_BOOSTS,boosts+1);
            fromJump=true;inAutoHop=false;window=0;
            return (float)Math.sqrt(1f+.5f*boosts);
        }
        if(canStartNormal) {
            boosts=0;
            fromJump=true;inAutoHop=false;window=0;
            return 1f;
        }
        return 0f;
    }
    /** Returns true exactly once when an automatic intermediate hop should begin. */
    boolean landed() {
        if(inAutoHop){inAutoHop=false;boosts=0;window=0;return false;}
        if(fromJump){fromJump=false;inAutoHop=true;window=WINDOW_SECONDS;return true;}
        return false;
    }
    /** A ceiling hit always returns to jump height one, immediately. */
    void ceilingContact(){boosts=0;inAutoHop=false;window=0;}
    boolean automaticHopActive(){return inAutoHop;}
    float currentHeight(){return 1f+.5f*boosts;}
}