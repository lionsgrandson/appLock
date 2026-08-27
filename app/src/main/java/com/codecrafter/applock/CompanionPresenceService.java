package com.codecrafter.applock;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceService;
import android.companion.DevicePresenceEvent;
import android.os.Build;

public class CompanionPresenceService extends CompanionDeviceService {

    @Override
    public void onDeviceAppeared(AssociationInfo associationInfo) {
        if (associationInfo != null && matches(associationInfo.getId())) setPresent(true);
    }

    @Override
    public void onDeviceDisappeared(AssociationInfo associationInfo) {
        if (associationInfo != null && matches(associationInfo.getId())) setPresent(false);
    }

    @Override
    public void onDevicePresenceEvent(DevicePresenceEvent event) {
        if (Build.VERSION.SDK_INT < 36 || event == null || !matches(event.getAssociationId())) return;

        int type = event.getEvent();
        if (type == DevicePresenceEvent.EVENT_BLE_APPEARED
                || type == DevicePresenceEvent.EVENT_BT_CONNECTED
                || type == DevicePresenceEvent.EVENT_SELF_MANAGED_APPEARED
                || type == DevicePresenceEvent.EVENT_SELF_MANAGED_NEARBY) {
            setPresent(true);
        } else if (type == DevicePresenceEvent.EVENT_BLE_DISAPPEARED
                || type == DevicePresenceEvent.EVENT_BT_DISCONNECTED
                || type == DevicePresenceEvent.EVENT_SELF_MANAGED_DISAPPEARED
                || type == DevicePresenceEvent.EVENT_SELF_MANAGED_NOT_NEARBY) {
            setPresent(false);
        } else if (type == DevicePresenceEvent.EVENT_ASSOCIATION_REMOVED) {
            CompanionHelper.clearAssociation(this);
        }
    }

    private boolean matches(int associationId) {
        return associationId >= 0 && associationId == Prefs.get(this).getInt(Prefs.KEY_ASSOCIATION_ID, -1);
    }

    private void setPresent(boolean present) {
        Prefs.get(this).edit().putBoolean(Prefs.KEY_PROXIMITY_PRESENT, present).apply();
    }
}
