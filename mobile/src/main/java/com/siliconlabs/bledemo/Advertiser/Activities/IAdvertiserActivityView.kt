package com.siliconlabs.bledemo.Advertiser.Activities

import com.siliconlabs.bledemo.Advertiser.Models.Advertiser

interface IAdvertiserActivityView {
    fun onAdvertiserPopulated(items: ArrayList<Advertiser>)
    fun onCopyClicked(position: Int)
    fun showEditIntent(item: Advertiser)
    fun onItemRemoved(position: Int)
    fun onItemCreated(position: Int)
    fun refreshItem(position: Int)
    fun startAdvertiserService()
    fun stopAdvertiserService()
    fun showMessage(message: String)
}