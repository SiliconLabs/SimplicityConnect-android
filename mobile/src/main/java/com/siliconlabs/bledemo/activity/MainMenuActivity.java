package com.siliconlabs.bledemo.activity;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.BuildConfig;
import com.siliconlabs.bledemo.dialogs.LocationInfoDialog;
import com.siliconlabs.bledemo.menu.MenuItemType;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.MenuAdapter;
import com.siliconlabs.bledemo.adapters.ViewPagerAdapter;
import com.siliconlabs.bledemo.ble.BlueToothService;
import com.siliconlabs.bledemo.fragment.DemoFragment;
import com.siliconlabs.bledemo.fragment.DevelopFragment;
import com.siliconlabs.bledemo.fragment.SelectDeviceDialog;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static android.view.View.VISIBLE;
import static com.siliconlabs.bledemo.utils.Constants.BOTTOM_NAVI_DEMO;
import static com.siliconlabs.bledemo.utils.Constants.BOTTOM_NAVI_DEVELOP;


public class MainMenuActivity extends BaseActivity implements MenuAdapter.OnMenuItemClickListener {

    private Dialog helpDialog;
    private Dialog hiddenDebugDialog;
    private boolean isBluetoothAdapterEnabled = true;

    private static final int LOCATION_PERMISION_REQUEST_CODE = 200;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 300;

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.bottom_navigation_view)
    BottomNavigationView bottomNavigationView;
    @InjectView(R.id.bluetooth_enable)
    RelativeLayout bluetoothEnableBar;
    @InjectView(R.id.bluetooth_enable_msg)
    TextView bluetoothEnableMsg;
    @InjectView(R.id.bluetooth_enable_btn)
    TextView bluetoothEnableBtn;
    @InjectView(R.id.location_disabled)
    LinearLayout locationDisabledBar;
    @InjectView(R.id.enable_location)
    TextView locationEnableBtn;
    @InjectView(R.id.location_info)
    TextView locationInfoBtn;
    @InjectView(R.id.help_button)
    TextView helpButton;
    @InjectView(R.id.view_pager)
    ViewPager viewPager;

    private final BroadcastReceiver bluetoothAdapterStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        isBluetoothAdapterEnabled = false;
                        showEnableBluetoothAdapterBar();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (!isBluetoothAdapterEnabled) {
                            Toast.makeText(MainMenuActivity.this, R.string.toast_bluetooth_enabled, Toast.LENGTH_SHORT).show();
                        }
                        isBluetoothAdapterEnabled = true;
                        bluetoothEnableBar.setVisibility(View.GONE);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        isBluetoothAdapterEnabled = false;
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        ButterKnife.inject(this);

        askForWriteExternalStoragePermission();

        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        toolbar.setTitle(getString(R.string.title_Develop));
        bottomNavigationView.setSelectedItemId(R.id.navigation_develop);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isBluetoothAdapterEnabled = bluetoothAdapter.isEnabled();
        } else {
            isBluetoothAdapterEnabled = false;
        }

        // handle bluetooth adapter on/off state
        bluetoothEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeEnableBluetoothAdapterToConnecting();
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpDialog.show();
            }
        });

        locationEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(enableLocationIntent);
            }
        });

        locationInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationInfoDialog dialog = new LocationInfoDialog();
                dialog.show(getSupportFragmentManager(), "location_info_dialog");
            }
        });

        initHelpDialog();
        initHiddenDebugDialog();
        initViewPager();
    }

    private void initViewPager() {
        setupViewPager(viewPager);
        viewPager.setCurrentItem(1);
        initViewPagerBehavior(viewPager);
    }

    private void askForWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
    }

    private void initViewPagerBehavior(ViewPager viewPager) {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
                if (position == 0) {
                    toolbar.setTitle(BOTTOM_NAVI_DEMO);
                } else if (position == 1) {
                    toolbar.setTitle(BOTTOM_NAVI_DEVELOP);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new DemoFragment(), getString(R.string.title_Demo));
        viewPagerAdapter.addFragment(new DevelopFragment(), getString(R.string.title_Develop));
        viewPager.setAdapter(viewPagerAdapter);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.navigation_demo:
                    viewPager.setCurrentItem(0);
                    toolbar.setTitle(BOTTOM_NAVI_DEMO);
                    return true;
                case R.id.navigation_develop:
                    viewPager.setCurrentItem(1);
                    toolbar.setTitle(BOTTOM_NAVI_DEVELOP);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isBluetoothAdapterEnabled = bluetoothAdapter.isEnabled();
        } else {
            isBluetoothAdapterEnabled = false;
        }

        if (!isBluetoothAdapterEnabled) {
            showEnableBluetoothAdapterBar();
        }

        if (!isLocationEnabled()) {
            showLocationDisabledBar();
        } else {
            hideLocationDisabledBar();
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothAdapterStateChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // no menu needed here
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        return false;
    }

    private void hideLocationDisabledBar() {
        locationDisabledBar.setVisibility(View.GONE);
    }

    private void showLocationDisabledBar() {
        locationDisabledBar.setVisibility(VISIBLE);
    }

    private void initHelpDialog() {
        helpDialog = new Dialog(MainMenuActivity.this);
        if (helpDialog.getWindow() != null) {
            helpDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        helpDialog.setContentView(R.layout.dialog_help_demo_item);
        ((TextView) helpDialog.findViewById(R.id.dialog_help_version_text)).setText(getString(R.string.version_text,
                BuildConfig.VERSION_NAME));
        View okButton = helpDialog.findViewById(R.id.help_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpDialog.dismiss();
            }
        });

        final TextView silabsProductsWirelessTV = helpDialog.findViewById(R.id.silabs_products_wireless);
        final TextView silabsSupportTV = helpDialog.findViewById(R.id.silabs_support);
        final TextView githubSiliconLabsEfrconnectTV = helpDialog.findViewById(R.id.github_siliconlabs_efrconnect);
        final TextView docsSilabsBluetoothLatestTV = helpDialog.findViewById(R.id.docs_silabs_bluetooth_latest);
        final TextView playStoreSiliconLabsApps = helpDialog.findViewById(R.id.help_text_playstore);

        silabsProductsWirelessTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uriUrl = Uri.parse("https://" + getString(R.string.help_text_moreinfo_url));
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });

        silabsSupportTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uriUrl = Uri.parse("https://" + getString(R.string.help_text_support_url));
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });

        githubSiliconLabsEfrconnectTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uriUrl = Uri.parse("https://" + getString(R.string.help_text_sourcecode_url));
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });

        docsSilabsBluetoothLatestTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uriUrl = Uri.parse("https://" + getString(R.string.help_text_documentation_url));
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });

        playStoreSiliconLabsApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uriUrl = Uri.parse(getString(R.string.help_text_pub_url));
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });


    }

    private void initHiddenDebugDialog() {
        hiddenDebugDialog = new Dialog(MainMenuActivity.this);
        hiddenDebugDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        hiddenDebugDialog.setContentView(R.layout.dialog_hidden_debug_calibration);
        View okButton = hiddenDebugDialog.findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hiddenDebugDialog.dismiss();
            }
        });
    }

    private void showEnableBluetoothAdapterBar() {
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_disabled);
        bluetoothEnableBar.setBackgroundColor(ContextCompat.getColor(MainMenuActivity.this, R.color.silabs_red_dark));
        bluetoothEnableBtn.setVisibility(VISIBLE);
        bluetoothEnableBar.setVisibility(VISIBLE);
        Toast.makeText(MainMenuActivity.this, R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
    }

    private void changeEnableBluetoothAdapterToConnecting() {
        BluetoothAdapter.getDefaultAdapter().enable();
        bluetoothEnableBtn.setVisibility(View.GONE);
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_turning_on);
        bluetoothEnableBar.setBackgroundColor(ContextCompat.getColor(MainMenuActivity.this, R.color.silabs_blue));
    }

    // Detect which menu item was clicked
    @Override
    public void onMenuItemClick(MenuItemType menuItemType) {


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISION_REQUEST_CODE);
        } else if (isBluetoothAdapterEnabled) {

            Intent intent;

            switch (menuItemType) {
                case HEALTH_THERMOMETER:
                    BlueToothService.GattConnectType connectType = BlueToothService.GattConnectType.THERMOMETER;
                    List<Pair<Integer, Integer>> profilesInfo = Arrays.asList(new Pair<>(R.string.htp_title, R.string.htp_id));

                    SelectDeviceDialog selectDeviceDialog = SelectDeviceDialog.newDialog(R.string.title_Health_Thermometer, R.string.description_Thermometer, profilesInfo, connectType);
                    selectDeviceDialog.show(getSupportFragmentManager(), "select_device_tag");
                    break;
                case KEY_FOBS:
                    intent = new Intent(this, KeyFobsActivity.class);
                    startActivity(intent);
                    break;
                case BLUETOOTH_BROWSER:
                    intent = new Intent(this, BrowserActivity.class);
                    startActivity(intent);
                    break;
            }
        } else {
            Toast.makeText(this, R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_PERMISION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainMenuActivity.this, getResources().getString(R.string.Permissions_granted_succesfully), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainMenuActivity.this, R.string.permissions_not_granted, Toast.LENGTH_LONG).show();
                }
                break;
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainMenuActivity.this, getResources().getString(R.string.Permissions_granted_succesfully), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainMenuActivity.this, getResources().getString(R.string.Grant_WRITE_FILES_permission_to_access_OTA), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
