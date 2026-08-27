package com.codecrafter.applock;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_PAIR = 5101;
    private static final int REQ_BT = 5102;

    private static final Map<String, String> COMMON_DISTRACTIONS = new LinkedHashMap<>();
    static {
        COMMON_DISTRACTIONS.put("com.zhiliaoapp.musically", "TikTok");
        COMMON_DISTRACTIONS.put("com.instagram.android", "Instagram");
        COMMON_DISTRACTIONS.put("com.facebook.katana", "Facebook");
        COMMON_DISTRACTIONS.put("com.facebook.orca", "Messenger");
        COMMON_DISTRACTIONS.put("com.snapchat.android", "Snapchat");
        COMMON_DISTRACTIONS.put("com.twitter.android", "X / Twitter");
        COMMON_DISTRACTIONS.put("com.reddit.frontpage", "Reddit");
        COMMON_DISTRACTIONS.put("com.google.android.youtube", "YouTube");
        COMMON_DISTRACTIONS.put("com.netflix.mediaclient", "Netflix");
        COMMON_DISTRACTIONS.put("com.discord", "Discord");
        COMMON_DISTRACTIONS.put("com.pinterest", "Pinterest");
        COMMON_DISTRACTIONS.put("tv.twitch.android.app", "Twitch");
    }

    private TextView statusTitle;
    private TextView statusBody;
    private TextView accessibilityState;
    private TextView scheduleSummary;
    private TextView proximitySummary;
    private Button startTimeButton;
    private Button endTimeButton;
    private LinearLayout appListContainer;
    private final Map<String, CheckBox> packageCheckboxes = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        CompanionHelper.startObservation(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(246, 247, 251));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(38));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("AppLock", 32, Color.rgb(21, 23, 26));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("Block distracting apps manually, on a schedule, or when a trusted Bluetooth device is nearby.", 16, Color.rgb(98, 102, 109));
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(-1, -2);
        subtitleLp.topMargin = dp(6);
        subtitleLp.bottomMargin = dp(18);
        root.addView(subtitle, subtitleLp);

        LinearLayout statusCard = card();
        statusTitle = text("Ready", 20, Color.rgb(21, 23, 26));
        statusTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        statusCard.addView(statusTitle);
        statusBody = text("Not currently locked", 15, Color.rgb(98, 102, 109));
        LinearLayout.LayoutParams statusBodyLp = new LinearLayout.LayoutParams(-1, -2);
        statusBodyLp.topMargin = dp(4);
        statusCard.addView(statusBody, statusBodyLp);
        root.addView(statusCard);

        root.addView(section("1. Blocking permission"));
        accessibilityState = text("Accessibility service: checking…", 15, Color.rgb(98, 102, 109));
        root.addView(accessibilityState);
        Button accessibilityButton = button("Enable / manage Accessibility service");
        accessibilityButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibilityButton, buttonParams());

        root.addView(section("2. Lock controls"));
        Switch manual = switchRow("Lock selected apps now", Prefs.get(this).getBoolean(Prefs.KEY_MANUAL, false));
        manual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.get(this).edit().putBoolean(Prefs.KEY_MANUAL, isChecked).apply();
            refreshStatus();
        });
        root.addView(manual);

        Switch schedule = switchRow("Use daily schedule", Prefs.get(this).getBoolean(Prefs.KEY_SCHEDULE_ENABLED, false));
        schedule.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.get(this).edit().putBoolean(Prefs.KEY_SCHEDULE_ENABLED, isChecked).apply();
            refreshStatus();
        });
        root.addView(schedule);

        scheduleSummary = text("", 14, Color.rgb(98, 102, 109));
        LinearLayout.LayoutParams scheduleLp = new LinearLayout.LayoutParams(-1, -2);
        scheduleLp.topMargin = dp(4);
        root.addView(scheduleSummary, scheduleLp);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setWeightSum(2f);
        startTimeButton = button("Start");
        endTimeButton = button("End");
        startTimeButton.setOnClickListener(v -> chooseTime(true));
        endTimeButton.setOnClickListener(v -> chooseTime(false));
        LinearLayout.LayoutParams half1 = new LinearLayout.LayoutParams(0, dp(50), 1f);
        half1.setMargins(0, dp(8), dp(5), 0);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(50), 1f);
        half2.setMargins(dp(5), dp(8), 0, 0);
        timeRow.addView(startTimeButton, half1);
        timeRow.addView(endTimeButton, half2);
        root.addView(timeRow);

        root.addView(section("3. Trusted device proximity"));
        TextView proximityHelp = text(
                "Pair a Bluetooth device once. When Android reports that device as nearby/connected, AppLock can activate automatically. This is best-effort for another phone because phones do not always advertise Bluetooth presence continuously.",
                14,
                Color.rgb(98, 102, 109)
        );
        root.addView(proximityHelp);

        Switch proximity = switchRow("Lock when trusted device is nearby", Prefs.get(this).getBoolean(Prefs.KEY_PROXIMITY_ENABLED, false));
        proximity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.get(this).edit().putBoolean(Prefs.KEY_PROXIMITY_ENABLED, isChecked).apply();
            if (isChecked) CompanionHelper.startObservation(this);
            refreshStatus();
        });
        root.addView(proximity);

        proximitySummary = text("No trusted device paired", 14, Color.rgb(98, 102, 109));
        root.addView(proximitySummary);

        Button pairButton = button("Pair trusted Bluetooth device");
        pairButton.setOnClickListener(v -> requestPairing());
        root.addView(pairButton, buttonParams());

        Button forgetButton = button("Forget trusted device");
        forgetButton.setOnClickListener(v -> forgetTrustedDevice());
        root.addView(forgetButton, buttonParams());

        root.addView(section("4. Apps to block"));
        TextView appHelp = text(
                "Common distraction apps found on this phone are selected automatically the first time. You can add or remove anything below.",
                14,
                Color.rgb(98, 102, 109)
        );
        root.addView(appHelp);

        LinearLayout appButtons = new LinearLayout(this);
        appButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button selectCommon = button("Select common");
        selectCommon.setOnClickListener(v -> selectCommonInstalled());
        Button clear = button("Clear all");
        clear.setOnClickListener(v -> clearAllApps());
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, dp(48), 1f);
        b1.setMargins(0, dp(8), dp(5), dp(8));
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, dp(48), 1f);
        b2.setMargins(dp(5), dp(8), 0, dp(8));
        appButtons.addView(selectCommon, b1);
        appButtons.addView(clear, b2);
        root.addView(appButtons);

        appListContainer = new LinearLayout(this);
        appListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(appListContainer);
        populateApps();

        TextView footer = text(
                "Note: AppLock is intentionally local-only. A normal Android app can be disabled or uninstalled by the phone owner; true tamper resistance requires Device Owner / managed-device provisioning.",
                13,
                Color.rgb(98, 102, 109)
        );
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(-1, -2);
        footerLp.topMargin = dp(24);
        root.addView(footer, footerLp);

        setContentView(scroll);
        refreshStatus();
    }

    private void populateApps() {
        appListContainer.removeAllViews();
        packageCheckboxes.clear();

        List<AppEntry> apps = installedLaunchableApps();
        Set<String> selected = Prefs.selectedPackages(this);

        if (!Prefs.get(this).getBoolean(Prefs.KEY_DEFAULTS_SEEDED, false)) {
            for (AppEntry entry : apps) {
                if (COMMON_DISTRACTIONS.containsKey(entry.packageName)) selected.add(entry.packageName);
            }
            Prefs.setSelectedPackages(this, selected);
            Prefs.get(this).edit().putBoolean(Prefs.KEY_DEFAULTS_SEEDED, true).apply();
        }

        if (apps.isEmpty()) {
            appListContainer.addView(text("No launchable apps were visible to AppLock.", 14, Color.rgb(98, 102, 109)));
            return;
        }

        for (AppEntry entry : apps) {
            CheckBox box = new CheckBox(this);
            String suffix = COMMON_DISTRACTIONS.containsKey(entry.packageName) ? "  • common distraction" : "";
            box.setText(entry.label + suffix);
            box.setTextSize(15);
            box.setTextColor(Color.rgb(21, 23, 26));
            box.setPadding(dp(2), dp(8), dp(2), dp(8));
            box.setChecked(selected.contains(entry.packageName));
            box.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Set<String> current = Prefs.selectedPackages(this);
                if (isChecked) current.add(entry.packageName); else current.remove(entry.packageName);
                Prefs.setSelectedPackages(this, current);
                refreshStatus();
            });
            appListContainer.addView(box, new LinearLayout.LayoutParams(-1, -2));
            packageCheckboxes.put(entry.packageName, box);
        }
    }

    private List<AppEntry> installedLaunchableApps() {
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL);
        Map<String, AppEntry> byPackage = new HashMap<>();

        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName())) continue;
            CharSequence label = info.loadLabel(getPackageManager());
            String niceLabel = label == null ? pkg : label.toString();
            byPackage.put(pkg, new AppEntry(pkg, niceLabel));
        }

        List<AppEntry> result = new ArrayList<>(byPackage.values());
        result.sort((a, b) -> {
            boolean aCommon = COMMON_DISTRACTIONS.containsKey(a.packageName);
            boolean bCommon = COMMON_DISTRACTIONS.containsKey(b.packageName);
            if (aCommon != bCommon) return aCommon ? -1 : 1;
            return a.label.compareToIgnoreCase(b.label);
        });
        return result;
    }

    private void selectCommonInstalled() {
        Set<String> selected = Prefs.selectedPackages(this);
        for (String pkg : COMMON_DISTRACTIONS.keySet()) {
            CheckBox box = packageCheckboxes.get(pkg);
            if (box != null) {
                selected.add(pkg);
                box.setChecked(true);
            }
        }
        Prefs.setSelectedPackages(this, selected);
        refreshStatus();
    }

    private void clearAllApps() {
        Prefs.setSelectedPackages(this, Collections.emptySet());
        for (CheckBox box : packageCheckboxes.values()) box.setChecked(false);
        refreshStatus();
    }

    private void chooseTime(boolean start) {
        int fallback = start ? 22 * 60 : 7 * 60;
        int current = Prefs.get(this).getInt(start ? Prefs.KEY_START_MINUTES : Prefs.KEY_END_MINUTES, fallback);
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Prefs.get(this).edit().putInt(start ? Prefs.KEY_START_MINUTES : Prefs.KEY_END_MINUTES, hourOfDay * 60 + minute).apply();
                    refreshStatus();
                },
                current / 60,
                current % 60,
                true
        );
        dialog.show();
    }

    private void requestPairing() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP)) {
            Toast.makeText(this, "This phone does not support Android companion-device pairing.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean scan = checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        boolean connect = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        if (!scan || !connect) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQ_BT);
            return;
        }
        pairTrustedDevice();
    }

    private void pairTrustedDevice() {
        CompanionDeviceManager manager = getSystemService(CompanionDeviceManager.class);
        if (manager == null) {
            Toast.makeText(this, "Companion Device Manager is unavailable.", Toast.LENGTH_LONG).show();
            return;
        }

        BluetoothDeviceFilter filter = new BluetoothDeviceFilter.Builder().build();
        AssociationRequest request = new AssociationRequest.Builder()
                .addDeviceFilter(filter)
                .setSingleDevice(false)
                .build();

        manager.associate(request, getMainExecutor(), new CompanionDeviceManager.Callback() {
            @Override
            public void onAssociationPending(IntentSender intentSender) {
                try {
                    startIntentSenderForResult(intentSender, REQ_PAIR, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Toast.makeText(MainActivity.this, "Could not open the Bluetooth device chooser.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onAssociationCreated(AssociationInfo associationInfo) {
                CompanionHelper.saveAssociation(MainActivity.this, associationInfo);
                refreshStatus();
            }

            @Override
            public void onFailure(CharSequence error) {
                Toast.makeText(MainActivity.this, error == null ? "Pairing failed." : error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PAIR && resultCode == RESULT_OK && data != null) {
            AssociationInfo info = data.getParcelableExtra(CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo.class);
            if (info != null) {
                CompanionHelper.saveAssociation(this, info);
                refreshStatus();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BT) {
            boolean granted = true;
            for (int result : grantResults) granted &= result == PackageManager.PERMISSION_GRANTED;
            if (granted) pairTrustedDevice();
            else Toast.makeText(this, "Bluetooth permission is needed to pair a trusted device.", Toast.LENGTH_LONG).show();
        }
    }

    private void forgetTrustedDevice() {
        int id = Prefs.get(this).getInt(Prefs.KEY_ASSOCIATION_ID, -1);
        if (id >= 0) {
            CompanionDeviceManager manager = getSystemService(CompanionDeviceManager.class);
            if (manager != null) {
                try {
                    manager.disassociate(id);
                } catch (Exception ignored) {
                }
            }
        }
        CompanionHelper.clearAssociation(this);
        refreshStatus();
    }

    private void refreshStatus() {
        boolean active = Prefs.isLockActive(this);
        Set<String> selected = Prefs.selectedPackages(this);

        if (statusTitle != null) {
            statusTitle.setText(active ? "Blocking is active" : "Blocking is off");
            statusTitle.setTextColor(active ? Color.rgb(179, 38, 30) : Color.rgb(49, 86, 216));
        }
        if (statusBody != null) {
            statusBody.setText(Prefs.activeReason(this) + " • " + selected.size() + " app" + (selected.size() == 1 ? "" : "s") + " selected");
        }

        boolean accessibility = isAccessibilityServiceEnabled();
        if (accessibilityState != null) {
            accessibilityState.setText(accessibility ? "Accessibility service: enabled" : "Accessibility service: NOT enabled");
            accessibilityState.setTextColor(accessibility ? Color.rgb(31, 111, 74) : Color.rgb(179, 38, 30));
        }

        int start = Prefs.get(this).getInt(Prefs.KEY_START_MINUTES, 22 * 60);
        int end = Prefs.get(this).getInt(Prefs.KEY_END_MINUTES, 7 * 60);
        if (startTimeButton != null) startTimeButton.setText("Start  " + Prefs.formatMinutes(start));
        if (endTimeButton != null) endTimeButton.setText("End  " + Prefs.formatMinutes(end));
        if (scheduleSummary != null) {
            boolean enabled = Prefs.get(this).getBoolean(Prefs.KEY_SCHEDULE_ENABLED, false);
            scheduleSummary.setText((enabled ? "Enabled" : "Disabled") + " • every day " + Prefs.formatMinutes(start) + " → " + Prefs.formatMinutes(end));
        }

        if (proximitySummary != null) {
            String name = Prefs.get(this).getString(Prefs.KEY_ASSOCIATION_NAME, null);
            String address = Prefs.get(this).getString(Prefs.KEY_ASSOCIATION_ADDRESS, null);
            boolean present = Prefs.get(this).getBoolean(Prefs.KEY_PROXIMITY_PRESENT, false);
            int id = Prefs.get(this).getInt(Prefs.KEY_ASSOCIATION_ID, -1);
            if (id < 0) {
                proximitySummary.setText("No trusted device paired");
            } else {
                String device = !TextUtils.isEmpty(name) ? name : (!TextUtils.isEmpty(address) ? address : "Trusted device");
                proximitySummary.setText(device + " • " + (present ? "nearby / connected" : "not currently detected"));
            }
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName expected = new ComponentName(this, AppLockAccessibilityService.class);
        String[] services = enabled.split(":");
        for (String service : services) {
            ComponentName actual = ComponentName.unflattenFromString(service);
            if (expected.equals(actual)) return true;
        }
        return false;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView section(String value) {
        TextView title = text(value, 19, Color.rgb(21, 23, 26));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, dp(22), 0, dp(8));
        return title;
    }

    private Switch switchRow(String label, boolean checked) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextSize(16);
        sw.setTextColor(Color.rgb(21, 23, 26));
        sw.setChecked(checked);
        sw.setPadding(0, dp(8), 0, dp(8));
        return sw;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        return b;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.topMargin = dp(8);
        return lp;
    }

    private TextView text(String value, float size, int color) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setLineSpacing(0, 1.12f);
        return tv;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class AppEntry {
        final String packageName;
        final String label;

        AppEntry(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }
}
