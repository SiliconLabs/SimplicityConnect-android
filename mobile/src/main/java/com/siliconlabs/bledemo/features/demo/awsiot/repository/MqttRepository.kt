package com.siliconlabs.bledemo.features.demo.awsiot.repository

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.configure.advertiser.services.MqttForegroundService
import com.siliconlabs.bledemo.utils.CustomToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.InputStream
import java.security.KeyStore
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class MqttRepository(private val context: Context) {

    private val mqttClient =
        MqttAndroidClient(context, "ssl://$AWS_END_POINT:8883", UUID.randomUUID().toString())
    private val _mqttMessageLiveData = MutableLiveData<String>()
    val mqttMessageLiveData: LiveData<String> = _mqttMessageLiveData
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var oldTopic = MutableLiveData<String>()
    var oldPublishTopic  = MutableLiveData<String>()

    private val _connectionState = MutableLiveData<ConnectionResult>()
    val connectionState: LiveData<ConnectionResult> = _connectionState

    private var options = MqttConnectOptions().apply {
        isCleanSession = true
        isAutomaticReconnect = false // Manual retry logic for better control
        keepAliveInterval = 30
        socketFactory = createSSLContext(context).socketFactory
        setWill("debug/status", "Client disconnected unexpectedly".toByteArray(), 1, true)
    }


    private var retryDelay = 1000L // Start with 1 second
    private val maxRetryDelay = 30000L // Cap retry at 30 seconds

    fun connect(subscribeTopic: String) {
        // Show foreground notification when connecting
        coroutineScope.launch {
            withContext(Dispatchers.IO){
                context.startMqttForegroundService()
            }
        }
        coroutineScope.launch {
            withContext(Dispatchers.Main){
                _connectionState.value = ConnectionResult.Connecting
            }
        }
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                mqttClient.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT", "Connection lost: ${cause?.message}")
                        //CustomToastManager.show(context,"Connection Lost",5000)
                        _connectionState.value =
                            ConnectionResult.Error("Connection Lost")
                        retryConnection()
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        _mqttMessageLiveData.postValue(message?.toString() ?: "")
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                try {
                    mqttClient.connect(options, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("MQTT", "Connected successfully")
                            //CustomToastManager.show(context,"Connected successfully",5000)
                            _connectionState.value = ConnectionResult.Connected
                            retryDelay = 1000L // Reset retry delay after success
                            oldTopic.value = subscribeTopic
                            subscribe(subscribeTopic)

                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                            Log.e("MQTT", "Connection failed: ${exception?.message}")
                            //CustomToastManager.show(context,"Connection failed: ${exception?.message}",5000)
                            _connectionState.value =
                                ConnectionResult.Error("Connection failed: ${exception?.message}")
                            retryConnection()
                        }
                    })
                } catch (e: Exception) {
                    Log.e("MQTT", "Exception in connect: ${e.message}")
                    //CustomToastManager.show(context,"Exception in connect: ${e.message}",5000)
                    _connectionState.value =
                        ConnectionResult.Error("Exception in connect: ${e.message}")

                    retryConnection()
                }
            }
        }
    }

    fun subscribe(topic: String) {
        _connectionState.value = ConnectionResult.SubscribeConnecting
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT", "Subscribed to $topic successfully")
                        _connectionState.value = ConnectionResult.SubscribeConnected
                        CustomToastManager.show(context, "Subscribed to $topic successfully", 5000)

                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT", "Subscription failed: ${exception?.message}")
                        //CustomToastManager.show(context,"Subscription failed: ${exception?.message}",5000)
                        _connectionState.value =
                            ConnectionResult.Error("Subscription failed: ${exception?.message}")
                    }
                })

                mqttClient.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT", "Connection lost: ${cause?.message}")
                        //CustomToastManager.show(context,"Connection lost: ${cause?.message}",5000)
                        _connectionState.value =
                            ConnectionResult.Error("Connection lost")

                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        Log.d("MQTT", "Message received: ${message.toString()}")


                        // Post to LiveData on the Main Thread
                        CoroutineScope(Dispatchers.Main).launch {
                            _mqttMessageLiveData.postValue(message.toString())
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d("MQTT", "Message delivery complete")
                        CustomToastManager.show(context, "Message delivery complete", 5000)

                    }
                })
            } catch (e: Exception) {
                Log.e("MQTT", "Error in subscription: ${e.message}")
                //CustomToastManager.show(context,"Error in subscription: ${e.message}",5000)
                _connectionState.value =
                    ConnectionResult.Error("Error in subscription: ${e.message}")
            }
        }
    }

    private fun retryConnection() {
        _connectionState.value = ConnectionResult.Connecting
        coroutineScope.launch {
            delay(retryDelay)
            Log.d("MQTT", "Retrying connection in ${retryDelay / 1000} seconds...")
            CustomToastManager.show(
                context,
                "Retrying connection in ${retryDelay / 1000} seconds...",
                5000
            )
            retryDelay = (retryDelay * 2).coerceAtMost(maxRetryDelay) // Exponential backoff
            connect(oldTopic.value.toString())
        }
    }

    fun publish(topic: String, message: String) {
        oldPublishTopic.value = topic
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                if (mqttClient.isConnected) {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttClient.publish(topic, mqttMessage)
                    withContext(Dispatchers.Main) {
                        CustomToastManager.show(context, "Publish Successfully", 5000)
                    }


                } else {
                    Log.e("MQTT", "Publish failed: MQTT is not connected")
                    // CustomToastManager.show(context,"Publish failed: MQTT is not connected",5000)
                    _connectionState.value =
                        ConnectionResult.Error("Publish failed: MQTT is not connected")
                }
            }
        }
    }

    fun disconnect() {
        _connectionState.value = ConnectionResult.Disconnecting
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (null != mqttClient && mqttClient.isConnected) {
                        try {

                            mqttClient.disconnect(null, object : IMqttActionListener {
                                override fun onSuccess(asyncActionToken: IMqttToken?) {
                                    _connectionState.value = ConnectionResult.Disconnected
                                    coroutineScope.launch {
                                        withContext(Dispatchers.Main) {
                                            /*CustomToastManager.show(
                                                context,
                                                "AWS IoT Disconnected Successfully!",
                                                3000
                                            )*/
                                        }
                                    }
                                }

                                override fun onFailure(
                                    asyncActionToken: IMqttToken?,
                                    exception: Throwable?
                                ) {
                                    Log.e("MQTT", "Disconnection Failure")
                                    _connectionState.value = ConnectionResult.DisconnectionError
                                }

                            })
                        } catch (e: MqttException) {
                            Log.e("Mqtt Disconnection", "" + e.message)
                            _connectionState.value = ConnectionResult.DisconnectionError
                        }
                    } else {
                        _connectionState.value = ConnectionResult.DisconnectionError
                    }

                } catch (e: Exception) {
                    Log.e("MQTT", "Error during disconnect: ${e.message}")
                    _connectionState.value = ConnectionResult.DisconnectionError
                    _connectionState.value =
                        ConnectionResult.Error("Error during disconnect: ${e.message}")
                }
            }
        }
    }

    companion object {

        const val AWS_END_POINT = "a2m21kovu9tcsh-ats.iot.us-east-2.amazonaws.com"


        fun createSSLContext(context: Context): SSLContext {
            val keyStore = KeyStore.getInstance("PKCS12")
            val password =
                "1234567890".toCharArray() // Use the same password you used to create the .p12 file

            val inputStream: InputStream =
                context.resources.openRawResource(R.raw.silabsawsiot) // Put server.p12 in res/raw
            keyStore.load(inputStream, password)

            val keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, password)

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, null, null)

            return sslContext
        }
    }


    fun unsubscribeFromTopic(){
        coroutineScope.launch {
            withContext(Dispatchers.IO){
                if(null != mqttClient){
                    mqttClient.unsubscribe(oldTopic.value)
                }
            }
        }
    }

    fun Context.startMqttForegroundService() {
        val intent = Intent(this, MqttForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }
    }
}

sealed class ConnectionResult {
    object Connecting : ConnectionResult()
    object Connected : ConnectionResult()
    data class Error(val message: String, val throwable: Throwable? = null) : ConnectionResult()
    object SubscribeConnecting : ConnectionResult()
    object SubscribeConnected : ConnectionResult()
    object Disconnecting : ConnectionResult()
    object Disconnected : ConnectionResult()
    object DisconnectionError: ConnectionResult()
}