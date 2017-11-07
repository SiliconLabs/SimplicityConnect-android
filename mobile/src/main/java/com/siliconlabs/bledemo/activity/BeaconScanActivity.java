package com.siliconlabs.bledemo.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.BlueToothService;

import com.siliconlabs.bledemo.fragment.LogFragmentBeacon;
import com.siliconlabs.bledemo.fragment.SearchFragmentBeacon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class BeaconScanActivity extends AppCompatActivity {

    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.bluetooth_enable)
    RelativeLayout bluetoothEnableBar;
    @InjectView(R.id.bluetooth_enable_msg)
    TextView bluetoothEnableMsg;
    @InjectView(R.id.bluetooth_enable_btn)
    TextView bluetoothEnableBtn;
    private BeaconListFragment beaconListFragment;
    private BlueToothService.Binding bluetoothBinding;
    private boolean isBluetoothAdapterEnabled = false;

    //log
    private Thread logupdate;
    volatile boolean running = false;
    StringBuilder substraction = new StringBuilder();
    TextView tv;

    private final BroadcastReceiver bluetoothAdapterStateChangeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        isBluetoothAdapterEnabled = false;
                        showEnableBluetoothAdapterBar();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        isBluetoothAdapterEnabled = false;
                        showEnableBluetoothAdapterBar();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (!isBluetoothAdapterEnabled) {
                            Toast.makeText(BeaconScanActivity.this, R.string.toast_bluetooth_enabled, Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_beacon_scan);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /*if(savedInstanceState==null) {
            SearchFragmentBeacon searchFragmentBeacon = new SearchFragmentBeacon();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.filter_body, searchFragmentBeacon);
            transaction.commit();

            LogFragmentBeacon logFragmentBeacon = new LogFragmentBeacon();
            FragmentTransaction logtransaction = getSupportFragmentManager().beginTransaction();
            logtransaction.add(R.id.log_body, logFragmentBeacon);
            logtransaction.commit();
        }*/

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isBluetoothAdapterEnabled = bluetoothAdapter.isEnabled();
        } else {
            isBluetoothAdapterEnabled = false;
        }

        if (!isBluetoothAdapterEnabled) {
            showEnableBluetoothAdapterBar();
        }

        // handle bluetooth adapter on/off state
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterStateChangeListener, filter);
        bluetoothEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeEnableBluetoothAdapterToConnecting();
            }
        });

        bluetoothBinding = new BlueToothService.Binding(this) {
            @Override
            protected void onBound(final BlueToothService service) {

            }
        };
        BlueToothService.bind(bluetoothBinding);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothBinding.unbind();
        unregisterReceiver(bluetoothAdapterStateChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.menu_beacon_scan, menu);
            return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_filter:
                RelativeLayout filter = (RelativeLayout) findViewById(R.id.filter_body);
                if(filter.getVisibility()==View.GONE) {
                    filter.setVisibility(View.VISIBLE);

                } else {
                    filter.setVisibility(View.GONE);
                }
                return true;
            case R.id.action_log:
                adjustLayout();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void adjustLayout() {

        RelativeLayout logbody = (RelativeLayout) findViewById(R.id.log_body);
        RelativeLayout scanning_LL = (RelativeLayout) findViewById(R.id.scanning_LL);
        if (logbody.getVisibility()== View.GONE) {
            scanning_LL.setVisibility(View.GONE);
            logbody.setVisibility(View.VISIBLE);
            //Log.i("adjustLayout","Creating View");
            running = true;
            startlog();
        } else {
            //Log.i("adjustLayout","Hiding View");
            running = false;
            logbody.setVisibility(View.GONE);
            scanning_LL.setVisibility(View.VISIBLE);
        }
    }

    private void startlog() {
        if(logupdate == null) {
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
        } else logupdate.start();
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
                if (line.contains(" I ") || line.contains(" E ") || line.contains(" D ") ) { //TODO Log filter options
                    if (!line.contains("ViewRoot")) {
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                    }
                }
            }

            if(substraction.toString().length()!=stringBuilder.toString().length()) {
                //Log.i("log()","sending to handler");
                String result;
                if(substraction.toString().length()>stringBuilder.toString().length()) result = stringBuilder.toString();
                else result = stringBuilder.substring(substraction.length(),stringBuilder.length());
                substraction = stringBuilder;
                Message m = new Message();
                Bundle b = new Bundle();
                b.putString("what", result);
                m.setData(b);
                mHandler.sendMessage(m);
            }
        } catch (IOException e) {
            Log.e("log()","couldn't create log: " + e);
        }
    }

    public String getdate(String string) {
        String currentDateTimeString = null;
        switch(string){
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
        final File path = new File(Environment.getExternalStorageDirectory(),"SiliconLabs_BGApp");
        final File file = new File (path + File.separator + "SiliconLabs." + getdate("compact") + ".txt");
        Thread save_log = new Thread(new Runnable() {
            public void run() {
                String savedata = tv.getText().toString();
                if(!path.exists()) path.mkdir();
                BufferedWriter bw = null;
                try{
                    file.createNewFile();
                    bw = new BufferedWriter(new FileWriter(file),1024);
                    bw.write(savedata);
                } catch (IOException e){
                    Log.e("save_log()","error saving log:" + e);
                } finally {
                    if (bw!=null){
                        try {
                            bw.close();
                        } catch (IOException e){
                            Log.e("save_log()","error closing save_log(): " + e);
                        }
                    }
                }
            }
        });
        save_log.start();
        return file;
    }

    public void share_log(){
        logupdate.interrupt();
        Thread share_log = new Thread(new Runnable() {
            public void run() {
                String logdata = tv.getText().toString();
                Intent shareIntent = new Intent (android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SiliconLabs BGApp Log: " + getdate("normal"));
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, logdata);
                startActivity(Intent.createChooser(shareIntent,"Share SiliconLabs BGApp Log ..."));
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
    public void orderbyRSSI() {
        beaconListFragment = new BeaconListFragment();
        List devices = beaconListFragment.getDevicesInfo();
        beaconListFragment.updateWithDevicesRSSI(devices);
    }
    public void showEnableBluetoothAdapterBar() {
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_disabled);
        bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.alizarin_crimson_darker)); //Blue enable BT bar
        bluetoothEnableBtn.setVisibility(View.VISIBLE);
        bluetoothEnableBar.setVisibility(View.VISIBLE);
        Toast.makeText(BeaconScanActivity.this, R.string.toast_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
    }

    public void changeEnableBluetoothAdapterToConnecting() {
        BluetoothAdapter.getDefaultAdapter().enable();
        bluetoothEnableBtn.setVisibility(View.GONE);
        bluetoothEnableMsg.setText(R.string.bluetooth_adapter_bar_turning_on);
        bluetoothEnableBar.setBackgroundColor(getResources().getColor(R.color.cerulean));
    }
}
