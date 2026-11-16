package de.sschermer.kugelspiel;

/** Horizontal physics shared by gameplay and standalone input tests. */
final class HorizontalMotion {
    private HorizontalMotion(){}

    static float step(float velocity,int command,boolean onSurface,
                      boolean blockHorizontal,float dt,float maxSpeed){
        // The start, vertical-pair and post-release grace periods block movement.
        // A second finger joining an existing steering gesture does NOT.
        if(blockHorizontal)return 0f;
        if(command!=0)velocity+=command*(onSurface?1320f:860f)*dt;
        else velocity*=Math.max(0f,1f-(onSurface?10f:3f)*dt);
        return Math.max(-maxSpeed,Math.min(maxSpeed,velocity));
    }
}
