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
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.ble.TimeoutGattCallback;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Characteristic;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Service;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Common;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Converters;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Engine;
import com.siliconlabs.bledemo.fragment.FragmentCharacteristicDetail;
import com.siliconlabs.bledemo.fragment.LogFragmentConnected;
import com.siliconlabs.bledemo.utils.BLEUtils;
import com.siliconlabs.bledemo.utils.BLEUtils.Notifications;
import com.siliconlabs.bledemo.views.ServiceItemContainer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DeviceServicesActivity extends AppCompatActivity {
    public static final String ABOUT_DIALOG_HTML_ASSET_FILE_PATH = "file:///android_asset/about.html";
    public static final String CHARACTERISTIC_ADD_FRAGMENT_TRANSACTION_ID = "characteristicdetail";
    public static final int UI_CREATION_DELAY = 500;
    private static final String PROPERTY_ICON_TAG = "characteristicpropertyicon";
    Handler handler;

    /**Services UUIDs*/
    UUID ota_service = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0");
    UUID ota_control = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063");
    UUID ota_data = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153");
    UUID fw_version = UUID.fromString("4f4a2368-8cca-451e-bfff-cf0e2ee23e9f");
    UUID ota_version = UUID.fromString("4cc07bcf-0868-4b32-9dad-ba4cc41e5316");
    UUID homekit_descriptor = UUID.fromString("dc46f0fe-81d2-4616-b5d9-6abdd796939a");
    UUID homekit_service = UUID.fromString("0000003e-0000-1000-8000-0026bb765291");


    private int previousFragmentId = 0;
    private BluetoothDevice bluetoothDevice = null;
    private int generatedId = 0;
    private boolean serviceHasBeenSet;
    private BlueToothService service;
    private BlueToothService.Binding bluetoothBinding;
    private BluetoothGatt bluetoothGatt;
    private Dialog dialogLicense;
    private Dialog newPriority;
    private Dialog newMTU;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothManager bluetoothManager = null;
    private BluetoothLeScanner bluetoothLeScanner = null;

    /**OTA MenuButton*/
    MenuItem ota_button;

    /**LOG*/
    private Thread logupdate;
    volatile boolean running = false;
    StringBuilder substraction = new StringBuilder();
    TextView tv;

    /**OTA Progress*/
    private Dialog otaProgress;
    CheckBox otaBegin;
    CheckBox otaupload;
    CheckBox otaEnd;
    ProgressBar progressBar;
    Chronometer chrono;
    TextView dataRate;
    TextView datasize;
    TextView filename;
    ProgressBar uploadimage;
    Button OTAStart;


    /**OTA Setup*/
    private Dialog otaSetup;
    Spinner folderSpinner;
    Spinner appSpinner;
    Spinner stackSpinner;
    Button partialOTA;
    Button fullOTA;
    Button OTA_OK;
    SeekBar requestMTU;
    SeekBar delaySeekBar;
    int delayNoResponse;
    TextView sizename;
    TextView mtuname;
    CheckBox reliableWrite;
    TextView delayText;
    int priority = 2;

    /**File Selection*/
    String OTApath = "";
    String appPath = "";
    String stackPath = "";
    String[] folder;
    String[] stringOTAapp;
    String[] stringOTAstack;

    /**Loading Dialog*/
    private Dialog loadingdialog;
    TextView loadingLog;
    TextView loadingHeader;
    ProgressBar loadingimage;

    /**Global Variables*/
    int MTU = 247;
    int MTU_divisible = 0;
    long otatime = 0;
    int pack = 0;
    byte[] otafile;
    String reconnectaddress;
    long delayToConnect=0;
    int onScanCallback=0;

    /**Global Booleans*/
    boolean reliable = true;
    boolean boolFullOTA = false;
    boolean boolOTAbegin = false;
    boolean connected = false;
    boolean boolOTAdata = false;
    boolean UICreated = false;
    boolean discoverTimeout = true;
    boolean ota_mode=false;
    boolean boolrequest_mtu = false;
    boolean ota_process=false;
    boolean boolrefresh_services=false;
    boolean disconnect_gatt = false;
    boolean disconnectionTimeout = false;
    boolean homekit = false;

    BluetoothGattDescriptor kit_descriptor;

    FragmentCharacteristicDetail previousCharacteristicFragment = null;
    LinearLayout previousFragmentContainer = null;
    ImageView previousCharacteristicCaret = null;
    LinearLayout previousCharacteristicContainer = null;

    private Map<String, ServiceItemContainer> serviceItemContainers;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.services_container)
    LinearLayout servicesContainer;
    @InjectView(R.id.scrollViewWrapper)
    RelativeLayout scrollViewWrapper;
    @InjectView(R.id.loading_container)
    public RelativeLayout loadingContainer;
    @InjectView(R.id.loading_anim_gradient_right_container)
    public LinearLayout loadingGradientContainer;
    @InjectView(R.id.loading_bar_container)
    public RelativeLayout loadingBarContainer;


    /**BLUETOOTH GATT CALLBACKS*********************************************************/
    private TimeoutGattCallback gattCallback = new TimeoutGattCallback() {
        @Override
        public void onTimeout() {
            super.onTimeout();
            Log.d("gattCallback", "onTimeout");
        }

        @Override //CALLBACK TO REQUEST MTU
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d("onMtuChanged", "MTU: " + mtu + " - status: " + status);

            if (status == 0) { //NO ERRORS
                MTU = mtu;
                if (Build.VERSION.SDK_INT >= 21) {
                    Log.d("Connected", "PRIORITY: " + bluetoothGatt.requestConnectionPriority(priority));
                }
                if (boolrequest_mtu) { //Request MTU From button menu
                    MTUonButtonMenu();
                } else if (ota_process && !boolrequest_mtu) {
                    if (ota_mode && newMTU.isShowing()) { //Reopen OTA Setup
                        reopenOTASetup();
                    }
                    if (ota_mode) { //Reset OTA Progress
                        resetOTAProgress();
                    }
                }
            }

            else { //ERROR HANDLING
                final int error = status;
                Log.d("RequestMTU","Error: " + error);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "ERROR REQUESTING MTU: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disconnectGatt(bluetoothGatt);
                    }
                },2000);
            }
        }

        @Override //CALLBACK ON CONNECTION STATUS CHANGES
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("onConnectionStateChange","status = " + status + " - newState = " + newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED: //Handling Connections
                    connected = true;
                    Log.d("onConnectionStateChange", "CONNECTED");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!loadingdialog.isShowing()) {
                                Toast.makeText(DeviceServicesActivity.this, "DEVICE CONNECTED", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    if (ota_process) { //After OTA process started
                        Log.d("Address", "" + gatt.getDevice());
                        Log.d("Name", "" + gatt.getDevice().getName());

                       if (gatt.getServices().isEmpty()){
                           handler.postDelayed(new Runnable() {
                               @Override
                               public void run() {
                                   bluetoothGatt = null; //It's going to be equal gatt in Discover Services Callback...
                                   Log.d("onConnected", "Start Services Discovery: " + gatt.discoverServices());
                               }
                           },250);
                            discoverTimeout = true;
                            Runnable timeout = new Runnable(){ //Discover Services Timeout
                                @Override
                                public void run() {
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (discoverTimeout) {
                                                disconnectGatt(gatt);
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(getBaseContext(),"DISCOVER SERVICES TIMEOUT", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            }
                                        }
                                    },15000);
                                }
                            }; new Thread(timeout).start();
                       }
                    }
                    break;
                case BluetoothGatt.STATE_DISCONNECTED://Handling Disonnections
                    connected = false;
                    discoverTimeout = false;
                    final int error = status;
                    disconnectionTimeout = false;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(disconnect_gatt) {
                                exit(gatt);
                            }
                            if (loadingdialog!=null) if(!loadingdialog.isShowing()) {
                                Toast.makeText(DeviceServicesActivity.this, R.string.toast_debug_mode_device_disconnected, Toast.LENGTH_LONG).show();
                                if (error > 0) {
                                    Toast.makeText(getBaseContext(), connectionErrorHandling(error), Toast.LENGTH_LONG).show();
                                }
                            }
                            if (ota_process || boolOTAbegin || boolFullOTA) {
                                if (loadingdialog.isShowing()) {
                                    loadingLog.setText("Rebooting...");
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    loadingLog.setText("Waiting...");
                                                }
                                            });
                                        }
                                    }, 1500);
                                }
                            }
                            if (otaSetup!=null) if(otaSetup.isShowing()){
                                exit(gatt);
                            }
                        }
                    });

                    if (gatt != null && gatt.getServices().isEmpty()){
                        exit(gatt);

                    }
                    if (gatt != null && !boolFullOTA && !boolOTAbegin && !ota_process) {
                        exit(gatt);
                    }
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    Log.d("onConnectionStateChange", "Connecting...");
                    break;
            }
        }

        @Override //CALLBACK ON CHARACTERISTIC READ
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (previousCharacteristicFragment != null) {
                previousCharacteristicFragment.onActionDataAvailable(characteristic.getUuid().toString());
            }

            Log.i("Callback","OnCharacteristicRead: " + Converters.getHexValue(characteristic.getValue()) + " Status: " + status);

            if (characteristic == (bluetoothGatt.getService(ota_service).getCharacteristic(ota_control))) {
                byte[] value = characteristic.getValue();
                if (value[2] == (byte) 0x05){
                    Log.d("homekit_descriptor", "Insecure Connection");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(),"Error: Not a Homekit Secure Connection",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else if (value[2] == (byte) 0x04){
                    Log.d("homekit_descriptor", "Wrong Address");
                }
                else if (value[2] == (byte) 0x00){
                    Log.d("homekit_descriptor", "Entering in DFU_Mode...");
                    if (ota_mode && ota_process) {
                        Log.d("OTAUPLOAD", "Sent");
                        runOnUiThread(checkbeginrunnable);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                DFUMode("OTAUPLOAD");
                            }
                        }, 500);
                    } else if (!ota_mode && ota_process) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingLog.setText("Resetting...");
                                showLoading();
                                animaloading();
                                ota_button.setVisible(true);
                            }
                        });
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                reconnect(4000);
                            }
                        }, 200);
                    }
                }
            }
        }

        @Override //CALLBACK ON CHARACTERISTIC WRITE (PROPERTY: WHITE)
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (previousCharacteristicFragment != null) {
                previousCharacteristicFragment.onActionDataWrite(characteristic.getUuid().toString(), status);
            }

            if(characteristic.getValue().length<10)
            Log.d ("OnCharacteristicRead","Char: " + characteristic.getUuid().toString() + " Value: " + Converters.getHexValue(characteristic.getValue()) + " Status: " + status);

            if (status != 0){ // Error Handling
                Log.d("onCharWrite", "status: " + Integer.toHexString(status));
                final int error = status;
                runOnUiThread(new Runnable() { //Display error on Toast
                      @Override
                      public void run() {
                          Toast.makeText(getBaseContext(), charErrorHandling(error), Toast.LENGTH_LONG).show();
                      }
                });

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disconnectGatt(bluetoothGatt);
                    }
                },3000);

            } else {

                if (characteristic.getUuid().equals(ota_control)) { //OTA Control Callback Handling
                    if (characteristic.getValue().length==1) {
                        if (characteristic.getValue()[0] == (byte) 0x00) {
                            Log.d("Callback", "Control " + Converters.getHexValue(characteristic.getValue()) + "status: " + status);
                            if (ota_mode && ota_process) {
                                Log.d("OTAUPLOAD", "Sent");
                                runOnUiThread(checkbeginrunnable);
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        DFUMode("OTAUPLOAD");
                                    }
                                }, 500);
                            } else if (!ota_mode && ota_process) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingLog.setText("Resetting...");
                                        showLoading();
                                        animaloading();
                                        ota_button.setVisible(true);
                                    }
                                });
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        reconnect(4000);
                                    }
                                });
                            }
                        }
                        if (characteristic.getValue()[0] == (byte) 0x03) {
                            if (ota_process) {
                                Log.d("Callback", "Control " + Converters.getHexValue(characteristic.getValue()) + "status: " + status);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        OTAStart.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson));
                                        otaEnd.setChecked(true);
                                        OTAStart.setClickable(true);

                                    }
                                });
                                boolOTAbegin = false;
                                if (boolFullOTA) {
                                    stackPath = "";
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            otaProgress.dismiss();
                                            loadingLog.setText("Loading");
                                            showLoading();
                                            animaloading();
                                        }
                                    });

                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            reconnect(4000);
                                        }
                                    }, 500);
                                }
                            }
                        }
                    } else {
                        Log.i("OTA_Control","Received: " + Converters.getHexValue(characteristic.getValue()));
                        if (characteristic.getValue()[0]==0x00 && characteristic.getValue()[1]==0x02) {
                            Log.i("HomeKit","Reading OTA_Control...");
                            bluetoothGatt.readCharacteristic(characteristic);
                        }
                    }
                }

                if (characteristic.getUuid().equals(ota_data)) {   //OTA Data Callback Handling
                    if (reliable) {
                        if (otaProgress.isShowing()) {
                            pack += MTU_divisible;
                            if (pack <= otafile.length - 1) {
                                //Log.d("callback", "pack: " + (pack - MTUheader) + " / " + pack + " : " + Converters.getHexValue(characteristic.getValue()));
                                //Log.d("callback", "" + status);
                                otaWriteDataReliable();
                            } else if (pack > otafile.length - 1) {
                                //Log.d("callback", "last: " + pack + " / " + otafile.length + " : " + Converters.getHexValue(characteristic.getValue()));
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                otaupload.setChecked(true);
                                                chrono.stop();
                                                uploadimage.clearAnimation();
                                                uploadimage.setVisibility(View.INVISIBLE);
                                            }
                                        });
                                    }
                                });

                                boolOTAdata = false;
                                DFUMode("OTAEND");

                            }
                        }
                    }
                }
            }
        }

        @Override //CALLBACK ON DESCRIPTOR WRITE
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (previousCharacteristicFragment != null) {
                previousCharacteristicFragment.onDescriptorWrite(descriptor.getUuid());
            }
        }

        @Override //CALLBACK ON DESCRIPTOR READ
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            if (descriptor.getUuid().toString().equals(homekit_descriptor.toString())){

                byte[] value = new byte[2];
                value[0] = (byte) 0xF2;
                value[1] = (byte) 0xFF;

                if (descriptor.getValue()[0] == value[0] && descriptor.getValue()[1] == value[1]){

                    Log.i("descriptor","getValue " + Converters.getHexValue(descriptor.getValue()));
                    homeKitOTAControl(descriptor.getValue());

                }
            }
        }

        @Override //CALLBACK ON CHARACTERISTIC CHANGED VALUE (READ - CHARACTERISTIC NOTIFICATION)
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (previousCharacteristicFragment != null) {
                previousCharacteristicFragment.onActionDataAvailable(characteristic.getUuid().toString());
            }
        }

        @Override //CALLBACK ON SERVICES DISCOVERED
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (bluetoothGatt != gatt) {
                bluetoothGatt = gatt;
                refreshServices();
            } else {
                discoverTimeout = false;
                /**ERROR IN SERVICE DISCOVERY*/
                if (status != 0) {
                    Log.d("Error status", "" + Integer.toHexString(status));
                    final int error = status;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), charErrorHandling(error), Toast.LENGTH_LONG).show();
                        }
                    });


                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            disconnectGatt(bluetoothGatt);
                        }
                    }, 2000);

                } else {
                    /**ON SERVICE DISCOVERY WITHOUT ERROR*/

                    getServicesInfo(gatt); //SHOW SERVICES IN LOG

                    final BluetoothGatt btGatt = gatt;

                    //REFRESH SERVICES UI <- REFRESH SERVICES MENU BUTTON
                    if (boolrefresh_services) {
                        boolrefresh_services = false;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        onGattFetched();
                                        hideCharacteristicLoadingAnimation();
                                    }
                                });
                            }
                        }, (int) (1.75 * UI_CREATION_DELAY));
                    } else {
                        //DEFINE IF DEVICE SUPPORT OTA & MODE (NORMAL/DFU)

                        final boolean ota_service_check = btGatt.getService(ota_service) != null;
                        if (ota_service_check) {
                            //Log.d("ota_service_check", "" + ota_service_check);
                            final boolean ota_data_check = btGatt.getService(ota_service).getCharacteristic(ota_data) != null;
                            if (ota_data_check) {
                                //Log.d("ota_data_check", "" + ota_data_check);
                                final boolean homekit_check = btGatt.getService(homekit_service) != null;
                                if (!homekit_check) {
                                    ota_mode = true;
                                    int ota_data_property = btGatt.getService(ota_service).getCharacteristic(ota_data).getProperties();
                                    if (ota_data_property == 12 || ota_data_property == 8 || ota_data_property == 10) {
                                        //reliable = true;
                                    } else if (ota_mode && ota_data_property == 4) {
                                        //reliable = false;
                                    }
                                }
                            } else {
                                if (boolOTAbegin)onceAgain();
                            }
                        } //else Log.d("ota_service_check", "" + ota_service_check);



                        //REQUEST MTU
                        if (UICreated && loadingdialog.isShowing()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) bluetoothGatt.requestMtu(MTU);
                        }

                        //LAUNCH SERVICES UI
                        if (!boolFullOTA) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            onGattFetched();
                                            hideCharacteristicLoadingAnimation();
                                        }
                                    });
                                }
                            }, (int) (1.75 * UI_CREATION_DELAY));
                        }

                        //IF DFU_MODE, LAUNCH OTA SETUP AUTOMATICALLY
                        if (ota_mode && boolOTAbegin) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingimage.setVisibility(View.GONE);
                                        loadingdialog.dismiss();
                                        showOtaProgress();
                                    }
                                });
                                }
                            }, (int) (2.5 * UI_CREATION_DELAY));
                        }
                    }
                }
            }
        }
    };
    /************************************************************************************/

    /**BLUETOOTH ADAPTER RESPONSES**************************************************/
    private final BroadcastReceiver bluetoothAdapterStateChangeListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        //Log.d("BTAdapter","STATE_ON");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        //Log.d("BTAdapter","STATE_OFF");
                        break;
                }
            }
        }
    };
    /*************************************************************************************/


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**ACTIVITY STATES MACHINE***********************************************************/

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_services);
        ButterKnife.inject(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        showCharacteristicLoadingAnimation();

        if (savedInstanceState == null) {
            LogFragmentConnected logFragment = new LogFragmentConnected();
            android.support.v4.app.FragmentTransaction logtransaction = getSupportFragmentManager().beginTransaction();
            logtransaction.add(R.id.log_body_connected, logFragment);
            logtransaction.commit();
        }

        handler = new Handler();
        bluetoothBinding = new BlueToothService.Binding(this) {
            @Override
            protected void onBound(BlueToothService service) {
                serviceHasBeenSet = true;
                DeviceServicesActivity.this.service = service;

                if (!service.isGattConnected()) {
                    Toast.makeText(DeviceServicesActivity.this, R.string.toast_debug_connection_failed, Toast.LENGTH_LONG).show();
                    disconnectGatt(bluetoothGatt);
                } else {
                    service.getConnectedGatt().discoverServices();
                    service.registerGattCallback(true, gattCallback);
                }
            }
        };
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BlueToothService.bind(bluetoothBinding);
                    }
                });
            }
        }, UI_CREATION_DELAY);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((serviceHasBeenSet && service == null) || (service != null && !service.isGattConnected())) {
            Toast.makeText(DeviceServicesActivity.this, R.string.toast_debug_connection_failed, Toast.LENGTH_LONG).show();
            if (bluetoothGatt != null) if (service != null) {
                service.clearGatt();
            }
            bluetoothBinding.unbind();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bluetoothAdapterStateChangeListener);
        } catch (Exception b) {
            Log.e("onDestroy", "unregisterReceiver Err" + b);
        }

        if (service != null) {
            //Log.d("onDestroy","called");
            service.clearGatt();
        }

        try {
            bluetoothBinding.unbind();
        } catch (Exception e) {
            Log.e("onDestroy", "bluetoothBinding Err" + e);
        }

        if (bluetoothGatt != null) bluetoothGatt = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_services_bluegiga, menu);
        ota_button = menu.findItem(R.id.OTA_button);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_license:
                showAboutDialog();
                break;
            case R.id.bluetooth_modules_website:
                Intent intentBlueGigaWebsite = new Intent(Intent.ACTION_VIEW);
                intentBlueGigaWebsite.setData(Uri.parse(Common.BLUEGIGA_URL_SILICON_LABS_OCT_2015));
                startActivity(intentBlueGigaWebsite);
                break;
            case R.id.menu_log: //LOG MENU BUTTON
                adjustLayout();
                break;
            case R.id.OTA_button: //OTA MENU BUTTON
                if (Build.VERSION.SDK_INT >= 21) {
                    if (UICreated) OTAonClick();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext() ,"OTA is available only for Android 5 or higher",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case R.id.refresh_services: //REFRESH SERVICES MENU BUTTON
                boolrefresh_services = true;
                refreshServices();
                break;
            case R.id.request_mtu: //REQUEST MTU MENU BUTTON
                if (Build.VERSION.SDK_INT >= 21) {
                    if (UICreated) {
                        boolrequest_mtu = true;
                        showRequestMTU();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext() ,"Request MTU is available only for Android 5 or higher",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case R.id.request_priority://REQUEST PRIORITY MENU BUTTON
                if (Build.VERSION.SDK_INT >= 21) {
                    if (UICreated) {
                        showRequestPriority();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext() ,"Connection Interval is available only for Android 5 or higher",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case android.R.id.home: //BACK MENU BUTTON
                onBackPressed();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (bluetoothGatt != null) {
            disconnectGatt(bluetoothGatt);
        } else finish();
    }

    /*****************************************************************************************/

    /**FUNCTIONS*****************************************************************************/


    public void onceAgain(){
        writeOtaControl((byte) 0x00);
    }
    /**START OTA BUTTON (UI, Bools)*/
    public void OTAonClick() {


        if (ota_mode) {
            ota_process = true;
            boolOTAbegin = false;
        } else {
            ota_process = true;
            boolOTAbegin = true;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingimage.setVisibility(View.GONE);
                loadingdialog.dismiss();
                showOtaSetup();
                ota_button.setVisible(true);
            }
        });
    }
    /**ACTION WHEN MTU MENU BUTTON IS PRESSED************************************************/
    public void MTUonButtonMenu() {
        boolrequest_mtu = false;
        if (newMTU.isShowing()) {
            newMTU.dismiss();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "MTU: " + MTU, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**CLOSES THE MTU DIALOG AND SHOW OTA SETUP DIALOG*****************************************/
    public void reopenOTASetup(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newMTU.dismiss();
                otaSetup.show();
                requestMTU.setProgress(MTU);
            }
        });

    }

    /**SETS ALL THE INFO IN THE OTA PROGRESS DIALOG TO "" OR 0 ********************************/
    public void resetOTAProgress(){
        boolFullOTA = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                datasize.setText("");
                filename.setText("");
                loadingimage.setVisibility(View.GONE);
                loadingdialog.dismiss();
                otaBegin.setChecked(false);
                otaupload.setChecked(false);
                otaEnd.setChecked(false);
                progressBar.setProgress(0);
                datasize.setText("0 %");
                dataRate.setText("");
                OTAStart.setClickable(false);
                OTAStart.setBackgroundColor(getResources().getColor(R.color.dark_grey));
                showOtaProgress();
            }
        });
    }

    /**FUTURE IMPLEMENTATION OF LOG VIEW**************************************************/
    //Not used
    private void adjustLayout() { //TODO Fix animation (Almost ok)
        float scale = getResources().getDisplayMetrics().density;
        RelativeLayout logbody = (RelativeLayout) findViewById(R.id.log_body_connected);
        ViewGroup.MarginLayoutParams svw = (ViewGroup.MarginLayoutParams) scrollViewWrapper.getLayoutParams();
        if (logbody.getVisibility() == View.GONE) {
            Log.i("adjustLayout", "Creating View");
            running = true;
            svw.setMargins(0, 0, 0, (int) ((scale * 300) + 0.5f));
            scrollViewWrapper.setLayoutParams(svw);
            logbody.setVisibility(View.VISIBLE);
            startlog();
        } else {
            Log.i("adjustLayout", "Hiding View");
            running = false;
            svw.setMargins(0, 0, 0, 0);
            scrollViewWrapper.setLayoutParams(svw);
            logbody.setVisibility(View.GONE);
        }
    }
    //Not used
    private void startlog() {
        if (logupdate == null) {
            logupdate = new Thread(new Runnable() {
                public void run() {
                    synchronized (this) {
                        try {
                            while (running) {
                                log();
                                Thread.sleep(250);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            });
            logupdate.start();
        }
    }
    //Not used
    private void log() {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(" I ") && !line.contains("ViewRoot") && !line.contains("readRssi()")) { //TODO Log filter options
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
            }
            if (substraction.toString().length() != stringBuilder.toString().length()) {
                String result;
                if (substraction.toString().length() > stringBuilder.toString().length())
                    result = stringBuilder.toString();
                else
                    result = stringBuilder.substring(substraction.length(), stringBuilder.length());
                Message m = new Message();
                Bundle b = new Bundle();
                b.putString("whatisconnected", result);
                m.setData(b);
                logHandler.sendMessage(m);
                substraction = stringBuilder;
            }
        } catch (IOException e) {
            Log.e("log()", "couldn't create log");
        }
    }
    //Not used
    public Handler logHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Log.i("handler","receiving message");
            String sentString = msg.getData().getString("whatisconnected");
            try {
                tv = (TextView) findViewById(R.id.log_view);
                tv.setMovementMethod(new ScrollingMovementMethod());
                tv.append(sentString);
                tv.scrollTo(0, tv.getScrollY());
            } catch (Exception e) {
                Log.e("mHandle", "error: " + e);
            }

        }
    };
    //Not used
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
    //Not used
    public File save_log() {
        final File path = new File(Environment.getExternalStorageDirectory(), "SiliconLabs_BGApp");
        final File file = new File(path + File.separator + "SiliconLabs." + getdate("compact") + ".txt");
        Thread save_log = new Thread(new Runnable() {
            public void run() {
                String savedata = tv.getText().toString();
                if (!path.exists()) path.mkdirs();
                BufferedWriter bw = null;
                try {
                    file.createNewFile();
                    bw = new BufferedWriter(new FileWriter(file), 1024);
                    bw.write(savedata);
                } catch (IOException e) {
                    Log.e("save_log()", "error saving log", e);
                } finally {
                    if (bw != null) {
                        try {
                            bw.close();
                        } catch (IOException e) {
                            Log.e("save_log()", "error closing save_log()", e);
                        }
                    }
                }
            }
        });
        save_log.start();
        return file;
    }
    //Not used
    public void share_log() {
        Thread share_log = new Thread(new Runnable() {
            public void run() {
                String logdata = tv.getText().toString();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SiliconLabs BGApp Log: " + getdate("normal"));
                shareIntent.putExtra(Intent.EXTRA_TEXT, logdata);
                startActivity(Intent.createChooser(shareIntent, "Share SiliconLabs BGApp Log ..."));
            }
        });
        share_log.start();
    }
    //Not used
    public void clear_log() {
        tv = (TextView) findViewById(R.id.log_view);
        tv.setText("");
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-c"});
            Log.i("clear_log()", "log cleaned");
            //restartlog();
        } catch (IOException e) {
            Log.e("alogcat", "error clearing log", e);
        }
    }
    /**************************************************************************************/


    /**USED TO CLEAN CACHE AND REDISCOVER SERVICES****************************************/
    private void refreshServices(){
        if (bluetoothGatt != null && bluetoothGatt.getDevice()!=null) {
            refreshDeviceCache(bluetoothGatt);
            bluetoothGatt.discoverServices();
        } else if (service!=null && service.getConnectedGatt()!=null){
            refreshDeviceCache(service.getConnectedGatt());
            service.getConnectedGatt().discoverServices();
        }
    }

    /**INITIATES SERVICES VIEWS************************************************************/
    private void initServicesViews() {
        serviceItemContainers = new HashMap<>();
        // iterate through all of the services for the device, inflate and add views to the scrollview
        ArrayList<BluetoothGattService> services = (ArrayList<BluetoothGattService>) bluetoothGatt.getServices(); //service.getConnectedGatt().getServices();
        for (int position = 0; position < services.size(); position++) {
            final ServiceItemContainer serviceItemContainer = new ServiceItemContainer(DeviceServicesActivity.this);

            // get information about service at index 'position'
            UUID uuid = services.get(position).getUuid();
            Service service = Engine.getInstance().getService(uuid);
            String serviceName = service != null ? service.getName().trim() : getString(R.string.unknown_service);
            String serviceUuid = service != null ? Common.getUuidText(uuid) : uuid.toString();

            if (serviceUuid.equals(ota_service.toString())) serviceName = "OTA Service";

            // initialize information about services in service item container
            initServiceItemContainer(serviceItemContainer, position, serviceName, serviceUuid);

            // initialize views for each characteristic of the service, put into characteristics expansion for service's list item
            final BluetoothGattService blueToothGattService = service == null ? services.get(position) : bluetoothGatt.getService(service.getUuid());
            List<BluetoothGattCharacteristic> characteristics = blueToothGattService.getCharacteristics();
            if (characteristics.size() == 0) {
                serviceItemContainer.serviceInfoCardView.setBackgroundColor(Color.LTGRAY);
                serviceItemContainer.serviceExpansionCaret.setVisibility(View.INVISIBLE);
                continue;
            }
            // iterate through the characteristics of this service
            for (final BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristics) {
                // retrieve relevant bluetooth data for characteristic of service
                final BluetoothGattCharacteristic thisCharacteristic = bluetoothGattCharacteristic;
                // the engine parses through the data of the btgattcharac and returns a wrapper characteristic
                // the wrapper characteristic is matched with accepted bt gatt profiles, provides field types/values/units
                Characteristic charact = Engine.getInstance().getCharacteristic(bluetoothGattCharacteristic.getUuid());
                String characteristicName = charact != null ? charact.getName().trim() : getString(R.string.unknown_characteristic_label);
                String characteristicUuid = (charact != null ? Common.getUuidText(charact.getUuid()) : bluetoothGattCharacteristic.getUuid().toString());

                //TODO: They are in GattCharacteristic, but their names are not appearing
                if (characteristicUuid.equals(ota_control.toString())) characteristicName = "OTA Control";
                if (characteristicUuid.equals(ota_data.toString())) characteristicName = "OTA Data";
                if (characteristicUuid.equals(fw_version.toString())) characteristicName = "FW Version";
                if (characteristicUuid.equals(ota_version.toString())) characteristicName = "OTA Version";

                // inflate/create ui elements
                LayoutInflater layoutInflater = LayoutInflater.from(this);
                final LinearLayout characteristicContainer = (LinearLayout) layoutInflater.inflate(R.layout.list_item_debug_mode_characteristic_of_service, null);
                final ImageView characteristicExpansionCaret = (ImageView) characteristicContainer.findViewById(R.id.characteristic_expansion_caret);
                final LinearLayout characteristicExpansion = (LinearLayout) characteristicContainer.findViewById(R.id.characteristic_expansion);
                final LinearLayout propsContainer = (LinearLayout) characteristicContainer.findViewById(R.id.characteristic_props_container);
                TextView characteristicNameTextView = (TextView) characteristicContainer.findViewById(R.id.characteristic_title);
                TextView characteristicUuidTextView = (TextView) characteristicContainer.findViewById(R.id.characteristic_uuid);
                final Button characteristicDetailsBtn = (Button) characteristicContainer.findViewById(R.id.btn_expand_to_show_characteristic_details);
                View characteristicSeparator = characteristicContainer.findViewById(R.id.characteristics_separator);
                characteristicExpansion.setId(generateNextId());

                // init/populate ui elements with info from bluetooth data for characteristic of service
                characteristicNameTextView.setText(characteristicName);
                characteristicUuidTextView.setText(characteristicUuid);

                // hide divider between characteristics if last characteristic of service
                if (serviceItemContainer.groupOfCharacteristicsForService.getChildCount() == characteristics.size() - 1) {
                    characteristicSeparator.setVisibility(View.GONE);
                    serviceItemContainer.lastItemDivider.setVisibility(View.VISIBLE);
                }
                serviceItemContainer.groupOfCharacteristicsForService.addView(characteristicContainer);

                // add properties to characteristic list item in expansion
                addPropertiesToCharacteristic(bluetoothGattCharacteristic, propsContainer);

                serviceItemContainer.setCharacteristicNotificationState(characteristicUuid, Notifications.DISABLED);

                final String finalServiceName = serviceName;
                final String finalCharacteristicName = characteristicName;

                characteristicDetailsBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnCaretPressed = characteristicDetailsBtn;

                        // check if characteristic of currently pressed caret is the same characteristic as previously selected caret
                        if (previousFragmentId == characteristicExpansion.getId()) {
                            unsetPropertyClickListeners(propsContainer);
                            if (characteristicExpansion.getVisibility() == View.VISIBLE) {
                                FragmentManager fragmentManager = getFragmentManager();
                                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                                // animate caret collapse
                                animateCaretCollapse(DeviceServicesActivity.this, characteristicExpansionCaret);

                                // animate characteristic collapse
                                fragmentTransaction.remove(previousCharacteristicFragment);
                                fragmentTransaction.commit();
                                previousFragmentContainer.setVisibility(View.GONE);

                                previousCharacteristicFragment = null;
                                previousFragmentContainer = null;
                                previousFragmentId = 0;
                                previousCharacteristicContainer = null;
                            }
                            return;
                        }

                        // the currently selected characteristic is not the previously selected characteristic
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction;

                        if (previousFragmentContainer != null) {
                            // remove characteristic view/edit fragment of previously selected characteristic
                            fragmentTransaction = fragmentManager.beginTransaction();
                            fragmentTransaction.remove(previousCharacteristicFragment);
                            fragmentTransaction.commit();
                            previousFragmentContainer.setVisibility(View.GONE);
                        }

                        // If expanding a new characteristic, unset the click listeners for the buttons on the old characteristic
                        if (previousCharacteristicContainer != null) {
                            unsetPropertyClickListeners((LinearLayout) previousCharacteristicContainer.findViewById(R.id.characteristic_props_container));
                        }

                        // init selected characteristic fragment
                        FragmentCharacteristicDetail characteristicDetail = new FragmentCharacteristicDetail();
                        characteristicDetail.address = bluetoothGatt.getDevice().getAddress();
                        characteristicDetail.setmService(blueToothGattService);
                        characteristicDetail.setmBluetoothCharact(thisCharacteristic);

                        // animate carets
                        if (previousCharacteristicCaret != null) {
                            animateCaretCollapse(DeviceServicesActivity.this, previousCharacteristicCaret);
                        }
                        animateCaretExpansion(DeviceServicesActivity.this, characteristicExpansionCaret);

                        // show characteristic's expansion and add the fragment to view/edit characteristic detail
                        characteristicExpansion.setVisibility(View.VISIBLE);
                        fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.add(characteristicExpansion.getId(), characteristicDetail, CHARACTERISTIC_ADD_FRAGMENT_TRANSACTION_ID);
                        fragmentTransaction.commit();

                        previousCharacteristicCaret = characteristicExpansionCaret;

                        previousCharacteristicFragment = characteristicDetail;
                        previousFragmentContainer = characteristicExpansion;
                        previousFragmentId = characteristicExpansion.getId();
                        previousCharacteristicContainer = characteristicContainer;

                        setPropertyClickListeners(propsContainer, bluetoothGattCharacteristic, finalServiceName);
                    }
                });
            }
            servicesContainer.addView(serviceItemContainer, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            serviceItemContainers.put(serviceName, serviceItemContainer);
        }
    }

    // This is used to refresh from FragmentCharacteristicDetail after a write
    // refactor into a callback / a more comprehensive mechanism needed
    Button btnCaretPressed = null;

    public void refreshCharacteristicExpansion() {
        if (btnCaretPressed != null) {
            btnCaretPressed.performClick();
            btnCaretPressed.performClick();
        }
    }

    /**INITIATES SERVICES ITENS************************************************************/
    private void initServiceItemContainer(final ServiceItemContainer serviceItemContainer, int position, String serviceName, String serviceUuid) {

        if (position == 0) {
            UICreated = true;
            serviceItemContainer.serviceHeaderLabel.setVisibility(View.VISIBLE);
            if (bluetoothGatt.getServices().contains(bluetoothGatt.getService(ota_service))) {
                ota_button.setVisible(true);
            } else {
                ota_button.setVisible(false);
            }
        } else {
            serviceItemContainer.serviceHeaderLabel.setVisibility(View.GONE);
        }
        serviceItemContainer.groupOfCharacteristicsForService.setVisibility(View.GONE);
        serviceItemContainer.groupOfCharacteristicsForService.removeAllViews();
        serviceItemContainer.serviceTitleTextView.setText(serviceName);
        serviceItemContainer.serviceUuidTextView.setText(serviceUuid);

    }

    /**SHOW CHARACTERISTIC PROPERTIES IN UI: READ, WRITE************************************/
    private void addPropertiesToCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic,
                                               LinearLayout propsContainer) {
        String propertiesString = Common.getProperties(DeviceServicesActivity.this, bluetoothGattCharacteristic.getProperties());
        String[] propsExploded = propertiesString.split(",");
        for (String propertyValue : propsExploded) {
            TextView propertyView = new TextView(this);

            String propertyValueTrimmed = propertyValue.trim();
            propertyValueTrimmed = propertyValue.length() > 13 ? propertyValue.substring(0, 13) : propertyValueTrimmed;
            propertyValueTrimmed.toUpperCase();
            propertyView.setText(propertyValueTrimmed);
            propertyView.setBackgroundColor(getResources().getColor(R.color.debug_mode_characteristic_expansion_background));
            propertyView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_property_text_size));
            propertyView.setTextColor(getResources().getColor(R.color.debug_mode_characteristic_expansion_property_text_color));
            propertyView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

            LinearLayout propertyContainer = new LinearLayout(DeviceServicesActivity.this);
            propertyContainer.setOrientation(LinearLayout.VERTICAL);

            ImageView propertyIcon = new ImageView(DeviceServicesActivity.this);
            int iconId;
            if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_BROADCAST)) {
                iconId = R.drawable.debug_prop_broadcast;
            } else if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_READ)) {
                iconId = R.drawable.debug_prop_read_disabled;
            } else if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_WRITE_NO_RESPONSE)) {
                iconId = R.drawable.debug_prop_write_no_resp;
            } else if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_WRITE)) {
                iconId = R.drawable.debug_prop_write_disabled;
            } else if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_NOTIFY)) {
                iconId = R.drawable.debug_prop_notify_disabled;
            } else if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_INDICATE)) {
                iconId = R.drawable.debug_prop_indicate_disabled;
            } else if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_SIGNED_WRITE)) {
                iconId = R.drawable.debug_prop_signed_write;
            } else if (propertyValue.trim().toUpperCase().equals(Common.PROPERTY_VALUE_EXTENDED_PROPS)) {
                iconId = R.drawable.debug_prop_ext;
            } else {
                iconId = R.drawable.debug_prop_ext;
            }
            propertyIcon.setBackgroundResource(iconId);
            propertyIcon.setTag(PROPERTY_ICON_TAG);

            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsText.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;

            int propIconEdgeLength = getResources().getDimensionPixelSize(R.dimen.prop_icon_edge_length);
            LinearLayout.LayoutParams paramsIcon = new LinearLayout.LayoutParams(propIconEdgeLength, propIconEdgeLength);
            paramsIcon.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;

            propertyContainer.addView(propertyIcon, paramsIcon);
            propertyContainer.addView(propertyView, paramsText);

            propertyContainer.setTag(propertyValue);

            LinearLayout.LayoutParams paramsTextAndIconContainer = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsTextAndIconContainer.gravity = Gravity.RIGHT;

            LayoutInflater.from(this).inflate(R.layout.empty_space_weighted, propsContainer);

            propsContainer.addView(propertyContainer, paramsTextAndIconContainer);
        }
    }

    private void setPropertyClickListeners(LinearLayout propsContainer, BluetoothGattCharacteristic bluetoothGattCharacteristic, String serviceName) {
        ImageView notificationIcon = getIconWithValue(propsContainer, Common.PROPERTY_VALUE_NOTIFY);
        ImageView indicationIcon = getIconWithValue(propsContainer, Common.PROPERTY_VALUE_INDICATE);
        String characteristicUuid = getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic);
        Notifications notificationState = serviceItemContainers.get(serviceName).getCharacteristicNotificationState(characteristicUuid);
        previousCharacteristicFragment.setNotificationsEnabled(notificationState == Notifications.NOTIFY);
        previousCharacteristicFragment.setIndicationsEnabled(notificationState == Notifications.INDICATE);
        for (int i = 0; i < propsContainer.getChildCount(); i++) {
            if (propsContainer.getChildAt(i).getTag() == null) {
                continue;
            }
            LinearLayout propertyContainer = (LinearLayout) propsContainer.getChildAt(i);
            ImageView propertyIcon = null;
            for (int j = 0; j < propertyContainer.getChildCount(); j++) {
                View view = propertyContainer.getChildAt(j);
                if (view.getTag() != null && view.getTag().equals(PROPERTY_ICON_TAG)) {
                    propertyIcon = (ImageView) view;
                }
            }
            String propertyValueId = ((String) propertyContainer.getTag()).trim().toUpperCase();
            switch (propertyValueId) {
                case Common.PROPERTY_VALUE_READ:
                    propertyContainer.setOnClickListener(getReadPropertyClickListener(bluetoothGattCharacteristic));
                    if (propertyIcon != null) {
                        propertyIcon.setBackgroundResource(R.drawable.debug_prop_read);
                    }
                    break;
                case Common.PROPERTY_VALUE_WRITE:
                    propertyContainer.setOnClickListener(getWritePropertyClickListener());
                    if (propertyIcon != null) {
                        propertyIcon.setBackgroundResource(R.drawable.debug_prop_write);
                    }
                    break;
                case Common.PROPERTY_VALUE_NOTIFY:
                    propertyContainer.setOnClickListener(getNotificationPropertyClickListener(bluetoothGattCharacteristic, propertyIcon, indicationIcon, serviceName));
                    if (propertyIcon != null && notificationState == Notifications.NOTIFY) {
                        propertyIcon.setBackgroundResource(R.drawable.debug_prop_notify);
                    }
                    break;
                case Common.PROPERTY_VALUE_INDICATE:
                    propertyContainer.setOnClickListener(getIndicationPropertyClickListener(bluetoothGattCharacteristic, propertyIcon, notificationIcon, serviceName));
                    if (propertyIcon != null && notificationState == Notifications.INDICATE) {
                        propertyIcon.setBackgroundResource(R.drawable.debug_prop_indicate);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private String getUuidFromBluetoothGattCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        Characteristic characteristic = Engine.getInstance().getCharacteristic(bluetoothGattCharacteristic.getUuid());
        return (characteristic != null ? Common.getUuidText(characteristic.getUuid()) : bluetoothGattCharacteristic.getUuid().toString());
    }

    private ImageView getIconWithValue(LinearLayout propsContainer, String value) {
        for (int i = 0; i < propsContainer.getChildCount(); i++) {
            if (propsContainer.getChildAt(i).getTag() == null) {
                continue;
            }
            LinearLayout propertyContainer = (LinearLayout) propsContainer.getChildAt(i);
            for (int j = 0; j < propertyContainer.getChildCount(); j++) {
                View view = propertyContainer.getChildAt(j);
                if (view.getTag() != null && view.getTag().equals(PROPERTY_ICON_TAG)) {
                    String propertyValue = ((String) propertyContainer.getTag()).trim().toUpperCase();
                    if (propertyValue.equals(value)) {
                        return (ImageView) view;
                    }
                }
            }
        }
        return null;
    }

    private void unsetPropertyClickListeners(LinearLayout propsContainer) {
        for (int i = 0; i < propsContainer.getChildCount(); i++) {
            if (propsContainer.getChildAt(i).getTag() == null) {
                continue;
            }
            LinearLayout propertyContainer = (LinearLayout) propsContainer.getChildAt(i);
            propertyContainer.setOnClickListener(null);
            ImageView propertyIcon = null;
            for (int j = 0; j < propertyContainer.getChildCount(); j++) {
                View view = propertyContainer.getChildAt(j);
                if (view.getTag() != null && view.getTag().equals(PROPERTY_ICON_TAG)) {
                    propertyIcon = (ImageView) view;
                }
            }
            if (propertyIcon == null) {
                return;
            }
            String propertyValueId = ((String) propertyContainer.getTag()).trim().toUpperCase();
            switch (propertyValueId) {
                case Common.PROPERTY_VALUE_READ:
                    propertyIcon.setBackgroundResource(R.drawable.debug_prop_read_disabled);
                    break;
                case Common.PROPERTY_VALUE_WRITE:
                    propertyIcon.setBackgroundResource(R.drawable.debug_prop_write_disabled);
                    break;
                case Common.PROPERTY_VALUE_NOTIFY:
                    propertyIcon.setBackgroundResource(R.drawable.debug_prop_notify_disabled);
                    break;
                case Common.PROPERTY_VALUE_INDICATE:
                    propertyIcon.setBackgroundResource(R.drawable.debug_prop_indicate_disabled);
                    break;
                default:
                    break;
            }
        }
    }

    private View.OnClickListener getReadPropertyClickListener(final BluetoothGattCharacteristic characteristic) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothGatt.readCharacteristic(characteristic);
            }
        };
    }

    private View.OnClickListener getNotificationPropertyClickListener(final BluetoothGattCharacteristic bluetoothGattCharacteristic, final ImageView propertyIcon, final ImageView otherIcon, final String serviceName) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean notificationsEnabled = previousCharacteristicFragment.getNotificationsEnabled();
                boolean submitted = BLEUtils.SetNotificationForCharacteristic(bluetoothGatt, bluetoothGattCharacteristic, notificationsEnabled ? Notifications.DISABLED : Notifications.NOTIFY);
                if (submitted) {
                    notificationsEnabled = !notificationsEnabled;
                }
                previousCharacteristicFragment.setNotificationsEnabled(notificationsEnabled);
                propertyIcon.setBackgroundResource(notificationsEnabled ? R.drawable.debug_prop_notify : R.drawable.debug_prop_notify_disabled);

                String characteristicUuid = getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic);
                serviceItemContainers.get(serviceName).setCharacteristicNotificationState(characteristicUuid, notificationsEnabled ? Notifications.NOTIFY : Notifications.DISABLED);

                previousCharacteristicFragment.setIndicationsEnabled(false);
                if (otherIcon != null) {
                    otherIcon.setBackgroundResource(R.drawable.debug_prop_indicate_disabled);
                }
            }
        };
    }

    private View.OnClickListener getIndicationPropertyClickListener(final BluetoothGattCharacteristic bluetoothGattCharacteristic, final ImageView propertyIcon, final ImageView otherIcon, final String serviceName) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean indicationsEnabled = previousCharacteristicFragment.getIndicationsEnabled();
                boolean submitted = BLEUtils.SetNotificationForCharacteristic(bluetoothGatt, bluetoothGattCharacteristic, indicationsEnabled ? Notifications.DISABLED : Notifications.INDICATE);
                if (submitted) {
                    indicationsEnabled = !indicationsEnabled;
                }
                previousCharacteristicFragment.setIndicationsEnabled(indicationsEnabled);
                propertyIcon.setBackgroundResource(indicationsEnabled ? R.drawable.debug_prop_indicate : R.drawable.debug_prop_indicate_disabled);

                String characteristicUuid = getUuidFromBluetoothGattCharacteristic(bluetoothGattCharacteristic);
                serviceItemContainers.get(serviceName).setCharacteristicNotificationState(characteristicUuid, indicationsEnabled ? Notifications.INDICATE : Notifications.DISABLED);

                previousCharacteristicFragment.setNotificationsEnabled(false);
                if (otherIcon != null) {
                    otherIcon.setBackgroundResource(R.drawable.debug_prop_notify_disabled);
                }
            }
        };
    }

    private View.OnClickListener getWritePropertyClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previousCharacteristicFragment.showCharacteristicWriteDialog();
            }
        };
    }

    /**INITIALIZES ABOUT DIALOG*******************************************************/
    private void initAboutDialog() {
        dialogLicense = new Dialog(this);
        dialogLicense.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogLicense.setContentView(R.layout.dialog_about_silicon_labs_blue_gecko);
        WebView webView = (WebView) dialogLicense.findViewById(R.id.menu_item_license);
        Button closeButton = (Button) dialogLicense.findViewById(R.id.close_about_btn);
        webView.loadUrl(ABOUT_DIALOG_HTML_ASSET_FILE_PATH);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogLicense.dismiss();
            }
        });
    }

    /**INITIALIZES OTA PROGRESS DIALOG*******************************************************/
    private void initOtaProgress() {
        otaProgress = new Dialog(this);
        otaProgress.requestWindowFeature(Window.FEATURE_NO_TITLE);
        otaProgress.setContentView(R.layout.ota_progress);
        TextView address = (TextView) otaProgress.findViewById(R.id.device_address);
        address.setText(bluetoothGatt.getDevice().getAddress());
        otaBegin = (CheckBox) otaProgress.findViewById(R.id.otabegin);
        otaBegin.setClickable(false);
        otaupload = (CheckBox) otaProgress.findViewById(R.id.otaupload);
        otaupload.setClickable(false);
        otaEnd = (CheckBox) otaProgress.findViewById(R.id.otaend);
        otaEnd.setClickable(false);
        progressBar = (ProgressBar) otaProgress.findViewById(R.id.otaprogress);
        dataRate = (TextView) otaProgress.findViewById(R.id.datarate);
        datasize = (TextView) otaProgress.findViewById(R.id.datasize);
        filename = (TextView) otaProgress.findViewById(R.id.filename);
        chrono = (Chronometer) otaProgress.findViewById(R.id.chrono);
        OTAStart = (Button) otaProgress.findViewById(R.id.otabutton);
        sizename = (TextView) otaProgress.findViewById(R.id.sizename);
        mtuname = (TextView) otaProgress.findViewById(R.id.mtuname);
        uploadimage = (ProgressBar) otaProgress.findViewById(R.id.connecting_spinner);
        OTAStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                otaProgress.dismiss();
                DFUMode("DISCONNECTION");
            }
        });
    }

    /**INITIALIZES OTA SETUP DIALOG*******************************************************/
    private void initOtaSetup() {
        otaSetup = new Dialog(this);
        otaSetup.requestWindowFeature(Window.FEATURE_NO_TITLE);
        otaSetup.setContentView(R.layout.ota_config);
        partialOTA = (Button) otaSetup.findViewById(R.id.radio_ota);
        TextView address = (TextView) otaSetup.findViewById(R.id.device_address);
        address.setText(bluetoothGatt.getDevice().getAddress());
        fullOTA = (Button) otaSetup.findViewById(R.id.radio_ota_full);
        folderSpinner = (Spinner) otaSetup.findViewById(R.id.folderspinner);
        appSpinner = (Spinner) otaSetup.findViewById(R.id.appspinner);
        final LinearLayout stacklayout = (LinearLayout) otaSetup.findViewById(R.id.stacklayout);
        stackSpinner = (Spinner) otaSetup.findViewById(R.id.stackspinner);
        OTA_OK = (Button) otaSetup.findViewById(R.id.ota_proceed);
        Button OTA_CANCEL = (Button) otaSetup.findViewById(R.id.ota_cancel);
        reliableWrite = (CheckBox) otaSetup.findViewById(R.id.check_reliable);
        delaySeekBar = (SeekBar) otaSetup.findViewById(R.id.delay_seekBar);
        delayText = (TextView) otaSetup.findViewById(R.id.delay_text);
        delayText.setVisibility(View.INVISIBLE);
        delaySeekBar.setVisibility(View.GONE);
        requestMTU = (SeekBar) otaSetup.findViewById(R.id.mtu_seekBar);


        final EditText mtu_value = (EditText) otaSetup.findViewById(R.id.mtu_value);

        mtu_value.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (mtu_value.getText() != null) {
                    int test = Integer.valueOf(mtu_value.getText().toString());
                    if (test < 23) test = 23;
                    else if (test > 512) test = 512;
                    requestMTU.setProgress(test-23);
                    MTU = test;
                }
                return false;

            }
        });


        requestMTU.setMax(512-23);
        requestMTU.setProgress(512-23);
        requestMTU.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mtu_value.setText(""+ (progress+23));
                MTU = progress+23;
            }
        });

        SeekBar requestPriority = (SeekBar) otaSetup.findViewById(R.id.connection_seekBar);
        requestPriority.setMax(2);
        requestPriority.setProgress(2);
        priority=1;
        requestPriority.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("onProgressChanged","" + progress);
                if (progress==1) priority=0;//BALANCE
                else if (progress==2) priority=1;//HIGH
                else if (progress==0) priority=2;//LOW

            }
        });

        delaySeekBar.setMax(100);
        delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                delayText.setText("" + progress + " ms");
                delayNoResponse = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        OTA_CANCEL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                otaSetup.dismiss();
            }
        });
        OTA_OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OTA_OK.setClickable(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        otaSetup.dismiss();
                        if (ota_mode) {
                            bluetoothGatt.requestMtu(Integer.valueOf(mtu_value.getText().toString()));
                        }
                        else DFUMode("OTABEGIN");
                    }
                });
            }
        });
        fullOTA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stacklayout.setVisibility(View.VISIBLE);
                partialOTA.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson));
                fullOTA.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson_darker));
            }
        });
        partialOTA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stacklayout.setVisibility(View.GONE);
                partialOTA.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson_darker));
                fullOTA.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson));
            }
        });

        reliableWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!reliableWrite.isChecked()){
                    reliable = false;
                    delayText.setVisibility(View.VISIBLE);
                    delaySeekBar.setVisibility(View.VISIBLE);
                } else {
                    delayText.setVisibility(View.INVISIBLE);
                    delaySeekBar.setVisibility(View.GONE);
                    reliable = true;
                }
            }
        });



    }

    /**INITIALIZES MTU DIALOG*******************************************************/
    private void initNewMTU(){
        newMTU = new Dialog(this);
        newMTU.requestWindowFeature(Window.FEATURE_NO_TITLE);
        newMTU.setContentView(R.layout.newmtu);
        final EditText mtuvalue = (EditText) newMTU.findViewById(R.id.mtuvalue);
        Button back = (Button) newMTU.findViewById(R.id.request);
        Button cancelrequest = (Button) newMTU.findViewById(R.id.cancel_request);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mtuvalue.getText().toString().equals("")) {
                    int possibleMTU = Integer.parseInt(mtuvalue.getText().toString());
                    if (possibleMTU > 513) possibleMTU = 512;
                    if (possibleMTU < 23) possibleMTU = 23;
                    Log.i("MTU", "Trying new value: " + possibleMTU);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        bluetoothGatt.requestMtu(possibleMTU);
                    }

                    newMTU.dismiss();
                    if (!boolrequest_mtu) otaSetup.show();
                } else {
                    newMTU.dismiss();
                }
            }
        });
        cancelrequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newMTU.dismiss();
                if (!boolrequest_mtu) otaSetup.show();
            }
        });


    }

    /**INITIALIZES CONNECTION INTERVAL DIALOG*******************************************************/
    private void initNewPriority(){
        newPriority = new Dialog(this);
        newPriority.requestWindowFeature(Window.FEATURE_NO_TITLE);
        newPriority.setContentView(R.layout.newpriority);
        Button request = (Button) newPriority.findViewById(R.id.request);
        Button cancelrequest = (Button) newPriority.findViewById(R.id.cancel_request);
        final CheckBox lowPriority = (CheckBox) newPriority.findViewById(R.id.low_priority);
        final CheckBox balancedPriority = (CheckBox) newPriority.findViewById(R.id.balanced_priority);
        final CheckBox highPriority = (CheckBox) newPriority.findViewById(R.id.high_priority);
        lowPriority.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lowPriority.isChecked()) {
                    if (highPriority.isChecked()) highPriority.setChecked(false);
                    if (balancedPriority.isChecked()) balancedPriority.setChecked(false);
                }
            }
        });
        balancedPriority.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (balancedPriority.isChecked()) {
                    if (lowPriority.isChecked()) lowPriority.setChecked(false);
                    if (highPriority.isChecked()) highPriority.setChecked(false);
                }
            }
        });
        highPriority.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (highPriority.isChecked()) {
                    if (lowPriority.isChecked()) lowPriority.setChecked(false);
                    if (balancedPriority.isChecked()) balancedPriority.setChecked(false);
                }
            }
        });
        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (highPriority.isChecked() || balancedPriority.isChecked() || lowPriority.isChecked()){
                    if (highPriority.isChecked()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                            Toast.makeText(getApplicationContext(),"CONNECTION PRIORITY: HIGH",Toast.LENGTH_SHORT).show();
                        } else Toast.makeText(getApplicationContext(),"NOT AVAILABLE FOR THIS ANDROID VERSION",Toast.LENGTH_SHORT).show();
                    } else if (balancedPriority.isChecked()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
                            Toast.makeText(getApplicationContext(), "CONNECTION PRIORITY: BALANCED", Toast.LENGTH_SHORT).show();
                        } else Toast.makeText(getApplicationContext(),"NOT AVAILABLE FOR THIS ANDROID VERSION",Toast.LENGTH_SHORT).show();
                    }else if (lowPriority.isChecked()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
                                Toast.makeText(getApplicationContext(), "CONNECTION PRIORITY: LOW POWER", Toast.LENGTH_SHORT).show();
                            } else Toast.makeText(getApplicationContext(),"NOT AVAILABLE FOR THIS ANDROID VERSION",Toast.LENGTH_SHORT).show();
                        }
                }
                newPriority.dismiss();
            }
        });
        cancelrequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPriority.dismiss();
            }
        });
    }

    /**INITIALIZES LOADING DIALOG*******************************************************/
    private void initLoading() {
        loadingdialog = new Dialog(this);
        loadingdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingdialog.setContentView(R.layout.loadingdialog);
        loadingimage = (ProgressBar) loadingdialog.findViewById(R.id.connecting_spinner);
        loadingLog = (TextView) loadingdialog.findViewById(R.id.loadingLog);
        loadingHeader = (TextView) loadingdialog.findViewById(R.id.loading_header);
    }

    /**SHOWS OTA PROGRESS DIALOG IN UI*******************************************************/
    private void showOtaProgress() {
        otaProgress.show();
        OTAStart.setClickable(false);
        otaProgress.setCanceledOnTouchOutside(false);
        DFUMode("OTABEGIN"); //OTAProgress
    }

    /**SHOWS OTA SETUP DIALOG IN UI*******************************************************/
    private void showOtaSetup() {
        if (otaSetup!=null && !otaSetup.isShowing()) {
            otaSetup.show();
            otaSetup.setCanceledOnTouchOutside(false);
            OTA_OK.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            OTA_OK.setClickable(false);
            if (reliable){
                reliableWrite.setChecked(true);
            } else {
                delaySeekBar.setVisibility(View.VISIBLE);
                delayText.setVisibility(View.VISIBLE);
            }



            foldersPath();
        }
    }

    /**SHOWS OTA SETUP DIALOG IN UI*******************************************************/
    private void showLoading(){
        if (loadingdialog!=null) {
            loadingdialog.show();
            loadingdialog.setCanceledOnTouchOutside(false);
            animaloading();
        }
    }

    /**SHOWS OTA REQUEST MTU DIALOG IN UI*******************************************************/
    private void showRequestMTU(){
        if (newMTU!=null) {
            newMTU.show();
            newMTU.setCanceledOnTouchOutside(false);
        }
    }

    /**SHOWS OTA CONNECTION INTERVAL DIALOG IN UI*******************************************************/
    private void showRequestPriority(){
        if (newPriority!=null){
            newPriority.show();
            newPriority.setCanceledOnTouchOutside(false);
        }
    }

    /**SHOWS OTA ABOUT DIALOG IN UI*******************************************************/
    private void showAboutDialog() {
        dialogLicense.show();
    }

    public int generateNextId() {
        generatedId += 1;
        return generatedId;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**INITILIAZES ALL NECESSARY DIALOGS AND VIEW IN UI - ONCREATE***********************/
    private void onGattFetched() {
        String deviceName = bluetoothGatt.getDevice().getName();
        deviceName = TextUtils.isEmpty(deviceName) ? getString(R.string.unknown) : deviceName;
        getSupportActionBar().setTitle(deviceName);
        servicesContainer.removeAllViews();
        initServicesViews();
        initAboutDialog();
        if (!boolOTAbegin) {
            initOtaSetup();
            initOtaProgress();
            initLoading();
            initNewMTU();
            initNewPriority();
        }
    }

    /**READ ALL THE SERVICES, PRINT IT ON LOG AND RECOGNIZES HOMEKIT ACCESSORIES*****************/
    public void getServicesInfo(BluetoothGatt gatt) {

        List<BluetoothGattService> gattServices = gatt.getServices();
        Log.i("onServicesDiscovered", "Services count: " + gattServices.size());

        for (BluetoothGattService gattService : gattServices) {
            String serviceUUID = gattService.getUuid().toString();
            Log.i("onServicesDiscovered", "Service UUID " + serviceUUID + " - Char count: " + gattService.getCharacteristics().size());
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                String CharacteristicUUID = gattCharacteristic.getUuid().toString();
                Log.i("onServicesDiscovered", "Characteristic UUID " + CharacteristicUUID + " - Properties: " + gattCharacteristic.getProperties());

                if (gattCharacteristic.getUuid().toString().equals(ota_control.toString())){
                    if (gattCharacteristics.contains(bluetoothGatt.getService(ota_service).getCharacteristic(ota_data))) {
                        if(!gattServices.contains(bluetoothGatt.getService(homekit_service))) {
                            Log.i("onServicesDiscovered", "Device in DFU Mode");
                        } else {
                            Log.i("onServicesDiscovered", "OTA_Control found");
                            List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();

                            for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
                                String descriptor = gattDescriptor.getUuid().toString();

                                if (gattDescriptor.getUuid().toString().equals(homekit_descriptor.toString())) {
                                    kit_descriptor = gattDescriptor;
                                    Log.i("descriptor", "UUID: " + descriptor);
                                    //bluetoothGatt.readDescriptor(gattDescriptor);
                                    byte[] stable = {(byte) 0x00, (byte) 0x00};
                                    homeKitOTAControl(stable);
                                    homekit = true;

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**WRITES OTA CONTROL FOR HOMEKIT DEVICES*****************************************/
    public void homeKitOTAControl (byte[] instanceID) {

        //WRITE CHARACTERISTIC FOR HOMEKIT
        byte[] value = {0x00, 0x02, (byte) 0xee, instanceID[0], instanceID[1], 0x03, 0x00, 0x01, 0x01, 0x01};
        writeGenericCharacteristic(ota_service,ota_control, value);
        Log.d("characteristic", "writting: " + Converters.getHexValue(value));

    }

    /**WRITES BYTE TO OTA CONTROL CHARACTERISTIC*****************************************/
    public boolean writeOtaControl(byte ctrl) {
        Log.d("writeOtaControl", "Called");

        if (bluetoothGatt.getService(ota_service)!=null){
            BluetoothGattCharacteristic charac = bluetoothGatt.getService(ota_service).getCharacteristic(ota_control);
            if (charac != null) {
                Log.d("Instance ID", "" + charac.getInstanceId());
                charac.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                Log.d("charac_properties", "" + charac.getProperties());
                byte[] control = new byte[1];
                control[0] = ctrl;
                charac.setValue(control);
                bluetoothGatt.writeCharacteristic(charac);
                return true;
            } else {
                Log.d("characteristic", "null");
            }
        } else {
            Log.d("service", "null");
        }
        return false;
    }

    /**WRITES BYTE ARRAY TO A GENERIC CHARACTERISTIC*****************************************/
    public boolean writeGenericCharacteristic (UUID service, UUID characteristic, byte[] value){

        if (bluetoothGatt != null){

            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(service).getCharacteristic(characteristic);
            Log.d("characteristic", "exists");

            if (bluetoothGattCharacteristic != null) {

                bluetoothGattCharacteristic.setValue(value);
                bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                Log.d("characteristic", "written");

            } else {

                Log.d("characteristic", "null");
                return false;
            }

        } else {

            Log.d("bluetoothGatt", "null");
            return false;

        }

        return true;

    }

    //Not used - White with NO RESPONSE*************************************************/
    public void whiteOtaData(final byte[] datathread) {
        boolOTAdata = true;
        byte[] value = new byte[MTU-3];
        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();
        int j = 0;
        for (int i = 0; i < datathread.length; i++) {
            value[j] = datathread[i];
            j++;
            if (j >= MTU-3 || i >= (datathread.length - 1)) {
                long wait = System.currentTimeMillis();
                final BluetoothGattCharacteristic charac = bluetoothGatt.getService(ota_service).getCharacteristic(ota_data);
                charac.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                final float progress = ((float) (i + 1) / datathread.length) * 100;
                final float bitrate = ((float) ((i + 1) * (8.0)) / (wait - start));
                if (j < MTU - 3) {
                    byte[] end = new byte[j];
                    for (int k = 0; k < j; k++) {
                        end[k] = value[k];
                    }
                    Log.d("Progress", "sent " + (i + 1) + " / " + datathread.length + " - " + String.format("%.1f", progress) + " % - " + String.format("%.2fkbit/s", bitrate) + " - " + Converters.getHexValue(end));
                    charac.setValue(end);
                } else {
                    j = 0;
                    Log.d("Progress", "sent " + (i + 1) + " / " + datathread.length + " - " + String.format("%.1f", progress) + " % - " + String.format("%.2fkbit/s", bitrate) + " - " + Converters.getHexValue(value));
                    charac.setValue(value);
                }

                if(bluetoothGatt.writeCharacteristic(charac)) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String datarate = String.format("%.2fkbit/s", bitrate);
                            dataRate.setText(datarate);
                            //String dataSize = String.format("%.2fkbit/s", (float) datathread.length/1000);
                            datasize.setText(Integer.toString((int) progress) + " %");
                            progressBar.setProgress((int) progress);
                        }
                    });

                    while (System.currentTimeMillis() - wait < delayNoResponse) ;
                } else {
                    do{
                        while (System.currentTimeMillis() - wait < delayNoResponse);
                        wait = System.currentTimeMillis();

                    } while (!bluetoothGatt.writeCharacteristic(charac));
                }
            }
        }
        long end = System.currentTimeMillis();
        float time = (end - start) / 1000L;
        Log.d("OTA Time - ", "" + time + "s");
        boolOTAdata = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                otaupload.setChecked(true);
                chrono.stop();
                uploadimage.clearAnimation();
                uploadimage.setVisibility(View.INVISIBLE);
            }
        });
        DFUMode("OTAEND");
    }

    /**WRITES EBL/GBL FILES TO OTA_DATA CHARACTERISTIC*****************************************/
    public void otaWriteDataReliable(){

        boolOTAdata = true;
        if (pack == 0) {
            /**SET MTU_divisible by 4*/
            int minus = 0;
            do {
                MTU_divisible = MTU - 3 - minus;
                minus++;
            } while (!(MTU_divisible % 4 == 0));
            mtuname.setText(Integer.toString(MTU_divisible) + " bytes");
        }

        byte[] writearray;
        final float pgss;

        if (pack+MTU_divisible > otafile.length-1){
            /**SET last by 4*/
            int plus = 0;
            int last = otafile.length - pack;
            do{
                last = last + plus;
                plus++;
            } while (!(last % 4 == 0));
            writearray = new byte[last];
            int j = 0;
            for (int i = pack; i < pack+last ; i++){
                if (otafile.length-1 < i){
                    writearray[j] = (byte) 0xFF;
                }else writearray[j] = otafile[i];
                j++;

            }
            pgss = ((float) (pack+last)/(otafile.length-1))*100;
            Log.d("characte", "last: " + pack + " / " + (pack+last) + " : " + Converters.getHexValue(writearray));
        } else {
            int j=0;
            writearray = new byte[MTU_divisible];
            for (int i = pack; i < pack+MTU_divisible ; i++){
                writearray[j] = otafile[i];
                j++;
            }
            pgss = ((float) (pack+MTU_divisible)/(otafile.length-1))*100;
            Log.d("characte", "pack: " + pack + " / " + (pack+MTU_divisible) + " : " + Converters.getHexValue(writearray));
        }

        BluetoothGattCharacteristic charac = bluetoothGatt.getService(ota_service).getCharacteristic(ota_data);
        charac.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        charac.setValue(writearray);
        bluetoothGatt.writeCharacteristic(charac);

        final long waiting_time = (System.currentTimeMillis() - otatime);
        final float bitrate = 8 * (float) pack / waiting_time;

        if (pack > 0) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress((int) pgss);
                                String datarate = String.format("%.2fkbit/s", bitrate);
                                dataRate.setText(datarate);
                                datasize.setText((int) pgss + " %");

                            }
                        });
                    }
                });
        } else {
            otatime = System.currentTimeMillis();
        }
    }

    /**(FOLDER SPINNER IN OTA SETUP) FIND FOLDERS IN THE PHONE STORAGE************************************/
    public void foldersPath() {
        OTApath = Environment.getExternalStorageDirectory() + File.separator + "SiliconLabs_BGApp" + File.separator + "OTAFiles" + File.separator;
        final File directory = new File(OTApath);
        if (!directory.exists()) directory.mkdirs();
        File[] directories = directory.listFiles();
        if (directories != null) {
            folder = new String[directories.length + 1];
            folder[0] = "";
            for (int i = 1; i <= directories.length; i++) {
                int last = directories[i - 1].toString().lastIndexOf(File.separator);
                folder[i] = directories[i - 1].toString().substring(last, directories[i - 1].toString().length());
            }
            ArrayAdapter<String> files = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, folder);
            files.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            folderSpinner.setAdapter(files);
            folderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        OTApath = directory.getPath().concat(folder[position] + File.separator);
                        Log.d("OTApath", "" + OTApath);
                        filePath();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    folderSpinner.setSelection(0);
                }
            });
        } else {
            Toast.makeText(getBaseContext(), "No OTA files in the directory", Toast.LENGTH_SHORT).show();
        }
    }

    /**(APP AND STACK SPINNER IN OTA SETUP) FIND FILES IN THE PHONE STORAGE************************************/
    public void filePath() {
        File OTAfile = new File(OTApath);
        File[] OTAFiles = OTAfile.listFiles();
        if (OTAFiles != null) {
            String[] testapp = new String[OTAFiles.length + 1];
            String[] teststack = new String[OTAFiles.length + 1];
            int app = 0;
            int stack = 0;
            for (int i = 0; i < OTAFiles.length; i++){
                int last = OTAFiles[i].toString().lastIndexOf(File.separator);
                String testing= OTAFiles[i].toString().substring(last, OTAFiles[i].toString().length());
                if (testing.toUpperCase().contains(".EBL") || testing.toUpperCase().contains(".GBL")){
                    //if (testing.toUpperCase().contains("APP")){
                        testapp[app]=testing;
                        app++;
                    //}
                    //if (testing.toUpperCase().contains("STACK")){
                        teststack[stack]=testing;
                        stack++;
                    //}
                }
            }
            stringOTAapp = new String[app+1];
            stringOTAapp[0] = "";
            for (int i = 1; i <= app; i++) {
                stringOTAapp[i] = testapp[i-1];
            }
            stringOTAstack = new String[stack+1];
            stringOTAstack[0] = "";
            for (int i = 1; i <= app; i++) {
                stringOTAstack[i] = teststack[i-1];
            }
            ArrayAdapter<String> apps = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, stringOTAapp);
            ArrayAdapter<String> stacks = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, stringOTAstack);
            apps.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stacks.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            appSpinner.setAdapter(apps);
            stackSpinner.setAdapter(stacks);
            appSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        OTA_OK.setClickable(true);
                        OTA_OK.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson));
                        appPath = OTApath.concat(stringOTAapp[position]);
                        Log.d("appPath", "" + appPath);
                    }
                    if (position == 0) {
                        OTA_OK.setClickable(false);
                        OTA_OK.setBackgroundColor(getResources().getColor(R.color.dark_grey));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    appSpinner.setSelection(0);
                }
            });

            stackSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        stackPath = OTApath.concat(stringOTAstack[position]);
                        Log.d("stackPath", "" + stackPath);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    stackSpinner.setSelection(0);
                }
            });
        } else {
            Toast.makeText(getBaseContext(), "No ebl files in this directory", Toast.LENGTH_SHORT).show();
        }
    }

    /**(RUNNABLE) CHECKS OTA BEGIN BOX AND STARTS**********************************/
    private Runnable checkbeginrunnable = new Runnable() {
        @Override
        public void run() {
            chrono.setBase(SystemClock.elapsedRealtime());
            chrono.start();
            otaBegin.setChecked(true);
        }
    };

    /**CREATES BAR PROGRESS ANIMATION IN LOADING AND OTA PROGRESS DIALOG**********************************/
    private void animaloading() {
        if (uploadimage != null && loadingimage != null && otaProgress != null) {
            uploadimage.setVisibility(View.GONE);
            loadingimage.setVisibility(View.GONE);
            if (loadingdialog.isShowing()) {
                loadingimage.setVisibility(View.VISIBLE);
            }
            if (otaProgress.isShowing()) {
                uploadimage.setVisibility(View.VISIBLE);
            }
        }
    }

    /**OTA STATE MACHINE*/
    public void DFUMode(String step) {

        switch (step) {

            case "INIT":
                DFUMode("OTABEGIN");
                break;

            /**WRITES 0x00 TO OTA_CONTROL CHARACTERISTIC*/
            case "OTABEGIN":
                if(ota_mode)
                    //START OTA PROCESS -> gattCallback -> OnCharacteristicWrite
                    Log.d("OTA_BEGIN","" + writeOtaControl((byte) 0x00));
                else {
                    //PUT DEVICE IN DFUMODE -> gattCallback -> OnCharacteristicWrite
                    if (homekit){
                        bluetoothGatt.readDescriptor(kit_descriptor);
                    } else Log.d("DFU_MODE","" + writeOtaControl((byte) 0x00));
                }
                break;

            /**SET THE FILES TO BE UPLOADED TO OTA_DATA CHARACTERISTIC*/
            case "OTAUPLOAD":
                Log.d("OTAUPLOAD", "Called");
                /**Check Services*/

                BluetoothGattService mBluetoothGattService = bluetoothGatt.getService(ota_service);
                if (mBluetoothGattService != null) {
                    BluetoothGattCharacteristic charac = bluetoothGatt.getService(ota_service).getCharacteristic(ota_data);
                    if (charac != null) {
                        charac.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        Log.d("Instance ID", "" + charac.getInstanceId());

                        /**Check Files*/

                        byte[] ebl = null;
                        try {
                            Log.d("stackPath", "" + stackPath);
                            Log.d("appPath", "" + stackPath);
                            File file;
                            if (!stackPath.equals("")) {
                                file = new File(stackPath);
                                boolFullOTA = true;
                            } else {
                                file = new File(appPath);
                                boolFullOTA = false;
                            }
                            FileInputStream fileInputStream = new FileInputStream(file);
                            int size = fileInputStream.available();
                            Log.d("size", "" + size);
                            byte[] temp = new byte[size];
                            fileInputStream.read(temp);
                            fileInputStream.close();
                            ebl = temp;
                        } catch (Exception e) {
                            Log.e("InputStream", "Couldn't open file" + e);
                        }
                        final byte[] datathread = ebl;
                        otafile = ebl;

                        /**Check if it is partial of full OTA*/

                        final String fn;
                        if(!stackPath.equals("")) {
                            int last = stackPath.lastIndexOf(File.separator);
                            fn = stackPath.substring(last, stackPath.length());
                        } else {
                            int last = appPath.lastIndexOf(File.separator);
                            fn = appPath.substring(last, appPath.length());
                        }
                        pack = 0;

                        /**Set info into UI OTA Progress*/
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                filename.setText(fn);
                                sizename.setText(Integer.toString(datathread.length) + " bytes");
                                mtuname.setText(Integer.toString(MTU));
                                uploadimage.setVisibility(View.VISIBLE);
                                animaloading();
                            }
                        });

                        /**Start OTA_data Upload in another thread*/
                        Thread otaUpload = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (reliable){
                                    otaWriteDataReliable();
                                } else whiteOtaData(datathread);
                            }
                        });
                        otaUpload.start();
                    }
                }
                break;

            /**WRITES 0x03 TO OTA_CONTROL CHARACTERISTIC*/
            case "OTAEND":
                Log.d("OTAEND", "Called");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        writeOtaControl((byte) 0x03);
                    }
                },500);
                break;

            /**ENDS THE OTA PROCESS*/
            case "DISCONNECTION":
                ota_process = false;
                boolFullOTA = false;
                boolOTAbegin = false;
                disconnectGatt(bluetoothGatt);
                break;
            default:
                break;
        }

    }

    //Not used - Using reconnect() instead
    private void resetconnection() {
        Timer scanTimer = new Timer();

        if (bluetoothGatt != null) {

            reconnectaddress = bluetoothGatt.getDevice().getAddress();

            bluetoothDevice = bluetoothGatt.getDevice();

            bluetoothGatt.disconnect();
            service.clearCache();
            refreshDeviceCache(bluetoothGatt);


            scanTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bluetoothBinding.unbind();
                    service.clearGatt();
                    bluetoothGatt.close();
                }
            },400);

            scanTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d("fetchUUIDs","" + bluetoothDevice.fetchUuidsWithSdp());
                }
            },500);

            scanTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bluetoothGatt = null;
                    bluetoothDevice = null;
                }
            },600);

        }

        scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
            }
        },1500);

        scanTimer.schedule(new TimerTask() {
              @Override
              public void run() {
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          startScanLeDevice();
                          loadingLog.setText("Scanning...");
                      }
                  });
              }
          },3000);

    }

    //Not used - Reconnect with device after scanner
    private void reconnectGatt(final BluetoothDevice btDevice){
        bluetoothDevice = btDevice;
        stopScan();

        Timer reconnectTimer = new Timer();
        reconnectTimer.schedule(new TimerTask() {
           @Override
           public void run() {
                bluetoothBinding = new BlueToothService.Binding(getApplicationContext()) {
                       @Override
                       protected void onBound(final BlueToothService service) {
                           service.connectGatt(bluetoothDevice, false, gattCallback);
                           bluetoothGatt = service.getConnectedGatt();
                       }
                   };
                   BlueToothService.bind(bluetoothBinding);
               }
           }, delayToConnect);
    }

    //Not Used - resetconnection
    private ScanCallback reScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final BluetoothDevice btDevice = result.getDevice();
            onScanCallback++;
            loadingLog.setText("Waiting to connect");
            reconnectGatt(btDevice);
            onScanCallback=0;
        }
    };
    //Not Used - resetconnection
    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.d("Scanning", "");
            if (device.getAddress().equals(reconnectaddress)) {
                bluetoothGatt = device.connectGatt(getBaseContext(), false, gattCallback);
                Log.d("onLeScan", "Device is found" + device.getAddress());
            }
        }
    };
    //Not Used - resetconnection
    private void startScanLeDevice() {
        ScanFilter macaddress = new ScanFilter.Builder().setDeviceAddress(reconnectaddress).build();
        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(macaddress);

        ScanSettings settings = new ScanSettings.Builder().build();

        if (Build.VERSION.SDK_INT >= 21)bluetoothLeScanner.startScan(filters, settings, reScanCallback);
        else bluetoothAdapter.startLeScan(leScanCallback);

        Log.d("startScanLeDevice", "Scan Started");

    }

    //Not Used - resetconnection
    private void stopScan(){
        if (Build.VERSION.SDK_INT >= 21) bluetoothLeScanner.stopScan(reScanCallback);
        else bluetoothAdapter.stopLeScan(leScanCallback);
        Log.d("stopScan", "Called");
    }


    /**CALLS A METHOD TO CLEAN DEVICE SERVICES********************************************************/
    private boolean refreshDeviceCache(final BluetoothGatt gatt) {
        try {
            Log.d("refreshDevice", "Called");
            Method localMethod = bluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(bluetoothGatt, new Object[0])).booleanValue();
                Log.d("refreshDevice", "bool: " + bool);
                return bool;
            }
        } catch (Exception localException) {
            Log.e("refreshDevice", "An exception occured while refreshing device");
        }
        return false;
    }

    /**DISCONNECT GATT GENTLY AND CLEAN GLOBAL VARIABLES***************************************************/
    public void disconnectGatt(BluetoothGatt gatt) {

        Timer disconnectTimer = new Timer();
        boolFullOTA = false;
        boolOTAbegin=false;
        running = false;
        ota_process=false;
        disconnect_gatt=true;
        UICreated = false;
        if (gatt != null && gatt.getDevice()!=null) {

            if(loadingdialog==null) {
                initLoading();
            }

            final BluetoothGatt btGatt = gatt;
            disconnectTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    /**Getting bluetoothDevice to FetchUUID*/
                    if(btGatt.getDevice()!=null) bluetoothDevice = btGatt.getDevice();
                    /**Disconnect gatt*/
                    btGatt.disconnect();
                    service.clearGatt();
                    Log.d("disconnectGatt", "gatt disconnect");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLoading();
                            loadingLog.setText("Disconnecting...");
                            loadingHeader.setText("GATT Connection");
                        }
                    });
                }
            },200);

            disconnectTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bluetoothDevice.fetchUuidsWithSdp();
                }
            },300);


            disconnectionTimeout = true;

            Runnable timeout = new Runnable(){
                @Override
                public void run() {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (disconnectionTimeout) {
                                finish();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getBaseContext(),"DISCONNECTION PROBLEM", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    },5000);
                }
            }; new Thread(timeout).start();


        } else {
            finish();
        }
    }

    /**CLEANS USER INTERFACE AND FINISH ACTIVITY*********************************************************/
    public void exit(BluetoothGatt gatt){
        gatt.close();
        service.getConnectedGatt().close();
        service.clearCache();
        bluetoothBinding.unbind();
        disconnect_gatt=false;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothGatt = null;
                service = null;
                bluetoothBinding = null;
                if(loadingdialog!=null && loadingdialog.isShowing()) loadingdialog.dismiss();
                if (otaProgress!=null && otaProgress.isShowing()) otaProgress.dismiss();
                if (otaSetup!=null && otaSetup.isShowing()) otaSetup.dismiss();
                finish();
            }
        },1000);
    }

    /**DISCONNECTS AND CONNECTS WITH THE SELECTED DELAY*******************************************************/
    public void reconnect(long delaytoconnect){

        Timer reconnectTimer = new Timer();
        bluetoothDevice = bluetoothGatt.getDevice();

        if (service.isGattConnected()) {
            service.clearGatt();
            service.clearCache();
        }

        bluetoothGatt.disconnect();

       reconnectTimer.schedule(new TimerTask() {
           @Override
           public void run() {
               bluetoothGatt.close();
               bluetoothBinding.unbind();
           }
       },400);


        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingdialog.isShowing()){
                            loadingLog.setText("Attempting connection...");
                        }
                    }
                });
                bluetoothBinding = new BlueToothService.Binding(getApplicationContext()) {
                    @Override
                    protected void onBound(final BlueToothService service) {
                        bluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(),false,gattCallback);
                    }
                };
                BlueToothService.bind(bluetoothBinding);
            }
        }, delaytoconnect);
    }

    /**DEFINES ERRORS IN GATT COMMUNICATION********************************************************************/
    public String charErrorHandling(int status){
        switch (status) {
            case 0x0001:
                return "GATT INVALID HANDLE";
            case 0x0002:
                return "GATT READ NOT PERMIT";
            case 0x0003:
                return "GATT WRITE NOT PERMIT";
            case 0x0004:
                return "GATT INVALID PDU";
            case 0x0005:
                return "GATT INSUF AUTHENTICATION";
            case 0x0006:
                return "GATT REQ NOT SUPPORTED";
            case 0x0007:
                return "GATT INVALID OFFSET";
            case 0x0008:
                return "GATT INSUF AUTHORIZATION";
            case 0x0009:
                return "GATT PREPARE Q FULL";
            case 0x000a:
                return "GATT NOT FOUND";
            case 0x000b:
                return "GATT NOT LONG";
            case 0x000c:
                return "GATT INSUF KEY SIZE";
            case 0x000d:
                return "GATT INVALID ATTR LEN";
            case 0x000e:
                return "GATT ERR UNLIKELY";
            case 0x000f:
                return "GATT INSUF ENCRYPTION";
            case 0x0010:
                return "GATT UNSUPPORT GRP TYPE";
            case 0x0011:
                return "GATT INSUF RESOURCE";
            case 0x0087:
                return "GATT ILLEGAL PARAMETER";
            //TODO Update App Specific Errors
            case 0x0080:
                return "GATT_NO_RESOURCES";
                //return "bg_err_security_image_checksum_error";
            case 0x0081:
                return "GATT_INTERNAL_ERROR";
                //return "bg_err_wrong_state";
            case 0x0082:
                return "GATT_WRONG_STATE";
                //return "bg_err_buffers_full";
            case 0x0083:
                return "GATT_DB_FULL";
                //return "bg_err_command_too_long";
            case 0x0084:
                return "GATT: BUSY";
                //return "0xbg_err_invalid file_format
            case 0x0085:
                //return "bg_err_unspecified";
                return "GATT ERROR";
            case 0x0086:
                return "GATT CMD STARTED";
            case 0x0088:
                return "GATT PENDING";
            case 0x0089:
                return "GATT AUTH FAIL";
            case 0x008a:
                return "GATT MORE";
            case 0x008b:
                return "GATT INVALID CFG";
            case 0x008c:
                return "GATT SERVICE STARTED";
            case 0x008d:
                return "GATT ENCRYPTED NO MITM";
            case 0x008e:
                return "GATT NOT ENCRYPTED";
            case 0x008f:
                return "GATT CONGESTED";
            case 0x00FD:
                return "GATT CCCD CFG ERROR";
            case 0x00FE:
                return "GATT PROCEDURE IN PROGRESS";
            case 0x00FF:
                return "GATT VALUE OUT OF RANGE";
            case 0x0101:
                return "TOO MANY OPEN CONNECTIONS";
            default:
                return "ERROR NOT HANDLED: " + status;
        }
    }

    /**DEFINES ERRORS IN GATT CONNECTION*********************************************************************/
    public String connectionErrorHandling(int status){
        switch(status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            case 0x01:
                return "GATT CONN L2C FAILURE";
            case 0x08:
                return "GATT CONN TIMEOUT";
            case 0x13:
                return "GATT CONN TERMINATE PEER USER";
            case 0x16:
                return "GATT CONN TERMINATE LOCAL HOST";
            case 0x3E:
                return "GATT CONN FAIL ESTABLISH";
            case 0x22:
                return "GATT CONN LMP TIMEOUT";
            case 0x0100:
                return "GATT CONN CANCEL ";
            case 0x0085:
                return "GATT ERROR"; // Device not reachable
            default:
                return "UNKNOWN (" + status + ")";
        }
    }

    /**ANIMATIONS CONTROLLERS******************************************************************************/
    public void showCharacteristicLoadingAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // this onclicklistener prevents services and characteristics from user interaction before ui is loaded
                    }
                });
                Animation loadingGradientAnimation = AnimationUtils.loadAnimation(DeviceServicesActivity.this, R.anim.connection_translate_right);
                loadingContainer.setVisibility(View.VISIBLE);
                loadingGradientContainer.startAnimation(loadingGradientAnimation);
                Animation loadingBarFlyIn = AnimationUtils.loadAnimation(DeviceServicesActivity.this, R.anim.scanning_bar_fly_in);
                loadingBarContainer.startAnimation(loadingBarFlyIn);
            }
        });
    }

    public void hideCharacteristicLoadingAnimation() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingGradientContainer.clearAnimation();
                Animation loadingBarFlyIn = AnimationUtils.loadAnimation(DeviceServicesActivity.this, R.anim.scanning_bar_fly_out);
                loadingBarContainer.startAnimation(loadingBarFlyIn);

                loadingBarFlyIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        loadingContainer.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        });
    }

    public static void animateCaretExpansion(Context context, final ImageView imageView) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.rotate_caret_to_opp_dir);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.setImageResource(R.drawable.debug_collapse);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animation);
    }

    public static void animateCaretCollapse(Context context, final ImageView imageView) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.rotate_caret_to_opp_dir);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imageView.setImageResource(R.drawable.debug_expand);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animation);
    }
    /********************************************************************************************************/

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("DeviceServices Page") // TODO: Define a title for the content shown.
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
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
