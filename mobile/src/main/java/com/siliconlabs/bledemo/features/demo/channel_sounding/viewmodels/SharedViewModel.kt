package com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel(){
    val message = MutableLiveData<String>()
}