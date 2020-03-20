package com.siliconlabs.bledemo.toolbars;

import com.siliconlabs.bledemo.utils.FilterDeviceParams;

public interface ToolbarCallback {
    void close();

    void submit(FilterDeviceParams filterDeviceParams, boolean close);

}
