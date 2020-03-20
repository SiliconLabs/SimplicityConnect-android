package com.siliconlabs.bledemo.views;

import android.content.Context;

import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

/**
 * @author Comarch S.A.
 */

public class BlockableSpinner extends Spinner {
    public BlockableSpinner(Context context) {
        super(context);
    }

    public BlockableSpinner(Context context, int mode) {
        super(context, mode);
    }

    public BlockableSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlockableSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlockableSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
    }

    @Override
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        OnItemSelectedListenerWrapper wrapper = null;

        if (listener != null) {
            wrapper = new OnItemSelectedListenerWrapper(listener);
        }

        super.setOnItemSelectedListener(wrapper);
    }

    private class OnItemSelectedListenerWrapper implements OnItemSelectedListener {

        private final OnItemSelectedListener original;

        private OnItemSelectedListenerWrapper(OnItemSelectedListener original) {
            this.original = original;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            original.onItemSelected(parent, view, position, id);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            original.onNothingSelected(parent);
        }
    }
}
