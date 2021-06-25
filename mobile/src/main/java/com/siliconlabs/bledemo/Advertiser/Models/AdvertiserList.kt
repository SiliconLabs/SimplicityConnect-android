package com.siliconlabs.bledemo.advertiser.models

import android.content.Context
import com.siliconlabs.bledemo.advertiser.utils.AdvertiserStorage

object AdvertiserList {
    private var list: ArrayList<Advertiser>? = null

    fun getList(context: Context): ArrayList<Advertiser>{
        if(list == null) list = AdvertiserStorage(context).loadAdvertiserList()
        return list as ArrayList<Advertiser>
    }

}