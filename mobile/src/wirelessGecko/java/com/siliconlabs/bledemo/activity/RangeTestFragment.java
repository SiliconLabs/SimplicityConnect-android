package com.siliconlabs.bledemo.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.rangetest.RangeTestMode;

/**
 * @author Comarch S.A.
 */

public class RangeTestFragment extends Fragment {

    public static final String ARG_MODE = "ARG_MODE";

    private RangeTestFragmentView view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_range_test, container, false);

        RangeTestPresenter.Controller controller = (RangeTestPresenter.Controller) getActivity();

        view = new RangeTestFragmentView(controller);
        ButterKnife.inject(view, layout);

        int modeCode = getArguments().getInt(ARG_MODE);
        RangeTestMode mode = RangeTestMode.fromCode(modeCode);

        view.setup(mode);

        controller.setView(view);

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view = null;

        RangeTestPresenter.Controller controller = (RangeTestActivity) getActivity();
        controller.setView(null);
    }
}
