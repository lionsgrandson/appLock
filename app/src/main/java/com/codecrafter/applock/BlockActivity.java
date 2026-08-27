package com.codecrafter.applock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BlockActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView reasonText;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (!Prefs.isLockActive(BlockActivity.this)) {
                finish();
                return;
            }
            if (reasonText != null) reasonText.setText(Prefs.activeReason(BlockActivity.this));
            handler.postDelayed(this, 750);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        goHome();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(30), dp(30), dp(30), dp(30));
        root.setBackgroundColor(Color.WHITE);

        TextView lock = new TextView(this);
        lock.setText("🔒");
        lock.setTextSize(54);
        lock.setGravity(Gravity.CENTER);
        root.addView(lock, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("This app is blocked");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(21, 23, 26));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(12);
        root.addView(title, titleLp);

        reasonText = new TextView(this);
        reasonText.setText(Prefs.activeReason(this));
        reasonText.setTextSize(17);
        reasonText.setTextColor(Color.rgb(98, 102, 109));
        reasonText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams reasonLp = new LinearLayout.LayoutParams(-1, -2);
        reasonLp.topMargin = dp(10);
        reasonLp.bottomMargin = dp(28);
        root.addView(reasonText, reasonLp);

        Button home = new Button(this);
        home.setText("Go home");
        home.setAllCaps(false);
        home.setOnClickListener(v -> goHome());
        root.addView(home, new LinearLayout.LayoutParams(-1, dp(52)));

        Button settings = new Button(this);
        settings.setText("Open AppLock settings");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(-1, dp(52));
        settingsLp.topMargin = dp(10);
        root.addView(settings, settingsLp);

        setContentView(root);
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
