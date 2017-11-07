package com.siliconlabs.bledemo.fragment;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.BeaconScanActivity;
import com.siliconlabs.bledemo.activity.KeyFobsActivity;
import com.siliconlabs.bledemo.activity.MainActivityDebugMode;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.interfaces.DemoPageLauncher;

import java.util.Arrays;
import java.util.List;

import butterknife.InjectViews;

public class DemoItemProvider {
    static final SparseIntArray ICONS = new SparseIntArray();
    static final SparseIntArray TITLES = new SparseIntArray();
    static final SparseIntArray TEXTS = new SparseIntArray();
    static final SparseArray<List<Pair<Integer, Integer>>> PROFILES = new SparseArray<>();
    static final SparseArray<DemoPageLauncher> LAUNCHERS = new SparseArray<>();

    static {
        ICONS.put(R.id.demo_health_thermometer, R.drawable.home_thermometer);
        ICONS.put(R.id.demo_retail_beacon, R.drawable.home_retail_beacon);
        ICONS.put(R.id.demo_key_fobs, R.drawable.home_key_fob);
        ICONS.put(R.id.demo_smart_watch, R.drawable.home_smart_watch);
        ICONS.put(R.id.demo_remote_keyboard, R.drawable.home_remote_keyboard);
        ICONS.put(R.id.blue_giga_debug, R.drawable.home_debug_phone);

        TITLES.put(R.id.demo_health_thermometer, R.string.demo_thermometer_title);
        TITLES.put(R.id.demo_retail_beacon, R.string.demo_beacon_title);
        TITLES.put(R.id.demo_key_fobs, R.string.demo_fob_title);
        TITLES.put(R.id.blue_giga_debug, R.string.demo_item_debug_mode_title);

        TEXTS.put(R.id.demo_health_thermometer, R.string.demo_thermometer_text);
        TEXTS.put(R.id.demo_retail_beacon, R.string.demo_beacon_text);
        TEXTS.put(R.id.demo_key_fobs, R.string.demo_fob_text);
        TEXTS.put(R.id.blue_giga_debug, R.string.demo_item_debug_mode_description);

        PROFILES.put(R.id.demo_health_thermometer, Arrays.asList(new Pair<>(R.string.htp_title, R.string.htp_id)));
        PROFILES.put(R.id.demo_key_fobs, Arrays.asList(new Pair<>(R.string.fmp_title, R.string.fmp_id)));

        LAUNCHERS.put(R.id.demo_health_thermometer, new DemoPageLauncher() {
            @Override
            public DialogFragment launchPage(View view, FragmentManager fragmentManager) {
                int title = TITLES.get(view.getId());
                int description = TEXTS.get(view.getId());
                List<Pair<Integer, Integer>> profilesInfo = PROFILES.get(view.getId());

                BlueToothService.GattConnectType connectType = view.getId() == R.id.demo_health_thermometer ? BlueToothService.GattConnectType.THERMOMETER : null;
                SelectDeviceDialog selectDeviceDialog = SelectDeviceDialog.newDialog(title, description, profilesInfo, connectType);
                selectDeviceDialog.show(fragmentManager, "select_device_tag");
                return selectDeviceDialog;
            }
        });
        LAUNCHERS.put(R.id.demo_retail_beacon, new DemoPageLauncher() {
            @Override
            public DialogFragment launchPage(View view, FragmentManager fragmentManager) {
                Intent intent = new Intent(view.getContext(), BeaconScanActivity.class);
                view.getContext().startActivity(intent);
                return null;
            }
        });
        LAUNCHERS.put(R.id.demo_key_fobs, new DemoPageLauncher() {
            @Override
            public DialogFragment launchPage(View view, FragmentManager fragmentManager) {
                Intent intent = new Intent(view.getContext(), KeyFobsActivity.class);
                view.getContext().startActivity(intent);
                return null;
            }
        });
        LAUNCHERS.put(R.id.blue_giga_debug, new DemoPageLauncher() {
            @Override
            public DialogFragment launchPage(View view, FragmentManager fragmentManager) {
                Intent intent = new Intent(view.getContext(), MainActivityDebugMode.class);
                view.getContext().startActivity(intent);
                return null;
            }
        });
    }

    @InjectViews({ R.id.demo_health_thermometer,
                   R.id.demo_retail_beacon,
                   R.id.demo_key_fobs,
                   R.id.blue_giga_debug })
    List<View> demoItems;
}
