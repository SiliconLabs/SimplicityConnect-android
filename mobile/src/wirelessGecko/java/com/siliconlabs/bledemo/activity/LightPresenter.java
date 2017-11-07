package com.siliconlabs.bledemo.activity;

import java.util.Timer;
import java.util.TimerTask;

class LightPresenter {

    private static final int FAST_TOGGLING_READ_DELAY = 800;

    public interface View {

        void showLightState(boolean lightOn);

        void showTriggerSource(TriggerSource source);

    }

    public interface BluetoothController {

        boolean setLightValue(boolean lightOn);

        void setPresenter(LightPresenter presenter);

        boolean getLightValue();

    }

    private boolean lightOn;
    private TriggerSource triggerSource = TriggerSource.UNKNOWN;
    private LightPresenter.View view;
    private final BluetoothController bluetoothController;
    private Timer periodicReadTimer;

    LightPresenter(View view, BluetoothController bluetoothController) {
        this.view = view;
        this.bluetoothController = bluetoothController;
        //light_on will be set according to what we read from bluetooth, just hardcoding to start with off for now
        lightOn = false;
        view.showLightState(lightOn);
        view.showTriggerSource(triggerSource);
        bluetoothController.setPresenter(this);
        periodicReadTimer = new Timer();
    }

    private void updateLight() {
        bluetoothController.getLightValue();
    }

    void onLightClicked() {
        bluetoothController.setLightValue(!lightOn);
    }

    void onLightUpdated(boolean isLightOn) {
        lightOn = isLightOn;
        view.showLightState(lightOn);
    }

    void onSourceUpdated(TriggerSource value) {
        triggerSource = value;
        view.showTriggerSource(triggerSource);
    }

    void cancelPeriodicReads() {
        periodicReadTimer.cancel();
    }

    void getLightValueDelayed()
    {
            periodicReadTimer.cancel();
            periodicReadTimer.purge();
            periodicReadTimer = new Timer();
            periodicReadTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateLight();
                }
            },FAST_TOGGLING_READ_DELAY);
    }
}
