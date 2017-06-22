package com.siliconlabs.bledemo.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.siliconlabs.bledemo.application.SiliconLabsDemoApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a representation of a device that is bound to the BluetoothService.
 * Instances can be passed from Activity to Activity - using Intents - without losing its binding to the BluetoothService.
 */
public class BoundBluetoothDevice implements Parcelable  {
    private static final String EXTRA_RESULT = "_extra_result_";
    private final static ClassLoader CL = BoundBluetoothDevice.class.getClassLoader();
    private static final Map<String,BlueToothService.Binding> sBindingMap = Collections.synchronizedMap(new HashMap<String,BlueToothService.Binding>());

    public static final android.os.Parcelable.Creator<BoundBluetoothDevice> CREATOR = new android.os.Parcelable.Creator<BoundBluetoothDevice>() {
        @Override
        public BoundBluetoothDevice createFromParcel(android.os.Parcel in) {
            return new BoundBluetoothDevice(in);
        }
        @Override
        public BoundBluetoothDevice[] newArray(int size) {
            return new BoundBluetoothDevice[size];
        }
    };

    private BluetoothDevice device;
    private final int refCount;

    private final transient String id;
    private final transient BlueToothService.Binding binding;

    /**
     * Hands over the specified (and bound) device to another activity
     * @param fromActivity The calling activity.
     * @param toActivityClass The other target activity.
     * @param device The (bound) device.
     */
    public static void handover(Activity fromActivity, Class<?> toActivityClass, BluetoothDevice device) {
        Intent intent = new Intent(fromActivity, toActivityClass);
        if (device != null) {
            intent.putExtra(EXTRA_RESULT, new BoundBluetoothDevice(device));
        }
        fromActivity.startActivity(intent);
    }

    /**
     * Hands over (forwards) the specified device to another activity.
     * @param toIntent The intent describing the other target activity.
     * @param device The (bound) device to be forwarded.
     */
    public static void handover(Intent toIntent, BoundBluetoothDevice device) {
        if (device != null) {
            toIntent.putExtra(EXTRA_RESULT, new BoundBluetoothDevice(device));
        }
    }

    /**
     * Receives the (bound) device and binds the given binding to the BluetoothService.
     *
     * If the specified binding is null, the received device remains bound to the BluetoothService. In this case,
     * it is advised to call {@link #unbind()} when done with it (e.g. in the onDestroy of an activity).
     *
     * If the specified binding is not null, the received device will unbind from the BluetoothService after
     * the specified binding binds to the BluetoothService instead.
     *
     * @param receivingActivity The activity that will receive the binding
     * @param binding The binding to be received by the activity
     * @return The bound device.
     */
    public static BoundBluetoothDevice receiveAndBind(Activity receivingActivity, BlueToothService.Binding binding) {
        final BoundBluetoothDevice handover = receivingActivity.getIntent().getParcelableExtra(EXTRA_RESULT);

        if (binding != null) {
            BlueToothService.bind(binding);
            handover.unbind();
        }

        return handover;
    }

    private BoundBluetoothDevice(BluetoothDevice device) {
        this.device = device;
        this.refCount = 1;
        this.id = genId(refCount);
        this.binding = new HandoverBinding();

        if (BlueToothService.bind(binding)) {
            sBindingMap.put(id, binding);
        }
    }

    private BoundBluetoothDevice(BoundBluetoothDevice handover) {
        this.device = handover.device;
        this.refCount = handover.refCount + 1;
        this.id = genId(refCount);
        this.binding = new HandoverBinding();

        if (BlueToothService.bind(binding)) {
            sBindingMap.put(id, binding);
        }
    }

    private BoundBluetoothDevice(Parcel in) {
        this((BluetoothDevice) in.readValue(CL), in.readInt());
    }

    private BoundBluetoothDevice(BluetoothDevice device, int refCount) {
        this.device = device;
        this.refCount = refCount + 1;
        this.id = genId(this.refCount);
        this.binding = new HandoverBinding();

        BlueToothService.Binding oldBinding = sBindingMap.remove(genId(refCount));

        if (BlueToothService.bind(binding)) {
            sBindingMap.put(id, binding);
        }

        if (oldBinding != null) {
            oldBinding.unbind();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(device);
        dest.writeInt(refCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public int getRefCount() {
        return refCount;
    }

    /**
     * Unbinds this device from the BluetoothService.
     *
     * It is not necessary to call this if the method {@link #receiveAndBind(android.app.Activity, BlueToothService.Binding)} was
     * called with a non-null binding.
     */
    public void unbind() {
        unbind(id, binding);
    }

    @Override
    public String toString() {
        return id;
    }

    private String genId(int refCount) {
        return device.getAddress()+"-"+Integer.toString(refCount);
    }

    private static void unbind(String id, BlueToothService.Binding binding) {
        final BlueToothService.Binding latestBinding = sBindingMap.remove(id);
        if (latestBinding == binding) {
            binding.unbind();
        }
    }

    private static class HandoverBinding extends BlueToothService.Binding {
        HandoverBinding() {
            super(SiliconLabsDemoApplication.APP);
        }

        @Override
        protected void onBound(BlueToothService service) {
        }
    }
}
