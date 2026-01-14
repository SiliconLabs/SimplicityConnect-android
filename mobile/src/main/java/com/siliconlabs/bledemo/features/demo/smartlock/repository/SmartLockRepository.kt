package com.siliconlabs.bledemo.features.demo.smartlock.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siliconlabs.bledemo.R
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import timber.log.Timber
import java.io.InputStream
import java.security.KeyStore
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class SmartLockRepository(private val context: Context) {

    private val _smartLockAwsMessageLiveData = MutableLiveData<String>()
    val smartLockAwsMessageLiveData: LiveData<String> = _smartLockAwsMessageLiveData

    private val _smartLockAwsConnectionState = MutableLiveData<SmartLockConnectionResult>()
    val smartLockAwsConnectionState: LiveData<SmartLockConnectionResult> =
        _smartLockAwsConnectionState
    private lateinit var smartLockClient: MqttAndroidClient

//    private val smartLockClient = MqttAndroidClient(
//        context,
//        "ssl://$SMART_LOCK_AWS_END_POINT:8883",
//        UUID.randomUUID().toString(),
//        null,
//        MqttAndroidClient.Ack.AUTO_ACK
//    )

    fun connect(subscribeTopic: String, endPoint: String, sslContext: SSLContext) {
        println("Smart Lock connect--> subscribeTopic topic:$subscribeTopic")
        val options = MqttConnectOptions()
        options.isCleanSession = false
        //options.socketFactory = createSmartLockAwsSSLContext(context).socketFactory
        options.socketFactory = sslContext.socketFactory
        smartLockClient = MqttAndroidClient(
            context,
            "ssl://$endPoint:8883",
            UUID.randomUUID().toString(),
            null,
            MqttAndroidClient.Ack.AUTO_ACK
        )
        smartLockClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {

            }

            override fun messageArrived(
                topic: String?,
                message: MqttMessage?
            ) {
                println("smart repo ${message.toString()}")
                _smartLockAwsMessageLiveData.postValue(message.toString())
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if (reconnect) {
                    smartLockClient.subscribe(subscribeTopic, 1)
                }
            }

        })

        smartLockClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Timber.tag(TAG).d("Mqtt Connect")
                _smartLockAwsConnectionState.postValue(SmartLockConnectionResult.AWSConnected)
                subscribe(subscribeTopic)
            }

            override fun onFailure(
                asyncActionToken: IMqttToken?,
                exception: Throwable?
            ) {
                if (exception is MqttException && exception.reasonCode.toShort() == MqttException.REASON_CODE_BROKER_UNAVAILABLE) {
                    Timber.tag(TAG).e("Connection failed: Broker unavailable")
                    _smartLockAwsConnectionState.postValue(
                        SmartLockConnectionResult
                            .AWSConnectError("Connection failed: Broker unavailable", exception)
                    )
                } else if (exception is MqttException && exception.reasonCode.toShort() == MqttException.REASON_CODE_FAILED_AUTHENTICATION) {
                    Timber.tag(TAG).e("Authentication failed:Wrong user/Wrong Password- ${exception.message}")
                    _smartLockAwsConnectionState.postValue(
                        SmartLockConnectionResult
                            .AWSConnectError("Authentication failed:Wrong user/Wrong Password- ${exception.message}", exception)
                    )  } else {
                    Timber.tag(TAG).e("Connection failed: ${exception?.message}")
                    _smartLockAwsConnectionState.postValue(
                        SmartLockConnectionResult
                            .AWSConnectError("Connection failed ${exception?.message}")
                    )
                }
            }

        })
    }

    fun subscribe(subscribeTopic: String) {
        println("Smart Lock subscribe--> subscribeTopic topic:$subscribeTopic")
        //CustomToastManager.showError(context, "Subscribed to $subscribeTopic", 2000)
        smartLockClient.let {
            it.subscribe(subscribeTopic, 1, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    _smartLockAwsConnectionState.postValue(SmartLockConnectionResult.AWSSubscribed)
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken?,
                    exception: Throwable?
                ) {
                    _smartLockAwsConnectionState.postValue(
                        SmartLockConnectionResult
                            .AWSConnectError("subscribe failed: subscribe unavailable", exception)
                    )
                }

            })
        }
    }

    fun publish(topic: String, message: String) {
        if(::smartLockClient.isInitialized) {
            if (null != smartLockClient && smartLockClient.isConnected) {
                smartLockClient.let { client ->
                    val data = MqttMessage(message.toByteArray()).apply {
                        qos = 1
                        isRetained = false
                    }
                    try {
                        client.publish(topic, data)
                        _smartLockAwsConnectionState.postValue(SmartLockConnectionResult.AWSPublished)
                    } catch (e: MqttException) {
                        Timber.tag(TAG).e("Publish Exception ${e.message} ")
                    }

                }
            }
        }else {
            Timber.tag(TAG).w("SmartLockClient is not initialized or not connected")
        }
    }

    fun disconnect() {
        if(::smartLockClient.isInitialized) {
            if (null != smartLockClient && smartLockClient.isConnected) {
                smartLockClient.disconnect()
                _smartLockAwsConnectionState.postValue(SmartLockConnectionResult.AWSDisconnected)
            }
        }else {
            Timber.tag(TAG).w("SmartLockClient is not initialized or already disconnected")
        }
    }

    companion object {
        private val TAG = SmartLockRepository::class.java.toString()
    }
}

sealed class SmartLockConnectionResult {
    object AWSConnected : SmartLockConnectionResult()
    object AWSSubscribed : SmartLockConnectionResult()
    object AWSPublished : SmartLockConnectionResult()
    object AWSDisconnected : SmartLockConnectionResult()
    data class AWSConnectError(val message: String, val throwable: Throwable? = null) :
        SmartLockConnectionResult()
}