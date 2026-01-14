package com.siliconlabs.bledemo.features.demo.smartlock.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import com.siliconlabs.bledemo.features.demo.smartlock.repository.SmartLockRepository
import javax.net.ssl.SSLContext

class SmartLockAwsViewModel(applicaton: Application) :
    AndroidViewModel(applicaton) {
    private val repository = SmartLockRepository(applicaton.applicationContext)

    val smartLockAwsConnectionResult = repository.smartLockAwsConnectionState
    val smartLockAwsMessageLiveData = repository.smartLockAwsMessageLiveData

    fun connect(topic: String, endPoint: String, sslContext: SSLContext) =
        repository.connect(topic, endPoint, sslContext)

    fun subscribe(topic: String) = repository.subscribe(topic)

    fun publish(topic: String, message: String) = repository.publish(topic, message)

    fun disconnect() = repository.disconnect()
}