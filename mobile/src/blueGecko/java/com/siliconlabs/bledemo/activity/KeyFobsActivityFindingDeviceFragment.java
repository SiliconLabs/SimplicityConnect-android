package com.siliconlabs.bledemo.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.interfaces.FindKeyFobCallback;

import java.util.concurrent.atomic.AtomicInteger;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class KeyFobsActivityFindingDeviceFragment extends Fragment {
    public static final int MIN_FADE_DURATION = 250; //in milliseconds
    private static AtomicInteger fadeDuration = new AtomicInteger(500);

    private Animation fadeIn;
    private Animation fadeOut;

    @InjectView(R.id.led_bulb_rays)
    ImageView ledRays;
    @InjectView(R.id.finding_keyfob_device_name)
    TextView deviceNameText;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((FindKeyFobCallback) getActivity()).triggerDisconnect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_key_fobs_finding_device, container, false);
        ButterKnife.inject(this, view);
        String deviceName = ((FindKeyFobCallback) getActivity()).getDeviceName();
        if (!TextUtils.isEmpty(deviceName)) {
            deviceNameText.setText(deviceName);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);

        fadeIn.setDuration(getFadeDuration());
        fadeOut.setDuration(getFadeDuration());

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ledRays.startAnimation(fadeOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                int duration = getFadeDuration();
                fadeIn.setDuration(getFadeDuration());
                fadeOut.setDuration(getFadeDuration());
                ledRays.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        ledRays.startAnimation(fadeIn);
    }

    @Override
    public void onPause() {
        super.onPause();
        clearAnimationsAndListeners();
    }

    public void clearAnimationsAndListeners() {
        if (ledRays != null) {
            ledRays.clearAnimation();
        }

        if (fadeIn != null) {
            fadeIn.setAnimationListener(null);
        }

        if (fadeOut != null) {
            fadeOut.setAnimationListener(null);
        }
    }

    private int getFadeDuration() {
        return fadeDuration.get();
    }

    /**
     * Sets the duration for the blinking animation.  This should be based on the proximity of the BLE device
     *
     * @param animationDuration duration of animation in milliseconds
     */
    public void setFadeAnimationDuration(int animationDuration) {
        fadeDuration.set((animationDuration / 100) * (animationDuration / 100) + 100);
    }
}
