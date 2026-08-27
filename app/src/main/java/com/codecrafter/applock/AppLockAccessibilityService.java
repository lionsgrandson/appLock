package com.codecrafter.applock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Set;

public class AppLockAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String lastForegroundPackage;
    private long lastLaunchAt;
    private String lastBlockedPackage;

    private final Runnable checker = new Runnable() {
        @Override
        public void run() {
            refreshCurrentPackage();
            enforce(lastForegroundPackage);
            handler.postDelayed(this, 1500);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        handler.removeCallbacks(checker);
        handler.post(checker);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String packageName = event.getPackageName().toString();
        if (!packageName.equals(getPackageName())) lastForegroundPackage = packageName;
        enforce(packageName);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(checker);
        super.onDestroy();
    }

    private void refreshCurrentPackage() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null && root.getPackageName() != null) {
                String packageName = root.getPackageName().toString();
                if (!packageName.equals(getPackageName())) lastForegroundPackage = packageName;
            }
        } catch (Exception ignored) {
        }
    }

    private void enforce(String packageName) {
        if (packageName == null || packageName.equals(getPackageName())) return;
        if (!Prefs.isLockActive(this)) return;

        Set<String> selected = Prefs.selectedPackages(this);
        if (!selected.contains(packageName)) return;

        long now = SystemClock.elapsedRealtime();
        if (packageName.equals(lastBlockedPackage) && now - lastLaunchAt < 900) return;
        lastBlockedPackage = packageName;
        lastLaunchAt = now;

        Intent block = new Intent(this, BlockActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("blocked_package", packageName);
        startActivity(block);
    }
}
