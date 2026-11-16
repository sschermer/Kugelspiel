package de.sschermer.kugelspiel;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public final class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        game = new GameView(this);
        setContentView(game);
    }
    @Override protected void onPause() {
        super.onPause();
        if(game!=null)game.setPaused(true);
    }
    @Override protected void onResume() {
        super.onResume();
        if(game!=null)game.setPaused(false);
    }
}