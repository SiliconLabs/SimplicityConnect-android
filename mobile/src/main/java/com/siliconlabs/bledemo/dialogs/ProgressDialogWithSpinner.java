package com.siliconlabs.bledemo.dialogs;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ProgressDialogWithSpinner extends android.app.ProgressDialog {
    private int imageViewResourceId;
    private boolean animate;
    private String caption;
    private Animation rotateIconAnimation;
    private Handler handler;

    @InjectView(R.id.progress_spinner)
    ImageView icon;
    @InjectView(R.id.dialog_text)
    TextView captionTextView;

    private final Runnable autoDismiss = new Runnable() {
        @Override
        public void run() {
            if (isShowing()) {
                dismiss();
            }
        }
    };

    public ProgressDialogWithSpinner(Context context, String caption, boolean animate, int imageViewResourceId) {
        super(context);
        init();
        this.caption = caption;
        this.animate = animate;
        this.imageViewResourceId = imageViewResourceId;
    }

    public ProgressDialogWithSpinner(Context context, int theme) {
        super(context, theme);
        init();
    }

    private void init() {
        setCancelable(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_with_progress_spinner);
        ButterKnife.inject(this, this);
        captionTextView.setText(caption);
        if (animate) {
            rotateIconAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_progress_dialog_spinner);
        } else {
            // if not using spinner to animate, set the image resource for the imageview
            icon.setImageResource(imageViewResourceId);
            icon.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    public void show() {
        super.show();

        if (animate) {
            // turn off hardware acceleration, prevents graphic/animation from getting broken
            icon.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            icon.startAnimation(rotateIconAnimation);
        }
    }

    public void show(long timer) {
        show();
        handler.postDelayed(autoDismiss, timer);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        handler.removeCallbacks(autoDismiss);
        clearAnimation();
    }

    public void clearAnimation(){
        icon.clearAnimation();
    }
}