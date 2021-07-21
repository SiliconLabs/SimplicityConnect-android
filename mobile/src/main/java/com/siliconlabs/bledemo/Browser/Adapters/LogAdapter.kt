package com.siliconlabs.bledemo.Browser.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Browser.Adapters.LogAdapter.LogViewHolder
import com.siliconlabs.bledemo.Browser.Models.LogType
import com.siliconlabs.bledemo.Browser.Models.Logs.Log
import com.siliconlabs.bledemo.R

class LogAdapter(private var logList: List<Log>, private val context: Context) : RecyclerView.Adapter<LogViewHolder>() {
    private var deviceAddress: String? = null
    private var logByAddress = false

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var tvTime = itemView.findViewById(R.id.tv_log_time) as TextView
        private var tvInfo = itemView.findViewById(R.id.tv_log_info) as TextView

        fun bind(log: Log) {
            if (logByAddress) {
                if (log.deviceAddress == deviceAddress) {
                    if (log.logType == LogType.CALLBACK) {
                        setCallbackLog(log)
                    } else {
                        setCommonLog(log)
                    }
                    tvTime.text = log.logTime
                }
            } else {
                if (log.logType == LogType.CALLBACK) {
                    setCallbackLog(log)
                } else {
                    setCommonLog(log)
                }
                tvTime.text = log.logTime
            }
        }

        private fun setCallbackLog(log: Log) {
            tvInfo.text = "(Callback)" + log.logInfo
            tvInfo.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue))
        }

        private fun setCommonLog(log: Log) {
            tvInfo.text = log.logInfo
            tvInfo.setTextColor(ContextCompat.getColor(context, R.color.silabs_subtle_text))
        }
    }

    fun logByDeviceAddress(deviceAddress: String?) {
        this.deviceAddress = deviceAddress
        logByAddress = true
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logList[position]
        holder.bind(log)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_log, parent, false)
        return LogViewHolder(view)
    }

    override fun getItemCount(): Int {
        return logList.size
    }

    fun setLogList(logList: List<Log>) {
        this.logList = logList
    }
}
