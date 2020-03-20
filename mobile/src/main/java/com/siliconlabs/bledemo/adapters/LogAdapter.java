package com.siliconlabs.bledemo.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.siliconlabs.bledemo.other.LogType;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.log.Log;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    private List<Log> logList;
    private Context context;
    private String deviceAddress = null;
    private boolean logByAddress = false;

    public class LogViewHolder extends RecyclerView.ViewHolder {
        TextView timeTV;
        TextView infoTV;

        public LogViewHolder(View itemView) {
            super(itemView);
            timeTV = itemView.findViewById(R.id.textview_log_time);
            infoTV = itemView.findViewById(R.id.textview_log_info);

        }
    }

    public LogAdapter(List logList, Context context) {
        this.logList = logList;
        this.context = context;
    }

    public void logByDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
        logByAddress = true;
        notifyDataSetChanged();
    }


    @Override
    public void onBindViewHolder(LogAdapter.LogViewHolder holder, int position) {
        Log log = logList.get(position);


        if (logByAddress) {
            if (log.getDeviceAddress().equals(this.deviceAddress)) {
                if (logList.get(position).getLogType().equals(LogType.CALLBACK)) {
                    holder.infoTV.setText("(Callback)" + log.getLogInfo());
                    holder.infoTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue));
                } else {
                    holder.infoTV.setText(log.getLogInfo());
                    holder.infoTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text));
                }

                holder.timeTV.setText(logList.get(position).getLogTime());
            }
        } else {
            if (logList.get(position).getLogType().equals(LogType.CALLBACK)) {
                holder.infoTV.setText("(Callback)" + log.getLogInfo());
                holder.infoTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue));
            } else {
                holder.infoTV.setText(log.getLogInfo());
                holder.infoTV.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text));
            }

            holder.timeTV.setText(logList.get(position).getLogTime());
        }
    }

    @Override
    public LogAdapter.LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public void setLogList(List<Log> logList) {
        this.logList = logList;
    }
}
