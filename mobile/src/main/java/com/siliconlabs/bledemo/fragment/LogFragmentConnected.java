package com.siliconlabs.bledemo.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.DeviceServicesActivity;

import java.io.File;

public class LogFragmentConnected extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.log_fragment, container, false);
        TextView tv = view.findViewById(R.id.log_view);
        Button save_log = view.findViewById(R.id.save_log);
        Button share_log = view.findViewById(R.id.share_log);
        Button clear_log = view.findViewById(R.id.clear_log);
        save_log.setOnClickListener(this);
        share_log.setOnClickListener(this);
        clear_log.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_log:
                File f = ((DeviceServicesActivity) getActivity()).save_log();
                String msg = f.toString() + " is saved";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                //Log.i("save_log", "clicked");
                break;
            case R.id.share_log:
                //Log.i("share_log", "clicked");
                ((DeviceServicesActivity) getActivity()).share_log();
                break;
            case R.id.clear_log:
                ((DeviceServicesActivity) getActivity()).clear_log();
                //Log.i("clear_log", "clicked");
                break;
            default:
                break;
        }
    }
}



