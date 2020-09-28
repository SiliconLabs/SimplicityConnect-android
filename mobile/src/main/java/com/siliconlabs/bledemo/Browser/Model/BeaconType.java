package com.siliconlabs.bledemo.Browser.Model;


import com.siliconlabs.bledemo.BeaconUtils.BleFormat;

public class BeaconType {
    private String beaconTypeName;
    private boolean isChecked;
    private BleFormat bleFormat;


    public BeaconType(String beaconTypeName, boolean isChecked, BleFormat bleFormat) {
        this.beaconTypeName = beaconTypeName;
        this.isChecked = isChecked;
        this.bleFormat = bleFormat;
    }

    public String getBeaconTypeName() {
        return beaconTypeName;
    }

    public void setBeaconTypeName(String beaconTypeName) {
        this.beaconTypeName = beaconTypeName;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public BleFormat getBleFormat() {
        return bleFormat;
    }

    public void setBleFormat(BleFormat bleFormat) {
        this.bleFormat = bleFormat;
    }
}
