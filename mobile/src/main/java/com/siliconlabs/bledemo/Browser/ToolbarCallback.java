package com.siliconlabs.bledemo.Browser;

import com.siliconlabs.bledemo.Utils.FilterDeviceParams;

public interface ToolbarCallback {
    void close();

    void submit(FilterDeviceParams filterDeviceParams, boolean close);

}
