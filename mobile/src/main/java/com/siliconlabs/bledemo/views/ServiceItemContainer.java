package com.siliconlabs.bledemo.views;

import android.content.Context;

import androidx.cardview.widget.CardView;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.utils.BLEUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ServiceItemContainer extends LinearLayout {
    private int ANIMATION_DURATION_FOR_EXPAND_AND_COLLAPSE = 333;

    @InjectView(R.id.service_info_card_view)
    public CardView serviceInfoCardView;
    @InjectView(R.id.btn_expand_to_show_characteristics)
    public Button btnExpandToShowCharacteristics;
    @InjectView(R.id.service_title)
    public TextView serviceTitleTextView;
    @InjectView(R.id.image_view_edit_service)
    public ImageView serviceEditNameImageView;
    @InjectView(R.id.linear_layout_edit_service_name)
    public LinearLayout serviceEditNameLinearLayout;
    @InjectView(R.id.sevice_uuid)
    public TextView serviceUuidTextView;
    @InjectView(R.id.container_of_characteristics_for_service)
    public LinearLayout groupOfCharacteristicsForService;
    @InjectView(R.id.last_item_divider)
    public LinearLayout lastItemDivider;

    private Map<String, BLEUtils.Notifications> characteristicNotificationStates;

    public ServiceItemContainer(Context context) {
        super(context);
        init(context);
    }

    public ServiceItemContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ServiceItemContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.list_item_debug_mode_service, this);

        ButterKnife.inject(this);

        btnExpandToShowCharacteristics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (groupOfCharacteristicsForService.getVisibility() == View.VISIBLE) {
                    btnExpandToShowCharacteristics.setText(getResources().getString(R.string.More_Info));
//                    animateCaretCollapse(context, serviceExpansionCaret);\
                    groupOfCharacteristicsForService.setVisibility(GONE);
                } else {
                    btnExpandToShowCharacteristics.setText(getResources().getString(R.string.Less_Info));
                    animateCharacteristicExpansion();
                }
            }
        });

        serviceInfoCardView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (groupOfCharacteristicsForService.getVisibility() == View.VISIBLE) {
                    btnExpandToShowCharacteristics.setText(getResources().getString(R.string.More_Info));
//                    animateCaretCollapse(context, serviceExpansionCaret);\
                    groupOfCharacteristicsForService.setVisibility(GONE);
                } else {
                    btnExpandToShowCharacteristics.setText(getResources().getString(R.string.Less_Info));
                    animateCharacteristicExpansion();
                }
            }
        });

        characteristicNotificationStates = new HashMap<>();
    }

    private void animateCharacteristicExpansion() {
        final View characteristicsExpansion = this.groupOfCharacteristicsForService;

        characteristicsExpansion.measure(FlowLayout.LayoutParams.MATCH_PARENT, FlowLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = characteristicsExpansion.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        characteristicsExpansion.getLayoutParams().height = 1;
        characteristicsExpansion.setVisibility(View.VISIBLE);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                characteristicsExpansion.getLayoutParams().height = interpolatedTime == 1 ? FlowLayout.LayoutParams.WRAP_CONTENT : (int) (targetHeight * interpolatedTime);
                characteristicsExpansion.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(ANIMATION_DURATION_FOR_EXPAND_AND_COLLAPSE);
        characteristicsExpansion.startAnimation(animation);
    }

    public void setCharacteristicNotificationState(String characteristicName, BLEUtils.Notifications state) {
        characteristicNotificationStates.put(characteristicName, state);
    }

    public BLEUtils.Notifications getCharacteristicNotificationState(String characteristicName) {
        return characteristicNotificationStates.get(characteristicName);
    }
}
