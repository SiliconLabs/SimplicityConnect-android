package com.siliconlabs.bledemo.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ServiceItemContainerRe extends LinearLayout {

    @InjectView(R.id.service_title)
    public TextView serviceTitleTextView;
    @InjectView(R.id.sevice_uuid)
    public TextView serviceUuidTextView;

    public ServiceItemContainerRe(Context context) {
        super(context);
        init(context);
    }

    public ServiceItemContainerRe(Context context, String title, String text) {
        super(context);
        init(context);
        serviceTitleTextView.setText(title);
        serviceUuidTextView.setText(text);
    }

    public ServiceItemContainerRe(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ServiceItemContainerRe(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.list_item_debug_mode_service_re, this);

        ButterKnife.inject(this);
    }

}
