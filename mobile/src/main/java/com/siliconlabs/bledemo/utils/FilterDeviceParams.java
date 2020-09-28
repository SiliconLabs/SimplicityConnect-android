package com.siliconlabs.bledemo.Utils;

import com.siliconlabs.bledemo.BeaconUtils.BleFormat;

import java.util.List;

public class FilterDeviceParams {
    private String filterName;
    private String name;
    private String advertising;
    private int rssiValue;
    private boolean rssiFlag;
    private boolean onlyFavourite;
    private boolean onlyConnectable;
    private List<BleFormat> bleFormats;


    public FilterDeviceParams(String filterName, String name, String advertising, int rssiValue, boolean rssiFlag,
                              List<BleFormat> bleFormats, boolean onlyFavourite, boolean onlyConnectable) {
        this.filterName = filterName;
        this.name = name;
        this.advertising = advertising;
        this.rssiValue = rssiValue;
        this.rssiFlag = rssiFlag;
        this.bleFormats = bleFormats;
        this.onlyFavourite = onlyFavourite;
        this.onlyConnectable = onlyConnectable;
    }

    public boolean isEmptyFilter() {
        return (name == null || name.equals(""))
                && (advertising == null || advertising.equals(""))
                && !rssiFlag
                && !onlyFavourite
                && !onlyConnectable
                && (bleFormats == null || bleFormats.isEmpty());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdvertising() {
        return advertising;
    }

    public void setAdvertising(String advertising) {
        this.advertising = advertising;
    }

    public int getRssiValue() {
        return rssiValue;
    }

    public void setRssiValue(int rssiValue) {
        this.rssiValue = rssiValue;
    }

    public boolean isRssiFlag() {
        return rssiFlag;
    }

    public void setRssiFlag(boolean rssiFlag) {
        this.rssiFlag = rssiFlag;
    }

    public boolean isOnlyFavourite() {
        return onlyFavourite;
    }

    public void setOnlyFavourite(boolean onlyFavourite) {
        this.onlyFavourite = onlyFavourite;
    }

    public List<BleFormat> getBleFormats() {
        return bleFormats;
    }

    public void setBleFormats(List<BleFormat> bleFormats) {
        this.bleFormats = bleFormats;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public boolean isOnlyConnectable() {
        return onlyConnectable;
    }

    public void setOnlyConnectable(boolean onlyConnectable) {
        this.onlyConnectable = onlyConnectable;
    }
}
