package com.siliconlabs.bledemo.Browser.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Browser.Adapters.LogAdapter
import com.siliconlabs.bledemo.Browser.Models.Logs.Log
import com.siliconlabs.bledemo.Browser.Services.ShareLogServices
import com.siliconlabs.bledemo.Browser.ToolbarCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.Constants
import kotlinx.android.synthetic.main.fragment_log.*
import java.util.*

class LoggerFragment : Fragment() {
    var adapter: LogAdapter? = null
    private var handler: Handler? = null
    private var allowRefreshScrollBottom = true
    private var isFiltering = false
    private var filteringPhrase: String? = null
    private var toolbarCallback: ToolbarCallback? = null


    private val logUpdater: Runnable = object : Runnable {
        override fun run() {
            adapter?.notifyDataSetChanged()
            handler?.postDelayed(this, LOG_UPDATE_PERIOD.toLong())
            if (allowRefreshScrollBottom) rv_log?.scrollToPosition(adapter?.itemCount!! - 1)
            android.util.Log.d("Log_Updater", "RUN")
        }
    }

    fun setCallback(toolbarCallback: ToolbarCallback?): LoggerFragment {
        this.toolbarCallback = toolbarCallback
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setSearchBox(et_search, iv_search_log_clear)
        iv_close.setOnClickListener {
            toolbarCallback?.close()
            stopLogUpdater()
        }

        btn_clear_log.setOnClickListener {
            Constants.LOGS.clear()
            adapter?.setLogList(Constants.LOGS)
            adapter?.notifyDataSetChanged()
        }

        btn_filter_log.setOnClickListener {
            ll_search_log_container.visibility = if (ll_search_log_container.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            log_separator.visibility = if (log_separator.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btn_share.setOnClickListener {
            val intent = Intent(context, ShareLogServices::class.java).apply {
                putExtra(IS_FILTERING_EXTRA, isFiltering)
                if (isFiltering) putExtra(FILTERING_PHRASE_EXTRA, filteringPhrase)
            }
            activity?.startService(intent)
        }

        val llm = LinearLayoutManager(activity)
        llm.stackFromEnd = true
        rv_log.layoutManager = llm
        rv_log.adapter = adapter
        adapter?.setLogList(Constants.LOGS)
        adapter?.notifyDataSetChanged()

        val filtered: MutableList<Log> = ArrayList()
        et_search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                filtered.clear()
                for (log in Constants.LOGS) {
                    if (log.logInfo.toLowerCase().contains(s.toString().toLowerCase())) {
                        filtered.add(log)
                    }
                }
                if (s.toString().isEmpty()) {
                    adapter?.setLogList(Constants.LOGS)
                    isFiltering = false
                } else {
                    adapter?.setLogList(filtered)
                    isFiltering = true
                    filteringPhrase = s.toString()
                }
                adapter?.notifyDataSetChanged()
            }
        })

        rv_log.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                allowRefreshScrollBottom = !recyclerView.canScrollVertically(1)
            }
        })

    }

    override fun onResume() {
        super.onResume()
        runLogUpdater()
    }

    override fun onPause() {
        super.onPause()
        stopLogUpdater()
        android.util.Log.d("OnPause", "LoggerFragment")
    }

    private fun setSearchBox(searchEditText: EditText, clearImageView: ImageView) {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (count <= 0) clearImageView.visibility = View.GONE else clearImageView.visibility = View.VISIBLE
            }

            override fun afterTextChanged(s: Editable) {}
        })
        clearImageView.setOnClickListener { searchEditText.text.clear() }
    }

    fun scrollToEnd() {
        if (rv_log != null && adapter?.itemCount!! > 1) {
            Handler().postDelayed({ rv_log?.scrollToPosition(adapter?.itemCount!! - 1) }, 200)
        }
    }

    fun runLogUpdater() {
        handler = handler ?: Handler()
        handler?.removeCallbacks(logUpdater)
        handler?.postDelayed(logUpdater, LOG_UPDATE_PERIOD.toLong())
        android.util.Log.d("Log_Updater", "START")
    }

    fun stopLogUpdater() {
        handler?.removeCallbacks(logUpdater)
        android.util.Log.d("Log_Updater", "STOP")
    }

    companion object {
        const val IS_FILTERING_EXTRA = "IS_FILTERING_EXTRA"
        const val FILTERING_PHRASE_EXTRA = "FILTERING_PHRASE_EXTRA"
        private const val LOG_UPDATE_PERIOD = 2000
    }
}