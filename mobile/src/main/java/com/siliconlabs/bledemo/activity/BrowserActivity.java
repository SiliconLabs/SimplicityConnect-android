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

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.ConnectionsAdapter;
import com.siliconlabs.bledemo.adapters.DebugModeDeviceAdapter;
import com.siliconlabs.bledemo.adapters.DeviceInfoViewHolder;
import com.siliconlabs.bledemo.adapters.LogAdapter;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.ble.Discovery;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Common;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Converters;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Device;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Engine;
import com.siliconlabs.bledemo.dialogs.Dialogs;
import com.siliconlabs.bledemo.fragment.LogFragment;
import com.siliconlabs.bledemo.fragment.SearchFragment;
import com.siliconlabs.bledemo.interfaces.DebugModeCallback;
import com.siliconlabs.bledemo.interfaces.ServicesConnectionsCallback;
import com.siliconlabs.bledemo.log.TimeoutLog;
import com.siliconlabs.bledemo.toolbars.ConnectionsFragment;
import com.siliconlabs.bledemo.toolbars.FilterFragment;
import com.siliconlabs.bledemo.toolbars.LoggerFragment;
import com.siliconlabs.bledemo.toolbars.ToolbarCallback;
import com.siliconlabs.bledemo.utils.Constants;
import com.siliconlabs.bledemo.utils.FilterDeviceParams;
import com.siliconlabs.bledemo.utils.SharedPrefUtils;
import com.siliconlabs.bledemo.utils.ToolbarName;

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

public class BrowserActivity extends BaseActivity implements DebugModeCallback, SwipeRefreshLayout.OnRefreshListener, Discovery.BluetoothDiscoveryHost, Discovery.DeviceContainer, ServicesConnectionsCallback {
    // static vars
    public static final String ABOUT_DIALOG_HTML_ASSET_FILE_PATH = "file:///android_asset/about.html";
    private static final int BlUETOOTH_SETTINGS_REQUEST_CODE = 100;
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
    private SharedPrefUtils sharedPrefUtils;
    private ConnectionsFragment connectionsFragment;
    private ConnectionsAdapter connectionsAdapter;
    private LoggerFragment loggerFragment;
    private boolean scanning = false;
    private boolean userStartScanning = true;
    private volatile boolean running = false;

