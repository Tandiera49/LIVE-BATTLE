package com.livebattle.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    private BattleGameView gameView;
    private TikTokEventServer eventServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        gameView = new BattleGameView(this);
        setContentView(gameView);

        eventServer = new TikTokEventServer(
                (type, username, giftId, giftName, count) ->
                        gameView.post(() ->
                                gameView.handleTikTokEvent(
                                        type,
                                        username,
                                        giftId,
                                        giftName,
                                        count
                                )
                        )
        );

        eventServer.start();
    }

    @Override
    protected void onDestroy() {
        if (eventServer != null) {
            eventServer.stop();
        }

        super.onDestroy();
    }
}
