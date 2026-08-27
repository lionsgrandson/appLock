package com.codecrafter.applock;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.companion.ObservingDevicePresenceRequest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public final class CompanionHelper {
    private CompanionHelper() {}

    public static void saveAssociation(Context context, AssociationInfo info) {
        SharedPreferences.Editor editor = Prefs.get(context).edit()
                .putInt(Prefs.KEY_ASSOCIATION_ID, info.getId())
                .putBoolean(Prefs.KEY_PROXIMITY_PRESENT, false);

        CharSequence displayName = info.getDisplayName();
        if (displayName != null) editor.putString(Prefs.KEY_ASSOCIATION_NAME, displayName.toString());
        if (info.getDeviceMacAddress() != null) editor.putString(Prefs.KEY_ASSOCIATION_ADDRESS, info.getDeviceMacAddress().toString());
        editor.apply();
        startObservation(context);
    }

    public static void startObservation(Context context) {
        if (Build.VERSION.SDK_INT < 33) return;

        int associationId = Prefs.get(context).getInt(Prefs.KEY_ASSOCIATION_ID, -1);
        if (associationId < 0) return;

        CompanionDeviceManager manager = context.getSystemService(CompanionDeviceManager.class);
        if (manager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= 36) {
                ObservingDevicePresenceRequest request = new ObservingDevicePresenceRequest.Builder()
                        .setAssociationId(associationId)
                        .build();
                manager.startObservingDevicePresence(request);
            } else {
                String address = Prefs.get(context).getString(Prefs.KEY_ASSOCIATION_ADDRESS, null);
                if (address != null && !address.isEmpty()) manager.startObservingDevicePresence(address);
            }
        } catch (SecurityException | IllegalArgumentException | IllegalStateException ignored) {
        }
    }

    public static void clearAssociation(Context context) {
        Prefs.get(context).edit()
                .remove(Prefs.KEY_ASSOCIATION_ID)
                .remove(Prefs.KEY_ASSOCIATION_ADDRESS)
                .remove(Prefs.KEY_ASSOCIATION_NAME)
                .putBoolean(Prefs.KEY_PROXIMITY_PRESENT, false)
                .apply();
    }
}
