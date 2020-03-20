package com.siliconlabs.bledemo.toolbars;


import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.LogAdapter;
import com.siliconlabs.bledemo.log.Log;
import com.siliconlabs.bledemo.services.ShareLogServices;
import com.siliconlabs.bledemo.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class LoggerFragment extends Fragment {

    private LogAdapter adapter;
    private RecyclerView logRV;

    public LoggerFragment() {
    }

    private ToolbarCallback toolbarCallback;

    public LoggerFragment setCallback(ToolbarCallback toolbarCallback) {
        this.toolbarCallback = toolbarCallback;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        ImageView closeBtn = view.findViewById(R.id.imageview_close);
        final Button shareBtn = view.findViewById(R.id.button_share);
        Button clearBtn = view.findViewById(R.id.textview_clear_log);
        Button filterBtn = view.findViewById(R.id.textview_filter_log);
        final LinearLayout searchBoxLL = view.findViewById(R.id.search_log_container);
        EditText searchLogET = view.findViewById(R.id.edit_text_search);
        ImageView clearLogIV = view.findViewById(R.id.search_log_clear);
        final View logSeparator = view.findViewById(R.id.log_separator);
        setSearchBox(searchLogET, clearLogIV);


        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarCallback.close();
            }
        });
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Constants.LOGS.clear();
                adapter.setLogList(Constants.LOGS);
                adapter.notifyDataSetChanged();
            }
        });
        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBoxLL.setVisibility(searchBoxLL.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                logSeparator.setVisibility(logSeparator.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ShareLogServices.class);
                getActivity().startService(intent);
            }
        });
        logRV = view.findViewById(R.id.recyclerview_log);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setStackFromEnd(true);
        logRV.setLayoutManager(llm);
        logRV.setAdapter(adapter);
        adapter.setLogList(Constants.LOGS);
        adapter.notifyDataSetChanged();
        final List<Log> filtered = new ArrayList<>();

        searchLogET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filtered.clear();
                for (Log log : Constants.LOGS) {
                    if (log.getLogInfo().toLowerCase().contains(s.toString().toLowerCase())) {
                        filtered.add(log);
                    }
                }
                adapter.setLogList(filtered);
                adapter.notifyDataSetChanged();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter.getItemCount() > 1) {
            logRV.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    public void setAdapter(LogAdapter adapter) {
        this.adapter = adapter;
    }

    private void setSearchBox(final EditText searchEditText, final ImageView clearImageView) {

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count <= 0) clearImageView.setVisibility(View.GONE);
                else clearImageView.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        clearImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEditText.getText().clear();
            }
        });

    }

    public LogAdapter getAdapter() {
        return this.adapter;
    }
}
