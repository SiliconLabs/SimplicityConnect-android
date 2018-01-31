package com.siliconlabs.bledemo.interfaces;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

public interface DemoPageLauncher {
    DialogFragment launchPage(View view, FragmentManager fragmentManager);
}
