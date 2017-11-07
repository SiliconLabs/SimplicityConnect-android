/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF 
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.activity;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.DebugModeDeviceAdapter;
import com.siliconlabs.bledemo.adapters.DeviceInfoViewHolder;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Common;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Converters;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Device;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Engine;
import com.siliconlabs.bledemo.dialogs.DeviceFilterDialog;
import com.siliconlabs.bledemo.dialogs.Dialogs;
import com.siliconlabs.bledemo.fragment.LogFragment;
import com.siliconlabs.bledemo.fragment.SearchFragment;
import com.siliconlabs.bledemo.interfaces.DebugModeCallback;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivityDebugMode extends BaseActivity implements DebugModeCallback, SwipeRefreshLayout.OnRefreshListener, Discovery.BluetoothDiscoveryHost, Discovery.DeviceContainer {
    // static vars
    public static final String ABOUT_DIALOG_HTML_ASSET_FILE_PATH = "file:///android_asset/about.html";
    public static final int BlUETOOTH_SETTINGS_REQUEST_CODE = 100;
    public static int SCAN_PERIOD = 8000;
    // member vars
    private boolean bleIsSupported = true;
    private boolean isBluetoothAdapterEnabled = false;
    private float lastScale = 0.0f;
    // bluetooth service related objects
    private final Discovery discovery = new Discovery(this, this);
    private BlueToothService.Binding bluetoothBinding;
    // used with  swipeRefreshLayout and devicesRecyclerView
    private DebugModeDeviceAdapter devicesAdapter;
    // dialogs
    private Dialog dialogDeviceExtraAdvertisement;
    private Dialog bluetoothEnableDialog;
    private Dialog dialogLicense;
    private Dialog filterDialog;

    boolean scanning = false;
    volatile boolean running = false;

    //log
    private Thread logUpdate;

    private final BroadcastReceiver bluetoothAdapterStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothAdapter defaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                     BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        isBluetoothAdapterEnabled = false;
                        Toast.makeText(MainActivityDebugMode.this,
                                       R.string.toast_bluetooth_not_enabled,
                                       Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        stopScanningAnimation();
                        showEnableBluetoothAdapterBar();
                        //flushContainer();
                        //noDevicesFound.setVisibility(View.VISIBLE);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        isBluetoothAdapterEnabled = false;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (defaultBluetoothAdapter != null &&
                                defaultBluetoothAdapter.isEnabled()) {
                            if (!isBluetoothAdapterEnabled) {
                                Toast.makeText(MainActivityDebugMode.this,
                                               R.string.toast_bluetooth_enabled,
                                               Toast.LENGTH_SHORT).show();
                            }

                            updateListWhenAdapterIsReady = false;
                            bluetoothEnableBar.setVisibility(View.GONE);

                            discovery.disconnect();
                            discovery.connect(MainActivityDebugMode.this);
                            startScanning();
                            startScanningAnimation();
                        }
                        isBluetoothAdapterEnabled = true;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        isBluetoothAdapterEnabled = false;
                        break;
                }
            }
        }
    };

    @InjectView(R.id.bluetooth_enable)
    RelativeLayout bluetoothEnableBar;
    @InjectView(R.id.bluetooth_enable_msg)
    TextView bluetoothEnableMsg;
    @InjectView(R.id.bluetooth_enable_btn)
    TextView bluetoothEnableBtn;
    @InjectView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.no_devices_found)
    LinearLayout noDevicesFound;
    @InjectView(R.id.looking_for_devices_background)
    LinearLayout lookingForDevicesBackgroundMessage;
    @InjectView(R.id.scan_anim_gradient_left)
    RelativeLayout scanGradientLeft;
    @InjectView(R.id.scan_anim_gradient_right)
    RelativeLayout scanGradientRight;
    @InjectView(R.id.scanning_label_textview)
    TextView scanningLabelTextView;
    @InjectView(R.id.scanning_gradient_container)
    RelativeLayout scanningGradientContainer;
    @InjectView(R.id.swipe_refresh_container)
    public SwipeRefreshLayout swipeRefreshLayout;
    StringBuilder subtraction = new StringBuilder();
    @InjectView(R.id.connecting_container)
    public RelativeLayout connectingContainer;
    @InjectView(R.id.connecting_anim_gradient_right_container)
    public LinearLayout connectingGradientContainer;
    @InjectView(R.id.connecting_bar_container)
    public RelativeLayout connectingBarContainer;
    @InjectView(R.id.fab_btn_refresh)
    public FloatingActionButton fabBtnRefresh;

    @InjectView(R.id.recyclerview_debug_devices)
    public RecyclerView devicesRecyclerView;
    TextView tv;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // prepare ui
        setContentView(R.layout.activity_main_debug_mode);
        ButterKnife.inject(this);

        // init bluetoooth discovery engine, matches to accepted bluetooth gatt profiles
        Engine.getInstance().init(this.getApplicationContext());
        // generate Folder
        createFiles();

        // init/config ui
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setShowSpinnerDialogVisibility(false);

        initLicenseDialog();
        initExtraAdvertisementDialog();
        initDevicesRecyclerView();
        initSwipeRefreshLayout();

        BluetoothAdapter defaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (defaultBluetoothAdapter.isEnabled()) {
            startScanningAnimation();
        }

        if (savedInstanceState == null) {
            LogFragment logFragment = new LogFragment();
            FragmentTransaction logtransaction = getSupportFragmentManager().beginTransaction();
            logtransaction.add(R.id.log_body, logFragment);
            logtransaction.commit();
        }
        if (savedInstanceState == null) {
            SearchFragment searchfragment = new SearchFragment();
            FragmentTransaction filtertransaction = getSupportFragmentManager().beginTransaction();
            filtertransaction.add(R.id.filter_body, searchfragment);
            filtertransaction.commit();
        }

        // handle bluetooth adapter on/off state
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);
        bluetoothEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothAdapter.getDefaultAdapter().enable();
                changeEnableBluetoothAdapterToConnecing();
            }
        });

        // attempt connection for discovery
        discovery.connect(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BlUETOOTH_SETTINGS_REQUEST_CODE) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled() && bluetoothEnableDialog != null) {
                bluetoothEnableDialog.show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureFontScale();

        //flushContainer();
        //noDevicesFound.setVisibility(View.VISIBLE);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            startScanning();
        } else {
            showEnableBluetoothAdapterBar();
        }

        if (bluetoothAdapter != null) {
            isBluetoothAdapterEnabled = bluetoothAdapter.isEnabled();
            if (boundService != null) {
                Log.d("OnResume", "Called");
                boundService.clearGatt();
                boundService = null;

            }
            if (bluetoothBinding != null) {
                bluetoothBinding.unbind();
            }
        } else {
            isBluetoothAdapterEnabled = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("onPause", "Called");
        if (scanning) {
            onScanningStopped();
        }
        unregisterReceiver(bluetoothAdapterStateChangeListener);

    }

    @Override
    public void onBackPressed() {
        if (connectingContainer.getVisibility() == View.VISIBLE) {
            if (boundService != null) {
                Log.d("onBackPressed", "Called");
                boundService.clearGatt();
            }

            hideConnectingAnimation();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy", "Called");
        if (bluetoothBinding != null) {
            bluetoothBinding.unbind();
        }
        discovery.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_debug_mode, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_license:
                showAbout();
                break;

            case R.id.bluetooth_modules_website:
                Intent intentBlueGigaWebsite = new Intent(Intent.ACTION_VIEW);
                intentBlueGigaWebsite.setData(Uri.parse(Common.BLUEGIGA_URL_SILICON_LABS_OCT_2015));
                startActivity(intentBlueGigaWebsite);
                break;

            case R.id.menu_filter:
                displayFilterDialog();
                break;

            case R.id.menu_log:
                adjustLayout();
                break;

            case android.R.id.home:
                finish();
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayFilterDialog() {
        if (filterDialog == null) {
            filterDialog = new DeviceFilterDialog(this, new DeviceFilterDialog.OnSubmitListener() {
                @Override
                public void onSubmit(String name,
                                     boolean filterName,
                                     int rssi,
                                     boolean filterRssi) {
                    filterDevices(name, filterName, rssi, filterRssi);
                }
            });
        }
        filterDialog.show();
    }

    void filterDevices(String name, boolean filterName, int rssi, boolean filterRssi) {
        devicesAdapter.filterDevices(name, filterName, rssi, filterRssi);
        devicesAdapter.setDebugMode();
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setHasFixedSize(true);
    }

    private void adjustLayout() { //TODO Fix animation (Almost ok)

        RelativeLayout logbody = (RelativeLayout) findViewById(R.id.log_body);

        float scale = getResources().getDisplayMetrics().density;
        // Setting FloatingActionButton and scanning container-----------------------------------------
        FloatingActionButton fab_btn = (FloatingActionButton) findViewById(R.id.fab_btn_refresh);
        ViewGroup.MarginLayoutParams fab = (ViewGroup.MarginLayoutParams) fab_btn.getLayoutParams();
        ViewGroup.MarginLayoutParams svg = (ViewGroup.MarginLayoutParams) scanningGradientContainer.getLayoutParams();
        // --------------------------------------------------------------------------------------------

        if (logbody.getVisibility() == View.GONE) {
            logbody.setVisibility(View.VISIBLE);
            Log.i("adjustLayout", "Creating View");
            running = true;
            startlog();
            // Move FloatingActionButton to top of log_body -----------------------------------------------
            fab.setMargins(0, 0, (int) ((scale * 15) + 0.5f), (int) ((scale * 315) + 0.5f));
            fab_btn.setLayoutParams(fab);
            svg.setMargins(0, 0, 0, (int) ((scale * 300) + 0.5f));
            scanningGradientContainer.setLayoutParams(svg);
            animateScanningBarFlyOut();
            // ---------------------------------------------------------------------------------------------

        } else {
            Log.i("adjustLayout", "Hiding View");
            // Move FloatingActionButton to bottom of log_body -----------------------------------------------
            fab.setMargins(0, 0, (int) ((scale * 15) + 0.5f), (int) ((scale * 15) + 0.5f));
            fab_btn.setLayoutParams(fab);
            svg.setMargins(0, 0, 0, 0);
            animateScanningBarFlyIn();
            scanningGradientContainer.setLayoutParams(svg);
            animateScanningBarFlyOut();

            // ---------------------------------------------------------------------------------------------
            //scanGradientLeft.setVisibility(View.GONE);
            //scanGradientRight.setVisibility(View.GONE);
            //scanningGradientContainer.setVisibility(View.GONE);
            running = false;
            logbody.setVisibility(View.GONE);
        }
    }

    private void startlog() {
        if (logUpdate == null) {
            logUpdate = new Thread(new Runnable() {
                public void run() {
                    synchronized (this) {
                        try {
                            while (running) {
                                log();
                                Thread.sleep(250);
                            }
                        } catch (Exception e) {
                            Log.e("logUpdate()", "error: " + e);
                        }
                    }
                }
            });
            logUpdate.start();
        }
    }

    private void createFiles() {

        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "SiliconLabs_BGApp");
        //File path = new File(getExternalFilesDir(null),"SiliconLabs_BGApp");

        //if(!path.exists()) {
        Log.d("BG Folder", "Creating folder path");
        path.mkdirs();
        File logo = new File(path, "Welcome.txt");
        String welcome = "Welcome to Silicon Labs Blue Gecko App";
        try {
            FileWriter writer = new FileWriter(logo);
            writer.append(welcome);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e("CreatingFolder", "Error" + e);
        }
        //} else Log.d("BG Folder","Path already exists");

        File path2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                              "SiliconLabs_BGApp" + File.separator + "OTAFiles");

        //if(!path2.exists()) {
        Log.d("OTA Folder", "Creating folder path");
        path2.mkdirs(); //TODO Still not appearing on Windows 7
        File instructions = new File(path2, "OTAInstructions.txt");
        String inst = "Welcome to Silicon Labs Blue Gecko App - OTA Instruction" + "\n" + "\n" +
                "To start update applications and stacks you need to save the respective files in a subfolder in this folder: " +
                "\n" +
                "Example -> SiliconLabs_BGApp/OTAFiles/ExampleFolder/Example.ebl" + "\n" + "\n" +
                "Organize your folders the way you find better, remember the root OTAFiles is static, and the app do not accept a sub-subfolder from the root." +
                "\n" + "\n" +
                "Good OTA!";
        try {
            FileWriter writer = new FileWriter(instructions);
            writer.append(inst);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e("CreatingFolder", "Error" + e);
        }
        //} else Log.d("BG Folder","Path already exists");
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Log.i("handler","receiving message");
            String sentString = msg.getData().getString("what");
            tv = (TextView) findViewById(R.id.log_view);
            tv.setMovementMethod(new ScrollingMovementMethod());
            tv.append(sentString);
            tv.scrollTo(0, tv.getScrollY());
        }
    };

    private void log() {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            //Log.i("log()","creating log");
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(" I ") || line.contains(" E ") || line.contains(" D ")) { //TODO Log filter options
                    if (!line.contains("ViewRoot")) {
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                    }
                }
            }

            if (subtraction.toString().length() != stringBuilder.toString().length()) {
                //Log.i("log()","sending to handler");
                String result;
                if (subtraction.toString().length() > stringBuilder.toString().length()) {
                    result = stringBuilder.toString();
                } else {
                    result = stringBuilder.substring(subtraction.length(), stringBuilder.length());
                }
                subtraction = stringBuilder;
                Message m = new Message();
                Bundle b = new Bundle();
                b.putString("what", result);
                m.setData(b);
                mHandler.sendMessage(m);
            }
        } catch (IOException e) {
            Log.e("log()", "couldn't create log: " + e);
        }
    }

    public void sortlist(int comparator) {
        //Log.i("MADM","sortlist");
        devicesAdapter.sort(comparator);
        devicesAdapter.setDebugMode();
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setHasFixedSize(true);
    }

    public void filterbond() {
        devicesAdapter.updateWithBonded(devicesAdapter.getDevicesInfo());
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setHasFixedSize(true);
    }

    public String getdate(String string) {
        String currentDateTimeString = null;
        switch (string) {
            case "normal":
                currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                break;
            case "compact":
                SimpleDateFormat LOG_FILE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ssZ");
                currentDateTimeString = LOG_FILE_FORMAT.format(new Date());
                break;
        }
        return currentDateTimeString;
    }

    public File save_log() {
        final File path = new File(Environment.getExternalStorageDirectory(), "SiliconLabs_BGApp");
        final File file = new File(path + File.separator + "SiliconLabs." + getdate("compact") + ".txt");
        Thread save_log = new Thread(new Runnable() {
            public void run() {
                String savedata = tv.getText().toString();
                if (!path.exists()) {
                    path.mkdir();
                }
                BufferedWriter bw = null;
                try {
                    file.createNewFile();
                    bw = new BufferedWriter(new FileWriter(file), 1024);
                    bw.write(savedata);
                } catch (IOException e) {
                    Log.e("save_log()", "error saving log: " + e);
                } finally {
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            Log.e("save_log()", "error closing save_log(): " + e);
                        }
                    }
                }
            }
        });
        save_log.start();
        return file;
    }

    public void share_log() {
        Thread share_log = new Thread(new Runnable() {
            public void run() {
                String logdata = tv.getText().toString();
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SiliconLabs BGApp Log: " + getdate("normal"));
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, logdata);
                startActivity(Intent.createChooser(shareIntent, "Share SiliconLabs BGApp Log ..."));
            }
        });
        share_log.start();
    }

    public void clear_log() {
        tv = (TextView) findViewById(R.id.log_view);
        tv.setText("");
        try {
            Runtime.getRuntime().exec(new String[] { "logcat", "-c" });
            Log.i("clear_log()", "log cleaned");
            //restartlog();
        } catch (IOException e) {
            Log.e("clear_log()", "error clearing log: " + e);
        }
    }

    public void performSearch(String string) {
        Toast.makeText(getBaseContext(), "Search for " + string, Toast.LENGTH_SHORT).show();
        if (string.equals("")) {
            updateWithDevices(devicesAdapter.getDevicesInfo());
        } else {
            updateWithDevices(devicesAdapter.getDevicesInfo(), string);
        }
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setHasFixedSize(true);
    }

    @Override
    public void onRefresh() {
        // callback for swiperefreshlayout
        BluetoothAdapter defaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultBluetoothAdapter != null && defaultBluetoothAdapter.isEnabled() &&
                bluetoothEnableBar.getVisibility() == View.GONE) {
            startScanning();
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @OnClick(R.id.fab_btn_refresh)
    public void onRefreshBtnClick() {
        BluetoothAdapter defaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultBluetoothAdapter != null && defaultBluetoothAdapter.isEnabled() &&
                bluetoothEnableBar.getVisibility() == View.GONE) {
            Log.d("onRefreshBtnClick", "Called");
            flushContainer();
            noDevicesFound.setVisibility(View.VISIBLE);
            startScanning();
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void initDevicesRecyclerView() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        devicesRecyclerView.setLayoutManager(layoutManager);
        devicesAdapter = new DebugModeDeviceAdapter(this,
                                                    this,
                                                    new DeviceInfoViewHolder.Generator(R.layout.list_item_debug_mode_device) {
                                                        @Override
                                                        public DeviceInfoViewHolder generate(View itemView) {
                                                            return new DebugModeDeviceAdapter.ViewHolder(
                                                                    MainActivityDebugMode.this,
                                                                    itemView,
                                                                    MainActivityDebugMode.this);
                                                        }
                                                    });
        devicesAdapter.setDebugMode();
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setHasFixedSize(true);
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(android.R.color.holo_red_dark),
                                                getResources().getColor(android.R.color.holo_orange_dark),
                                                getResources().getColor(android.R.color.holo_orange_light),
                                                getResources().getColor(android.R.color.holo_red_light));
    }

    private void initExtraAdvertisementDialog() {
        dialogDeviceExtraAdvertisement = new Dialog(this);
        dialogDeviceExtraAdvertisement.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDeviceExtraAdvertisement.setContentView(R.layout.dialog_advertisement_details);
        dialogDeviceExtraAdvertisement.findViewById(R.id.close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogDeviceExtraAdvertisement.dismiss();
            }
        });
    }

    //Webview -------------------------
    private void initLicenseDialog() {
        dialogLicense = new Dialog(this);
        dialogLicense.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogLicense.setContentView(R.layout.dialog_about_silicon_labs_blue_gecko);
        WebView view = (WebView) dialogLicense.findViewById(R.id.menu_item_license);
        Button closeButton = (Button) dialogLicense.findViewById(R.id.close_about_btn);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogLicense.dismiss();
            }
        });

        view.loadUrl(ABOUT_DIALOG_HTML_ASSET_FILE_PATH);
    }

    private void startScanningAnimation() {
        animateRefreshFabFlyout();
        animateScanningBarFlyIn();
        swipeRefreshLayout.setRefreshing(true);
    }

    private void stopScanningAnimation() {
        if (connectingContainer.getVisibility() == View.GONE &&
                scanningGradientContainer.getVisibility() == View.VISIBLE &&
                fabBtnRefresh.getVisibility() == View.GONE) {
            animateScanningBarFlyOut();
            animateRefreshFabFlyIn();
        }

        swipeRefreshLayout.setRefreshing(false);
    }

    private void animateScanningBarFlyIn() {
        Animation animBarFlyin = AnimationUtils.loadAnimation(MainActivityDebugMode.this, R.anim.scanning_bar_fly_in);
        scanningGradientContainer.setVisibility(View.VISIBLE);

        animBarFlyin.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Animation animScanLeft = AnimationUtils.loadAnimation(MainActivityDebugMode.this,
                                                                      R.anim.scan_translate_left);
                scanGradientLeft.startAnimation(animScanLeft);

                Animation animScanRight = AnimationUtils.loadAnimation(MainActivityDebugMode.this,
                                                                       R.anim.scan_translate_right);
                scanGradientRight.startAnimation(animScanRight);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        scanningGradientContainer.startAnimation(animBarFlyin);
    }

    private void animateScanningBarFlyOut() {
        Animation animBarFlyout = AnimationUtils.loadAnimation(MainActivityDebugMode.this, R.anim.scanning_bar_fly_out);
        scanningGradientContainer.startAnimation(animBarFlyout);

        animBarFlyout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                scanGradientLeft.clearAnimation();
                scanGradientRight.clearAnimation();
                scanningGradientContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        scanningGradientContainer.startAnimation(animBarFlyout);
    }

    public void cancelScanning(View view) {
        onScanningStopped();
        animateScanningBarFlyOut();
    }

    private void animateRefreshFabFlyout() {
        Animation animFabFlyout = AnimationUtils.loadAnimation(MainActivityDebugMode.this, R.anim.refresh_fab_fly_out);
        animFabFlyout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fabBtnRefresh.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fabBtnRefresh.startAnimation(animFabFlyout);
    }

    private void animateRefreshFabFlyIn() {
        Animation animFabFlyin = AnimationUtils.loadAnimation(MainActivityDebugMode.this, R.anim.refresh_fab_fly_in);
        fabBtnRefresh.setVisibility(View.VISIBLE);
        fabBtnRefresh.startAnimation(animFabFlyin);
    }

    // Displays dialog and request user to enable Bluetooth
    private void bluetoothEnable() {
        if (bluetoothEnableDialog == null || !bluetoothEnableDialog.isShowing()) {
            bluetoothEnableDialog = Dialogs.showAlert(this.getText(R.string.no_bluetooth_dialog_title_text), this
                                                              .getText(R.string.no_bluetooth_dialog_text), this, getText(android.R.string.ok),
                                                      getText(android.R.string.cancel), new OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            Intent intentBluetooth = new Intent();
                            intentBluetooth.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
                            MainActivityDebugMode.this.startActivityForResult(intentBluetooth,
                                                                              BlUETOOTH_SETTINGS_REQUEST_CODE);
                        }
                    }, new OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            MainActivityDebugMode.this.finish();
                        }
                    });
        }
    }

    // Displays about dialog
    private void showAbout() {
        dialogLicense.show();
    }

    // Configures number of shown advertisement types
    private void configureFontScale() {
        float scale = getResources().getConfiguration().fontScale;
        if (lastScale != scale) {
            lastScale = scale;
            if (lastScale == Common.FONT_SCALE_LARGE) {
                Device.MAX_EXTRA_DATA = 2;
            } else if (lastScale == Common.FONT_SCALE_XLARGE) {
                Device.MAX_EXTRA_DATA = 1;
            } else {
                Device.MAX_EXTRA_DATA = 3;
            }
        }
    }

    private String advertData(BluetoothDeviceInfo device, String advertisementData) {
        if (device.getName() != null && !device.getName().equals("null")) {
            advertisementData += (device.getName());
            advertisementData += ("<br><small>LOCAL NAME</small><br><br>");
        }
        advertisementData += (device.getAddress());
        advertisementData += ("<br><small>MAC ADDRESS</small><br><br>");
        if (device.getBleFormat() != null && !device.getBleFormat().toString().equals("UNSPECIFIED")) {
            advertisementData += (device.getBleFormat());
            advertisementData += ("<br><small>BEACON FORMAT</small><br><br>");
        }
        advertisementData += Integer.toHexString(device.scanInfo.getScanRecord().getAdvertiseFlags());//todo
        advertisementData += ("<br><small>FLAGS</small><br><br>");
        advertisementData += Integer.toString(device.scanInfo.getRssi());
        advertisementData += ("<br><small>RSSI</small><br><br>");
        if (device.scanInfo.getScanRecord().getTxPowerLevel() > (-100)) {
            advertisementData += Integer.toString(device.scanInfo.getScanRecord().getTxPowerLevel());
            advertisementData += ("<br><small>TX POWER</small><br><br>");
        }
        if (device.scanInfo.getAdvertData() != null && !device.scanInfo.getAdvertData().toString().equals("{}")) {
            String advertdata = device.scanInfo.getAdvertData().toString();
            advertdata = advertdata.substring(1, advertdata.length() - 1);
            advertdata = advertdata.replaceAll(",", "\n");
            advertisementData += (advertdata);
            advertisementData += ("<br><small>ADVERT DATA</small><br><br>");
        }
        if (device.scanInfo.getScanRecord().getManufacturerSpecificData() != null &&
                !device.scanInfo.getScanRecord().getManufacturerSpecificData().toString().equals("{}")) {
            SparseArray<byte[]> manData = device.scanInfo.getScanRecord().getManufacturerSpecificData();
            String advertdata;
            for (int i = 0; i < manData.size(); ++i) {
                int key = manData.keyAt(i);
                byte[] value = manData.get(key);
                advertdata = ("<br><small>Key: </small>" + Converters.getHexValue((byte)key) +
                        " <br><small>Value: </small>" + Converters.getHexValue(value));
                advertisementData += advertdata;
            }
            advertisementData += ("<br><small>MANUFACTURER SPECIFIC DATA</small><br><br>");
        }
        if (device.scanInfo.getScanRecord().getServiceData() != null &&
                !device.scanInfo.getScanRecord().getServiceData().toString().equals("{}")) {
            String advertdata = device.scanInfo.getScanRecord().getServiceData().toString();
            advertdata = advertdata.substring(1, advertdata.length() - 1);
            advertdata = advertdata.replaceAll(",", "\n");
            advertisementData += (advertdata);
            advertisementData += ("<br><small>SERVICE DATA</small><br><br>");
        }
        if (device.scanInfo.getScanRecord().getServiceUuids() != null) {
            String advertdata = device.scanInfo.getScanRecord().getServiceUuids().toString();
            advertdata = advertdata.substring(1, advertdata.length() - 1);
            advertdata = advertdata.replaceAll(",", "\n");
            advertisementData += (advertdata);
            advertisementData += ("<br><small>SERVICES UUIDs</small><br><br>");
        }
        return advertisementData;
    }

    private String prepareAdvertisementTextWithBoldedPropertyNames(BluetoothDeviceInfo device) {
        String advertisementData = "";

        for (int i = 0; i < device.getAdvertData().size(); i++) {
            String data = device.getAdvertData().get(i);
            String[] advertiseData = data.split(":");
            String deviceProperty = "<b>" + advertiseData[1] + "</b><br><small>" + advertiseData[0] + "</small>";
            deviceProperty += "<br>";

            if (advertiseData.length > 2) {
                deviceProperty += " <small>";

                for (int j = 2; j < advertiseData.length; j++) {
                    deviceProperty += advertiseData[j];
                    if (j != advertiseData.length - 1) {
                        advertisementData += ":";
                    }
                }

                deviceProperty += "</small>";
                if (i != device.getAdvertData().size() - 1) {
                    deviceProperty += "<br>";
                }
            }

            advertisementData += (deviceProperty + "<br>");

        }
        // Info in advertisement data ----------------------------------------------
        advertisementData = advertData(device, advertisementData);
        //-------------------------------------------------------------------------

        advertisementData += ("<br>Scan Record:<br>");
        advertisementData += ("");

        byte[] rawBytes = device.scanInfo.getScanRecord().getBytes();
        for (int i = 0; i < rawBytes.length; i++) {
            advertisementData += Converters.getHexValue(rawBytes[i]);

            if (i != rawBytes.length - 1) {
                advertisementData += "&nbsp;";
            }

            if ((i + 1) % 8 == 0) {
                advertisementData += "<br>&nbsp;";
            }
        }
        advertisementData += ("");

        return advertisementData;
    }

    private String prepareAdvertisementRawData(BluetoothDeviceInfo device) {
        String advertisementData = "";
        // Info in advertisement data ----------------------------------------------
        advertisementData = advertData(device, advertisementData);
        // -------------------------------------------------------------------------
        advertisementData += ("Scan Record:<br>");
        advertisementData += ("[");

        byte[] rawBytes = device.scanInfo.getScanRecord().getBytes();
        for (int i = 0; i < rawBytes.length; i++) {
            if (rawBytes[i] == 0x00 && rawBytes[i + 1] == 0x00) {
                break;
            }
            advertisementData += Converters.getHexValue(rawBytes[i]);

            if (i != rawBytes.length - 1) {
                advertisementData += "&nbsp;";
            }

            if ((i + 1) % 8 == 0) {
                advertisementData += "<br>&nbsp;";
            }
        }
        advertisementData += ("]");

        return advertisementData;
    }

    // Displays scanning status in UI and starts scanning for new BLE devices
    private void startScanning() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            bluetoothEnable();
        }
        scanning = true;
        setScanningProgress(true);
        setScanningStatus(true);
        devicesAdapter.setRunUpdater(true);
        // Connected devices are not deleted from list
        reDiscover(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                onScanningStopped();
            }
        }, SCAN_PERIOD);
    }

    private void onScanningStopped() {
        Log.d("onScanningStopped", "Called");
        scanning = false;
        discovery.stopDiscovery(false);
        setScanningStatus(devicesAdapter.getItemCount() > 0);
        setScanningProgress(false);
        devicesAdapter.setRunUpdater(false);
        int numbDevicesCurrentlyDisplaying = devicesAdapter.getItemCount();

        if (numbDevicesCurrentlyDisplaying > 0) {
            noDevicesFound.setVisibility(View.GONE);
            lookingForDevicesBackgroundMessage.setVisibility(View.GONE);
        }
    }

    private void setScanningStatus(boolean foundDevices) {
        if (!foundDevices) {
            noDevicesFound.setVisibility(View.VISIBLE);
            lookingForDevicesBackgroundMessage.setVisibility(View.GONE);

            if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                startScanning();
            }
        }
    }

    private void setScanningProgress(boolean isScanning) {
        if (devicesAdapter.getItemCount() == 0) {
            if (isScanning) {
                lookingForDevicesBackgroundMessage.setVisibility(View.VISIBLE);
                noDevicesFound.setVisibility(View.GONE);
            } else {
                lookingForDevicesBackgroundMessage.setVisibility(View.GONE);
                noDevicesFound.setVisibility(View.VISIBLE);
            }
        }
        fabBtnRefresh.setEnabled(!isScanning);
        swipeRefreshLayout.setRefreshing(isScanning && (bluetoothEnableBar.getVisibility() == View.GONE));

        if (isScanning) {
            startScanningAnimation();
        } else {
            stopScanningAnimation();
        }
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Log.d("refreshDevice", "Called");
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                Log.d("refreshDevice", "bool: " + bool);
                return bool;
            }
        } catch (Exception localException) {
            Log.e("refreshDevice", "An exception occured while refreshing device");
        }
        return false;
    }

    @Override
    public void callbackSelected(BluetoothDeviceInfo device) {
        // callback for when a device has been selected in the list of devices

    }

    //for device's extra advertisement dialog to show, see DebugModeDeviceAdapter
    @Override
    public void showAdvertisementDialog(BluetoothDeviceInfo device) {
        // update dialog title to name of selected device
        ((TextView) dialogDeviceExtraAdvertisement.findViewById(R.id.device_name_title)).setText(TextUtils.isEmpty(
                device.getName()) ? "Unknown" : device.getName());

        TextView extraAdvertisement = (TextView) dialogDeviceExtraAdvertisement.findViewById(R.id.no_extra_ad_data_label);
        if (device.hasAdvertDetails()) {
            extraAdvertisement.setText(Html.fromHtml(prepareAdvertisementTextWithBoldedPropertyNames(device)));
            extraAdvertisement.setGravity(Gravity.LEFT | Gravity.TOP);
        } else {
            extraAdvertisement.setText(Html.fromHtml(prepareAdvertisementRawData(device)));
            extraAdvertisement.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }

        dialogDeviceExtraAdvertisement.show();
    }

    public void showConnectingAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectingContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // this onclick listener prevents list of devices in the background from being scrolled or acted upon
                    }
                });
                fabBtnRefresh.clearAnimation();
                scanningGradientContainer.clearAnimation();
                fabBtnRefresh.setVisibility(View.GONE);
                scanningGradientContainer.setVisibility(View.GONE);

                Animation connectingGradientAnimation = AnimationUtils.loadAnimation(MainActivityDebugMode.this,
                                                                                     R.anim.connection_translate_right);
                connectingContainer.setVisibility(View.VISIBLE);
                connectingGradientContainer.startAnimation(connectingGradientAnimation);
                Animation connectingBarFlyIn = AnimationUtils.loadAnimation(MainActivityDebugMode.this,
                                                                            R.anim.scanning_bar_fly_in);
                connectingBarContainer.startAnimation(connectingBarFlyIn);
            }
        });
    }

    public void hideConnectingAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                devicesAdapter.debugModeConnectingDevice = null;
                devicesAdapter.notifyDataSetChanged();

                connectingContainer.setVisibility(View.GONE);
                connectingGradientContainer.clearAnimation();

                fabBtnRefresh.clearAnimation();
                scanningGradientContainer.clearAnimation();
                animateRefreshFabFlyIn();
                scanningGradientContainer.setVisibility(View.GONE);
            }
        });
    }

    BlueToothService boundService;

    @Override
    public void connectToDevice(BluetoothDeviceInfo deviceInfo) {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            return;
        }

        if (scanning) {
            onScanningStopped();
        }

        if (deviceInfo == null) {
            Log.e("deviceInfo", "null");
            return;
        }

        if (bluetoothBinding != null) {
            bluetoothBinding.unbind();
        }

        final BluetoothDeviceInfo bluetoothDeviceInfo = deviceInfo;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                devicesAdapter.debugModeConnectingDevice = bluetoothDeviceInfo;
                devicesAdapter.notifyDataSetChanged();
            }
        });

        showConnectingAnimation();

        bluetoothBinding = new BlueToothService.Binding(this) {
            @Override
            protected void onBound(final BlueToothService service) {
                boundService = service;

                if (service.isGattConnected()) {
                    hideConnectingAnimation();
                    startActivity(new Intent(MainActivityDebugMode.this, DeviceServicesActivity.class));
                    return;
                }

                service.connectGatt(bluetoothDeviceInfo.device, false, new TimeoutGattCallback() {

                    @Override
                    public void onTimeout() {
                        Toast.makeText(MainActivityDebugMode.this,
                                       R.string.toast_connection_timed_out,
                                       Toast.LENGTH_SHORT).show();
                        hideConnectingAnimation();
                    }

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        hideConnectingAnimation();

                        if (status != BluetoothGatt.GATT_SUCCESS) {

                            final String deviceName = TextUtils.isEmpty(bluetoothDeviceInfo.getName()) ? "Unknown" : bluetoothDeviceInfo
                                    .getName();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar snackbar = Snackbar.make(coordinatorLayout,
                                                                      getString(R.string.debug_mode_connection_failed_snackbar,
                                                                                deviceName),
                                                                      Snackbar.LENGTH_SHORT);
                                    View snackbarLayout = snackbar.getView();
                                    TextView textView = (TextView) snackbarLayout.findViewById(android.support.design.R.id.snackbar_text);
                                    textView.setTextSize(11);
                                    textView.setGravity(Gravity.CENTER_VERTICAL);
                                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.debug_failed, 0, 0, 0);
                                    textView.setCompoundDrawablePadding(getResources().getDimensionPixelOffset(R.dimen.debug_mode_device_selection_snackbar_padding));
                                    snackbar.show();
                                }
                            });
                        } else if (newState == BluetoothGatt.STATE_CONNECTED) {
                            //refreshDeviceCache(gatt);
                            if (service.isGattConnected()) {
                                startActivity(new Intent(MainActivityDebugMode.this, DeviceServicesActivity.class));
                            }
                        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                            Log.d("STATE_DISCONNECTED", "Called");
                            gatt.close();
                            service.clearGatt();
                            bluetoothBinding.unbind();
                        }
                    }
                });
            }
        };
        BlueToothService.bind(bluetoothBinding);
    }

    @Override
    public void disconnectFromDevice(BluetoothDeviceInfo device) {
        Toast.makeText(MainActivityDebugMode.this,
                       R.string.toast_debug_main_disconnected_from_device,
                       Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean isReady() {
        return !isFinishing();
    }

    @Override
    public void reDiscover() {
        reDiscover(false);
    }

    boolean updateListWhenAdapterIsReady = false;

    @Override
    public void onAdapterDisabled() {

    }

    @Override
    public void onAdapterEnabled() {

    }

    public void showEnableBluetoothAdapterBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_disabled);
                bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson_darker));
                bluetoothEnableBtn.setVisibility(View.VISIBLE);
                bluetoothEnableBar.setVisibility(View.VISIBLE);
                bluetoothEnableBar.postInvalidate();
            }
        });
    }

    public void changeEnableBluetoothAdapterToConnecing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter.getDefaultAdapter().enable();
                updateListWhenAdapterIsReady = true;
                bluetoothEnableBtn.setVisibility(View.GONE);
                bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_turning_on);
                bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.cerulean)); //TODO bluetooth bar color
            }
        });
    }

    @Override
    public void flushContainer() {
        devicesAdapter.flushContainer();
    }

    @Override
    public void updateWithDevices(List devices) {
        devicesAdapter.updateWith(devices);
        if (devicesAdapter.getItemCount() > 0) {
            lookingForDevicesBackgroundMessage.setVisibility(View.GONE);
            noDevicesFound.setVisibility(View.GONE);
        } else {
            lookingForDevicesBackgroundMessage.setVisibility(View.VISIBLE);
        }
    }

    public void updateWithDevices(List devices, String string) {
        devicesAdapter.updateWith(devices, string);
        if (devicesAdapter.getItemCount() > 0) {
            lookingForDevicesBackgroundMessage.setVisibility(View.GONE);
            noDevicesFound.setVisibility(View.GONE);
        } else {
            lookingForDevicesBackgroundMessage.setVisibility(View.VISIBLE);
        }
    }
    //}

    public void reDiscover(boolean clearCachedDiscoveries) {
        discovery.startDiscovery(clearCachedDiscoveries);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("MainActivityDebugMode Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //Log.d("onStop","Called");
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

}
