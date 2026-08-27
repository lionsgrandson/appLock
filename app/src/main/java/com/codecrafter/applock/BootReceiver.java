package com.codecrafter.applock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Prefs.get(context).edit().putBoolean(Prefs.KEY_PROXIMITY_PRESENT, false).apply();
            CompanionHelper.startObservation(context);
        }
    }
}
