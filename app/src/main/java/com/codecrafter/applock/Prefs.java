package com.codecrafter.applock;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Prefs {
    private static final String FILE = "app_lock_prefs";

    public static final String KEY_SELECTED = "selected_packages";
    public static final String KEY_DEFAULTS_SEEDED = "defaults_seeded";
    public static final String KEY_MANUAL = "manual_lock";
    public static final String KEY_SCHEDULE_ENABLED = "schedule_enabled";
    public static final String KEY_START_MINUTES = "start_minutes";
    public static final String KEY_END_MINUTES = "end_minutes";
    public static final String KEY_PROXIMITY_ENABLED = "proximity_enabled";
    public static final String KEY_PROXIMITY_PRESENT = "proximity_present";
    public static final String KEY_ASSOCIATION_ID = "association_id";
    public static final String KEY_ASSOCIATION_ADDRESS = "association_address";
    public static final String KEY_ASSOCIATION_NAME = "association_name";

    private Prefs() {}

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static Set<String> selectedPackages(Context context) {
        Set<String> stored = get(context).getStringSet(KEY_SELECTED, Collections.emptySet());
        return new HashSet<>(stored == null ? Collections.emptySet() : stored);
    }

    public static void setSelectedPackages(Context context, Set<String> packages) {
        get(context).edit().putStringSet(KEY_SELECTED, new HashSet<>(packages)).apply();
    }

    public static boolean isLockActive(Context context) {
        SharedPreferences p = get(context);
        if (p.getBoolean(KEY_MANUAL, false)) return true;
        if (p.getBoolean(KEY_PROXIMITY_ENABLED, false) && p.getBoolean(KEY_PROXIMITY_PRESENT, false)) return true;
        return isScheduleActive(context);
    }

    public static boolean isScheduleActive(Context context) {
        SharedPreferences p = get(context);
        if (!p.getBoolean(KEY_SCHEDULE_ENABLED, false)) return false;

        int start = p.getInt(KEY_START_MINUTES, 22 * 60);
        int end = p.getInt(KEY_END_MINUTES, 7 * 60);
        LocalTime now = LocalTime.now();
        int current = now.getHour() * 60 + now.getMinute();

        if (start == end) return true;
        if (start < end) return current >= start && current < end;
        return current >= start || current < end;
    }

    public static String activeReason(Context context) {
        SharedPreferences p = get(context);
        if (p.getBoolean(KEY_MANUAL, false)) return "Manual lock is on";
        if (p.getBoolean(KEY_PROXIMITY_ENABLED, false) && p.getBoolean(KEY_PROXIMITY_PRESENT, false)) {
            String name = p.getString(KEY_ASSOCIATION_NAME, "Trusted device");
            if (name == null || name.trim().isEmpty()) name = "Trusted device";
            return name + " is nearby";
        }
        if (isScheduleActive(context)) return "Scheduled focus time is active";
        return "Not currently locked";
    }

    public static String formatMinutes(int minutes) {
        minutes = ((minutes % 1440) + 1440) % 1440;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60);
    }
}
