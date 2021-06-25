package com.siliconlabs.bledemo.Advertiser.Models

import android.content.Context
import com.siliconlabs.bledemo.Advertiser.Utils.AdvertiserStorage

object AdvertiserList {
    private var list: ArrayList<Advertiser>? = null

    fun getList(context: Context): ArrayList<Advertiser>{
        if(list == null) list = AdvertiserStorage(context).loadAdvertiserList()
        return list as ArrayList<Advertiser>
    }

}