package com.siliconlabs.bledemo.features.iop_test.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.AdapterIopTestBinding
import com.siliconlabs.bledemo.features.iop_test.models.ItemTestCaseInfo


class IOPTestViewHolder(view: AdapterIopTestBinding) : RecyclerView.ViewHolder(view.root) {
    private val ivTestStatus = view.ivTestStatus
    private val tvTestTitle = view.tvTestTitle
    private val tvTestDescription = view.tvTestDescription
    private val tvTestStatus = view.tvTestStatus
    private val pbTestProgress = view.pbTestProgress

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
            val view =
                AdapterIopTestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return IOPTestViewHolder(view)
        }
    }
}