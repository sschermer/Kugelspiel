package de.sschermer.kugelspiel;

/** Pure horizontal motion update shared by the game and standalone regression tests. */
final class HorizontalMotion {
    private HorizontalMotion(){}

    static float step(float velocity,int command,boolean onSurface,
                      boolean bothHalvesHeld,float dt,float maxSpeed){
        // A two-half hold is not just neutral input. It must cancel all inertia.
        if(bothHalvesHeld)return 0f;
        if(command!=0)velocity+=command*(onSurface?1320f:860f)*dt;
        else velocity*=Math.max(0f,1f-(onSurface?10f:3f)*dt);
        return Math.max(-maxSpeed,Math.min(maxSpeed,velocity));
    }
}
