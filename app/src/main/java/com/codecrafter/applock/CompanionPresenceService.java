package com.codecrafter.applock;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceService;

public class CompanionPresenceService extends CompanionDeviceService {

    @Override
    public void onDeviceAppeared(AssociationInfo associationInfo) {
        if (associationInfo != null && matches(associationInfo.getId())) setPresent(true);
    }

    @Override
    public void onDeviceDisappeared(AssociationInfo associationInfo) {
        if (associationInfo != null && matches(associationInfo.getId())) setPresent(false);
    }

    private boolean matches(int associationId) {
        return associationId >= 0 && associationId == Prefs.get(this).getInt(Prefs.KEY_ASSOCIATION_ID, -1);
    }

    private void setPresent(boolean present) {
        Prefs.get(this).edit().putBoolean(Prefs.KEY_PROXIMITY_PRESENT, present).apply();
    }
}
