package com.siliconlabs.bledemo.utils;

public class FilterDeviceParams {
    private String name;
    private boolean filterName;
    private int rssi;
    private boolean filterRssi;

    public FilterDeviceParams(String name, boolean filterName, int rssi, boolean filterRssi) {
        this.name = name;
        this.filterName = filterName;
        this.rssi = rssi;
        this.filterRssi = filterRssi;
    }

    public boolean isEmptyFilter(){
        return !(this.filterName || this.filterRssi);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFilterName() {
        return filterName;
    }

    public void setFilterName(boolean filterName) {
        this.filterName = filterName;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean isFilterRssi() {
        return filterRssi;
    }

    public void setFilterRssi(boolean filterRssi) {
        this.filterRssi = filterRssi;
    }

}
