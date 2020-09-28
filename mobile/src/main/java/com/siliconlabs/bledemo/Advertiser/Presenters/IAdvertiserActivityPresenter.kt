package com.siliconlabs.bledemo.Advertiser.Presenters

import com.siliconlabs.bledemo.Advertiser.Models.Advertiser

interface IAdvertiserActivityPresenter {
    fun populateAdvertiserAdapter()
    fun copyItem(item: Advertiser)
    fun editItem(position: Int)
    fun removeItem(position: Int)
    fun createNewItem()
    fun switchAllItemsOff()
    fun switchItemOn(position: Int)
    fun switchItemOff(position: Int)
    fun persistData()
    fun checkExtendedAdvertisingSupported()
}