    private static final int TOOLBAR_OPEN_PERCENTAGE = 95;
    private static final int TOOLBAR_CLOSE_PERCENTACE = 95;

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
                        Toast.makeText(BrowserActivity.this,
                                R.string.toast_bluetooth_not_enabled,
                                Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        showEnableBluetoothAdapterBar();
                        finish();
                        //flushContainer();
                        //noDevicesFound.setVisibility(View.VISIBLE);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_TURNING_ON:
                        isBluetoothAdapterEnabled = false;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (defaultBluetoothAdapter != null &&
                                defaultBluetoothAdapter.isEnabled()) {
                            if (!isBluetoothAdapterEnabled) {
                                Toast.makeText(BrowserActivity.this,
                                        R.string.toast_bluetooth_enabled,
                                        Toast.LENGTH_SHORT).show();
                            }

                            updateListWhenAdapterIsReady = false;
                            bluetoothEnableBar.setVisibility(View.GONE);

                            discovery.disconnect();
                            discovery.connect(BrowserActivity.this);
                            startScanning();
                        }
                        isBluetoothAdapterEnabled = true;
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

    @InjectView(R.id.recyclerview_debug_devices)
    public RecyclerView devicesRecyclerView;


    // BluetoothBrowserToolbar view elements
    @InjectView(R.id.linearlayout_log)
    LinearLayout logLL;
    @InjectView(R.id.linearlayout_connections)
    LinearLayout connectionsLL;
    @InjectView(R.id.linearlayout_filter)
    LinearLayout filterLL;
    @InjectView(R.id.frame_layout)
    FrameLayout frameLayout;
    @InjectView(R.id.framelayout_container)
    RelativeLayout frameLayoutContainerRL;
    @InjectView(R.id.imageview_log)
    ImageView logIV;
    @InjectView(R.id.imageview_connections)
    ImageView connectionsIV;
    @InjectView(R.id.imageview_filter)
    ImageView filterIV;
    @InjectView(R.id.textview_log)
    TextView logTV;
    @InjectView(R.id.textview_connections)
    TextView connectionsTV;
    @InjectView(R.id.textview_filter)
    TextView filterTV;
    @InjectView(R.id.imageview_filter_start)
    ImageView filterStartIV;
    @InjectView(R.id.bluetooth_browser_background)
    RelativeLayout bluetoothBrowserBackgroundRL;
    @InjectView(R.id.button_scanning)
    Button scanningButton;

    private boolean btToolbarOpened = false;
    private ToolbarName btToolbarOpenedName = null;
    private TextView tv;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefUtils = new SharedPrefUtils(getApplicationContext());
        // prepare ui
        setContentView(R.layout.activity_browser);
        ButterKnife.inject(this);

        // init bluetoooth discovery engine, matches to accepted bluetooth gatt profiles
        Engine.getInstance().init(this.getApplicationContext());

        // init/config ui
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setShowSpinnerDialogVisibility(false);

        initLicenseDialog();
        initExtraAdvertisementDialog();
        initDevicesRecyclerView();
        initSwipeRefreshLayout();

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

        fragmentsInit();
        handleToolbarClickEvents();
        bluetoothBrowserBackgroundRL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btToolbarOpened) {
                    closeToolbar();
                    btToolbarOpened = !btToolbarOpened;
                }
            }
        });

    }

    private void fragmentsInit() {
        loggerFragment = new LoggerFragment().setCallback(new ToolbarCallback() {
            @Override
            public void close() {
                closeToolbar();
                btToolbarOpened = !btToolbarOpened;
            }

            @Override
            public void submit(FilterDeviceParams filterDeviceParams, boolean close) {

            }
        });
        loggerFragment.setAdapter(new LogAdapter(Constants.LOGS, getApplicationContext()));


        connectionsFragment = new ConnectionsFragment().setCallback(new ToolbarCallback() {
            @Override
            public void close() {
                closeToolbar();
                btToolbarOpened = !btToolbarOpened;
                devicesAdapter.notifyDataSetChanged();
                updateCountOfConnectedDevices();
            }

            @Override
            public void submit(FilterDeviceParams filterDeviceParams, boolean close) {

            }
        });
        connectionsAdapter = new ConnectionsAdapter(getConnectedBluetoothDevices(), getApplicationContext());
        connectionsFragment.setAdapter(connectionsAdapter);
        connectionsFragment.getAdapter().setServicesConnectionsCallback(this);

    }

    @Override
    public void onDisconnectClicked(final BluetoothDeviceInfo deviceInfo) {
        bluetoothBinding = new BlueToothService.Binding(getApplicationContext()) {
            @Override
            protected void onBound(BlueToothService service) {
                boolean successDisconnected = service.disconnectGatt(deviceInfo.getAddress());
                if (!successDisconnected) {
                    Toast.makeText(getApplicationContext(), R.string.device_not_from_EFR, Toast.LENGTH_LONG).show();
                }
                updateCountOfConnectedDevices();
                devicesAdapter.notifyDataSetChanged();
            }
        };
        BlueToothService.bind(bluetoothBinding);
    }

    @Override
    public void onDeviceClicked(BluetoothDeviceInfo device) {
        connectToDevice(device);
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

        scanningGradientContainer.setVisibility(View.VISIBLE);

        //flushContainer();
        //noDevicesFound.setVisibility(View.VISIBLE);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            if (userStartScanning) {
                startScanning();
                scanningButton.setText(getResources().getText(R.string.Stop_Scanning));
                scanningButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(BrowserActivity.this, R.color.silabs_red)));
            }
        } else {
            showEnableBluetoothAdapterBar();
        }

        if (bluetoothAdapter != null) {
            isBluetoothAdapterEnabled = bluetoothAdapter.isEnabled();
            if (boundService != null) {
                Log.d("OnResume", "Called");
//                boundService.clearGatt();
                boundService = null;

            }
            if (bluetoothBinding != null) {
                bluetoothBinding.unbind();
            }
        } else {
            isBluetoothAdapterEnabled = false;
        }
        updateCountOfConnectedDevices();
        devicesAdapter.notifyDataSetChanged();
    }

    private void handleToolbarClickEvents() {

        setToolbarItemsNotClicked();

        logLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btToolbarOpened && btToolbarOpenedName == ToolbarName.LOGS) {
                    closeToolbar();
                    btToolbarOpened = !btToolbarOpened;
                    return;
                }
                if (!btToolbarOpened) {
                    bluetoothBrowserBackgroundRL.setBackgroundColor(Color.parseColor("#99000000"));
                    bluetoothBrowserBackgroundRL.setVisibility(View.VISIBLE);
                    ViewCompat.setTranslationZ(bluetoothBrowserBackgroundRL, 4f);
                    animateToolbarOpen(TOOLBAR_OPEN_PERCENTAGE, 300);
                    btToolbarOpened = !btToolbarOpened;
                }
                setToolbarItemsNotClicked();
                setToolbarItemClicked(logIV, logTV);
                btToolbarOpenedName = ToolbarName.LOGS;
                setToolbarFragment(loggerFragment);
            }
        });

        connectionsLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btToolbarOpened && btToolbarOpenedName == ToolbarName.CONNECTIONS) {
                    closeToolbar();
                    btToolbarOpened = !btToolbarOpened;
                    return;
                }
                if (!btToolbarOpened) {
                    bluetoothBrowserBackgroundRL.setBackgroundColor(Color.parseColor("#99000000"));
                    bluetoothBrowserBackgroundRL.setVisibility(View.VISIBLE);
                    ViewCompat.setTranslationZ(bluetoothBrowserBackgroundRL, 4f);
                    animateToolbarOpen(TOOLBAR_OPEN_PERCENTAGE, 300);
                    btToolbarOpened = !btToolbarOpened;
                }
                setToolbarItemsNotClicked();
                setToolbarItemClicked(connectionsIV, connectionsTV);
                btToolbarOpenedName = ToolbarName.CONNECTIONS;
                setToolbarFragment(connectionsFragment);
            }
        });

        filterLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btToolbarOpened && btToolbarOpenedName == ToolbarName.FILTER) {
                    closeToolbar();
                    btToolbarOpened = !btToolbarOpened;
                    return;
                }
                if (!btToolbarOpened) {
                    bluetoothBrowserBackgroundRL.setBackgroundColor(Color.parseColor("#99000000"));
                    bluetoothBrowserBackgroundRL.setVisibility(View.VISIBLE);
                    ViewCompat.setTranslationZ(bluetoothBrowserBackgroundRL, 4f);
                    animateToolbarOpen(TOOLBAR_OPEN_PERCENTAGE, 300);
                    btToolbarOpened = !btToolbarOpened;
                }
                setToolbarItemsNotClicked();
                setToolbarItemClicked(filterIV, filterTV);
                btToolbarOpenedName = ToolbarName.FILTER;
                setToolbarFragment(new FilterFragment().setCallback(new ToolbarCallback() {
                    @Override
                    public void close() {
                        closeToolbar();
                        btToolbarOpened = !btToolbarOpened;
                    }

                    @Override
                    public void submit(FilterDeviceParams filterDeviceParams, boolean close) {
                        if (close) {
                            closeToolbar();
                            btToolbarOpened = !btToolbarOpened;
                        }
                        filterStartIV.setVisibility(filterDeviceParams.isEmptyFilter() ? View.GONE : View.VISIBLE);
                        filterDevices(filterDeviceParams);
                    }
                }));
            }
        });

    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void closeToolbar() {
        animateToolbarClose(TOOLBAR_CLOSE_PERCENTACE, 300);
        setToolbarItemsNotClicked();
        bluetoothBrowserBackgroundRL.setVisibility(View.GONE);
        hideKeyboard();
    }

    private void setToolbarItemClicked(ImageView imageView, TextView textView) {
        textView.setTextColor(ContextCompat.getColor(this, R.color.silabs_blue));
        DrawableCompat.setTint(imageView.getDrawable(), ContextCompat.getColor(this, R.color.silabs_blue));
    }

    private void setToolbarItemsNotClicked() {
        logTV.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text));
        DrawableCompat.setTint(logIV.getDrawable(), ContextCompat.getColor(this, R.color.silabs_primary_text));

        filterTV.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text));
        DrawableCompat.setTint(filterIV.getDrawable(), ContextCompat.getColor(this, R.color.silabs_primary_text));

        connectionsTV.setTextColor(ContextCompat.getColor(this, R.color.silabs_primary_text));
        DrawableCompat.setTint(connectionsIV.getDrawable(), ContextCompat.getColor(this, R.color.silabs_primary_text));
    }

    private void animateToolbarOpen(int openPercentHeight, int duration) {
        ValueAnimator animator = ValueAnimator.ofInt(0, percentHeightToPx(openPercentHeight)).setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                frameLayout.getLayoutParams().height = value.intValue();
                frameLayout.requestLayout();
            }
        });

        frameLayout.setVisibility(View.VISIBLE);
        ViewCompat.setTranslationZ(frameLayoutContainerRL, 5f);
        AnimatorSet set = new AnimatorSet();
        set.play(animator);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    private void animateToolbarClose(int openPercentHeight, int duration) {
        ValueAnimator animator = ValueAnimator.ofInt(percentHeightToPx(openPercentHeight), 0).setDuration(duration);


        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                frameLayout.getLayoutParams().height = value.intValue();
                frameLayout.requestLayout();
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.play(animator);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    private int percentHeightToPx(int percent) {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException();
        int height = devicesRecyclerView.getHeight() + scanningGradientContainer.getHeight();
        return (int) (((float) percent / 100.0) * height);
    }

    private void setToolbarFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("onPause", "Called");
        if (scanning) {
            onScanningStopped();
            scanningButton.setText(getResources().getString(R.string.Start_Scanning));
            scanningButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(BrowserActivity.this, R.color.silabs_blue)));
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

        bluetoothBinding = new BlueToothService.Binding(getApplicationContext()) {
            @Override
            protected void onBound(BlueToothService service) {
                for (BluetoothDevice bd : getConnectedBluetoothDevices()) {
                    service.disconnectGatt(bd.getAddress());
                }
                service.clearGatt();
                bluetoothBinding.unbind();
            }
        };
        BlueToothService.bind(bluetoothBinding);

