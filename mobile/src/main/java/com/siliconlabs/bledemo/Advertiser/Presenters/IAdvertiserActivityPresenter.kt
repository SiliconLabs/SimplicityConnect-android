package com.siliconlabs.bledemo.advertiser.presenters

import com.siliconlabs.bledemo.advertiser.models.Advertiser

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