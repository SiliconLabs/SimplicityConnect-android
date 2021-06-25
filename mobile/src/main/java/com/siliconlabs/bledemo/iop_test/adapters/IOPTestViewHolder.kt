package com.siliconlabs.bledemo.iop_test.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.iop_test.models.ItemTestCaseInfo
import kotlinx.android.synthetic.main.adapter_iop_test.view.*

class IOPTestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val ivTestStatus = view.iv_test_status
    private val tvTestTitle = view.tv_test_title
    private val tvTestDescription = view.tv_test_description
    private val tvTestStatus = view.tv_test_status
    private val pbTestProgress = view.pb_test_progress

    fun bind(info: ItemTestCaseInfo) {
        tvTestTitle.text = info.titlesTest
        tvTestDescription.text = info.describe
        setStatus(info)
    }

    private fun setStatus(info: ItemTestCaseInfo) {
        val context = itemView.context

        when (info.getStatusTest()) {
            0 -> {
                ivTestStatus.visibility = View.VISIBLE
                ivTestStatus.setBackgroundResource(R.drawable.ic_test_fail)
                tvTestStatus.visibility = View.VISIBLE
                tvTestStatus.setTextColor(context.getColor(R.color.silabs_red))
                pbTestProgress.visibility = View.GONE
            }
            1 -> {
                ivTestStatus.setBackgroundResource(R.drawable.ic_test_pass)
                ivTestStatus.visibility = View.VISIBLE
                tvTestStatus.visibility = View.VISIBLE
                tvTestStatus.setTextColor(context.getColor(R.color.silabs_blue))
                pbTestProgress.visibility = View.GONE
            }
            2 -> {
                ivTestStatus.visibility = View.GONE
                tvTestStatus.visibility = View.GONE
                pbTestProgress.visibility = View.VISIBLE

            }
            else -> {
                ivTestStatus.visibility = View.GONE
                tvTestStatus.visibility = View.VISIBLE
                tvTestStatus.setTextColor(context.getColor(R.color.silabs_inactive_light))
                pbTestProgress.visibility = View.GONE
            }
        }
        tvTestStatus.text = info.getValueStatusTest()
    }

    companion object {
        fun create(parent: ViewGroup): IOPTestViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_iop_test, parent, false)
            return IOPTestViewHolder(view)
        }
    }
}