//        Log.d("onDestroy", "Called");
//        if (bluetoothBinding != null) {
//            bluetoothBinding.unbind();
//        }
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

            case R.id.menu_log:
                adjustLayout();
                break;

            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_mappings:
                Intent intent = new Intent(BrowserActivity.this, MappingDictionaryActivity.class);
                startActivity(intent);

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    void filterDevices(FilterDeviceParams filterDeviceParams) {
        devicesAdapter.filterDevices(filterDeviceParams, true);
        devicesAdapter.setDebugMode();
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setHasFixedSize(true);
    }

    @OnClick(R.id.button_scanning)
    public void onScanningButtonClicked() {

        if (scanning) {
            userStartScanning = false;
            scanningButton.setText(getResources().getString(R.string.Start_Scanning));
            scanningButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(BrowserActivity.this, R.color.silabs_blue)));
            onScanningStopped();
        } else {
            scanningButton.setText(getResources().getString(R.string.Stop_Scanning));
            scanningButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(BrowserActivity.this, R.color.silabs_red)));
            startScanning();
        }


    }

    private void adjustLayout() { //TODO Fix animation (Almost ok)

        RelativeLayout logbody = findViewById(R.id.log_body);

        if (logbody.getVisibility() == View.GONE) {
            logbody.setVisibility(View.VISIBLE);
            Log.i("adjustLayout", "Creating View");
            running = true;
            startlog();
        } else {
            Log.i("adjustLayout", "Hiding View");

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

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Log.i("handler","receiving message");
            String sentString = msg.getData().getString("what");
            tv = findViewById(R.id.log_view);
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
        devicesAdapter.sort(comparator, true);
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
        final File path = new File(Environment.getExternalStorageDirectory(), "SiliconLabs_EFRConnect");
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
        tv = findViewById(R.id.log_view);
        tv.setText("");
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-c"});
            Log.i("clear_log()", "log cleaned");
            //restartlog();
        } catch (IOException e) {
            Log.e("clear_log()", "error clearing log: " + e);
        }
    }

    public void performSearch(String string) {
        Toast.makeText(getBaseContext(), getResources().getString(R.string.Search_for_s, string), Toast.LENGTH_SHORT).show();
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

        onScanningStopped();
        flushContainer();
        userStartScanning = true;
        startScanning();

        scanningButton.setText(getResources().getText(R.string.Stop_Scanning));
        scanningButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(BrowserActivity.this, R.color.silabs_red)));

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    private void initDevicesRecyclerView() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        devicesRecyclerView.setLayoutManager(layoutManager);
        devicesAdapter = new DebugModeDeviceAdapter(this,
                this,
                new DeviceInfoViewHolder.Generator(R.layout.bluetooth_browser_device_item) {
                    @Override
                    public DeviceInfoViewHolder generate(View itemView) {
                        return new DebugModeDeviceAdapter.ViewHolder(
                                BrowserActivity.this,
                                itemView,
                                BrowserActivity.this);
                    }
                });
        devicesAdapter.setDebugMode();
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setHasFixedSize(true);
    }

    private void initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(BrowserActivity.this, android.R.color.holo_red_dark),
                ContextCompat.getColor(BrowserActivity.this, android.R.color.holo_orange_dark),
                ContextCompat.getColor(BrowserActivity.this, android.R.color.holo_orange_light),
                ContextCompat.getColor(BrowserActivity.this, android.R.color.holo_red_light));
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
        WebView view = dialogLicense.findViewById(R.id.menu_item_license);
        Button closeButton = dialogLicense.findViewById(R.id.close_about_btn);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogLicense.dismiss();
            }
        });

        view.loadUrl(ABOUT_DIALOG_HTML_ASSET_FILE_PATH);
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
                            BrowserActivity.this.startActivityForResult(intentBluetooth,
                                    BlUETOOTH_SETTINGS_REQUEST_CODE);
                        }
                    }, new OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            BrowserActivity.this.finish();
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
            StringBuilder advertisementDataBuilder = new StringBuilder(advertisementData);
            for (int i = 0; i < manData.size(); ++i) {
                int key = manData.keyAt(i);
                byte[] value = manData.get(key);
                advertdata = ("<br><small>Key: </small>" + Converters.getHexValue((byte) key) +
                        " <br><small>Value: </small>" + Converters.getHexValue(value));
                advertisementDataBuilder.append(advertdata);
            }
            advertisementData = advertisementDataBuilder.toString();
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
        StringBuilder advertisementData = new StringBuilder();

        for (int i = 0; i < device.getAdvertData().size(); i++) {
            String data = device.getAdvertData().get(i);
            String[] advertiseData = data.split(":");
            StringBuilder deviceProperty = new StringBuilder("<b>" + advertiseData[1] + "</b><br><small>" + advertiseData[0] + "</small>");
            deviceProperty.append("<br>");

            if (advertiseData.length > 2) {
                deviceProperty.append(" <small>");

                for (int j = 2; j < advertiseData.length; j++) {
                    deviceProperty.append(advertiseData[j]);
                    if (j != advertiseData.length - 1) {
                        advertisementData.append(":");
                    }
                }

                deviceProperty.append("</small>");
                if (i != device.getAdvertData().size() - 1) {
                    deviceProperty.append("<br>");
                }
            }

            advertisementData.append(deviceProperty).append("<br>");

        }
        // Info in advertisement data ----------------------------------------------
        advertisementData = new StringBuilder(advertData(device, advertisementData.toString()));
        //-------------------------------------------------------------------------

        advertisementData.append("<br>Scan Record:<br>");
        advertisementData.append("");

        byte[] rawBytes = device.scanInfo.getScanRecord().getBytes();
        for (int i = 0; i < rawBytes.length; i++) {
            advertisementData.append(Converters.getHexValue(rawBytes[i]));

            if (i != rawBytes.length - 1) {
                advertisementData.append("&nbsp;");
            }

            if ((i + 1) % 8 == 0) {
                advertisementData.append("<br>&nbsp;");
            }
        }
        advertisementData.append("");

        return advertisementData.toString();
    }

    private String prepareAdvertisementRawData(BluetoothDeviceInfo device) {
        StringBuilder advertisementData = new StringBuilder();
        // Info in advertisement data ----------------------------------------------
        advertisementData = new StringBuilder(advertData(device, advertisementData.toString()));
        // -------------------------------------------------------------------------
        advertisementData.append("Scan Record:<br>");
        advertisementData.append("[");

        byte[] rawBytes = device.scanInfo.getScanRecord().getBytes();
        for (int i = 0; i < rawBytes.length; i++) {
            if (rawBytes[i] == 0x00 && rawBytes[i + 1] == 0x00) {
                break;
            }
            advertisementData.append(Converters.getHexValue(rawBytes[i]));

            if (i != rawBytes.length - 1) {
                advertisementData.append("&nbsp;");
            }

            if ((i + 1) % 8 == 0) {
                advertisementData.append("<br>&nbsp;");
            }
        }
        advertisementData.append("]");

        return advertisementData.toString();
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
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Log.d("refreshDevice", "Called");
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");
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

    public void showConnectingAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scanningGradientContainer.setVisibility(View.GONE);

                Animation connectingGradientAnimation = AnimationUtils.loadAnimation(BrowserActivity.this,
                        R.anim.connection_translate_right);
                connectingContainer.setVisibility(View.VISIBLE);
                connectingGradientContainer.startAnimation(connectingGradientAnimation);
                Animation connectingBarFlyIn = AnimationUtils.loadAnimation(BrowserActivity.this,
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

                scanningGradientContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private BlueToothService boundService;

    @Override
    public void connectToDevice(final BluetoothDeviceInfo deviceInfo) {
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
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
////                devicesAdapter.debugModeConnectingDevice = bluetoothDeviceInfo;
////                devicesAdapter.notifyDataSetChanged();
//            }
//        });

        showConnectingAnimation();

        bluetoothBinding = new BlueToothService.Binding(this) {
            @Override
            protected void onBound(final BlueToothService service) {
                boundService = service;

                //fixme

                if (service.isGattConnected(deviceInfo.getAddress())) {
                    hideConnectingAnimation();
                    if (btToolbarOpened) {
                        closeToolbar();
                        btToolbarOpened = !btToolbarOpened;
                    }
                    Intent intent = new Intent(BrowserActivity.this, DeviceServicesActivity.class);
                    intent.putExtra("DEVICE_SELECTED_ADDRESS", deviceInfo.getAddress());
                    startActivity(intent);
                    return;
                }

                service.connectGatt(bluetoothDeviceInfo.device, false, new TimeoutGattCallback() {

                    @Override
                    public void onTimeout() {
                        Constants.LOGS.add(new TimeoutLog(bluetoothDeviceInfo.device));
                        Toast.makeText(BrowserActivity.this,
                                R.string.toast_connection_timed_out,
                                Toast.LENGTH_SHORT).show();
                        hideConnectingAnimation();
                    }

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, final int status, final int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        updateCountOfConnectedDevices();
                        service.gattMap.put(deviceInfo.getAddress(), gatt);
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
                                    TextView textView = snackbarLayout.findViewById(com.google.android.material.R.id.snackbar_text);
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

                                if (btToolbarOpened) {
                                    closeToolbar();
                                    btToolbarOpened = !btToolbarOpened;
                                }

                                Intent intent = new Intent(BrowserActivity.this, DeviceServicesActivity.class);
                                intent.putExtra("DEVICE_SELECTED_ADDRESS", deviceInfo.getAddress());
                                startActivity(intent);
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
    public void addToFavorite(String deviceAddress) {
        sharedPrefUtils.addDeviceToFavorites(deviceAddress);
    }

    @Override
    public void removeFromFavorite(String deviceAddress) {
        sharedPrefUtils.removeDeviceFromFavorites(deviceAddress);
    }

    @Override
    public void updateCountOfConnectedDevices() {
        final List<BluetoothDevice> connectedBluetoothDevices = getConnectedBluetoothDevices();
        final int size = connectedBluetoothDevices.size();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionsTV.setText(size + " Connections");
                connectionsFragment.getAdapter().setConnectionsList(connectedBluetoothDevices);
                connectionsFragment.getAdapter().notifyDataSetChanged();
            }
        });
    }

    private List<BluetoothDevice> getConnectedBluetoothDevices() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    }

    @Override
    public boolean isReady() {
        return !isFinishing();
    }

    @Override
    public void reDiscover() {
        reDiscover(false);
    }

    private boolean updateListWhenAdapterIsReady = false;

    @Override
    public void onAdapterDisabled() {

    }

    @Override
    public void onAdapterEnabled() {

    }

    private void showEnableBluetoothAdapterBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_disabled);
                bluetoothEnableBar.setBackgroundColor(ContextCompat.getColor(BrowserActivity.this, R.color.silabs_red_dark));
                bluetoothEnableBtn.setVisibility(View.VISIBLE);
                bluetoothEnableBar.setVisibility(View.VISIBLE);
                bluetoothEnableBar.postInvalidate();
            }
        });
    }

    private void changeEnableBluetoothAdapterToConnecing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothAdapter.getDefaultAdapter().enable();
                updateListWhenAdapterIsReady = true;
                bluetoothEnableBtn.setVisibility(View.GONE);
                bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_turning_on);
                bluetoothEnableBar.setBackgroundColor(ContextCompat.getColor(BrowserActivity.this, R.color.silabs_blue)); //TODO bluetooth bar color
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
                .setName("BrowserActivity Page") // TODO: Define a title for the content shown.
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
