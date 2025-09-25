package com.siliconlabs.bledemo.features.demo.awsiot

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ActivityAwsDemoBinding
import com.siliconlabs.bledemo.features.configure.advertiser.services.MqttForegroundService
import com.siliconlabs.bledemo.features.configure.advertiser.services.MqttForegroundService.Companion.NOTIFICATION_ID
import com.siliconlabs.bledemo.features.demo.awsiot.adapter.GridAdapter
import com.siliconlabs.bledemo.features.demo.awsiot.listener.OnMqttGridItemClickListener
import com.siliconlabs.bledemo.features.demo.awsiot.model.GridItem
import com.siliconlabs.bledemo.features.demo.awsiot.repository.ConnectionResult
import com.siliconlabs.bledemo.features.demo.awsiot.viewmodel.MqttViewModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import com.siliconlabs.bledemo.utils.AppUtil
import com.siliconlabs.bledemo.utils.CustomToastManager
import org.json.JSONException
import org.json.JSONObject


class AWSIOTDemoActivity : AppCompatActivity(), OnMqttGridItemClickListener {
    private lateinit var backPressedCallback: OnBackPressedCallback
    private val messageTimeoutMillis: Long = 30000L
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    private var publishTopic = ""
    private var subscribeTopic = ""
    private lateinit var binding: ActivityAwsDemoBinding

    private lateinit var sharedPreferences: SharedPreferences
    private var customProgressDialog: CustomProgressDialog? = null
    private val mqttViewModel: MqttViewModel by viewModels { MqttViewModelFactory(this) }
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GridAdapter

    // Keep track of the current state of each LED button
    private var isRedOn = false
    private var isGreenOn = false
    private var isBlueOn = false

    companion object {
        private const val PREFS_NAME = "MqttPrefs"
        private const val KEY_SUBSCRIBE_TOPIC = "subscribeTopic"
        private const val KEY_PUBLISH_TOPIC = "publishTopic"

        fun isValidJsonObject(jsonString: String): Boolean {
            return try {
                JSONObject(jsonString)
                true // Parsing succeeded, it's valid JSON
            } catch (e: JSONException) {
                false // Parsing failed, it's not valid JSON
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAwsDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppUtil.setEdgeToEdge(window, this)
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        actionBar!!.setHomeAsUpIndicator(R.drawable.matter_back)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = getString(R.string.aws_dashboard)
        actionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#0F62FE")))
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initUI()

        initObservers()


        // Register the back press callback
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("Activity", "Back button pressed with OnBackPressedDispatcher!")

                //showDisconnectConfirmationDialog()
                if(null != mqttViewModel){
                    mqttViewModel.unsubscribeFromTopic()
                    mqttViewModel.disconnect()
                    finish()
                }
            }
        }

        // Add callback to the dispatcher
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun initObservers() {


        mqttViewModel.connectionResult.observe(this, Observer { result ->
            when (result) {
                is ConnectionResult.Connecting -> {
                    showProgressDialog(getString(R.string.connecting_to_aws))
                }

                is ConnectionResult.Connected -> {

                    removeProgress()
                    runOnUiThread {
                        CustomToastManager.show(this,
                            getString(R.string.connection_successful), 5000)

                    }


                }

                is ConnectionResult.Error -> {
                    // Handle the error
                    println("Connection Error: ${result.message}")
                    result.throwable?.printStackTrace() // Print the stack trace for debugging

                    runOnUiThread {
                        if (result.message.contains(getString(R.string.publish_failed),false) or result.message.contains(
                                getString(
                                    R.string.subscription_failed
                                ),false)){
                            removeProgress()
                            CustomToastManager.show(this, result.message, 5000)
                            showMqttTopicDialog()
                        }else{
                            CustomToastManager.show(this, result.message, 5000)
                        }

                    }
                }

                ConnectionResult.SubscribeConnected -> {
                    binding.placeholder.visibility = View.GONE
                    showProgressDialog(getString(R.string.loading_data))
                    // Start timeout timer
                    startMessageTimeout()

                    mqttViewModel.mqttMessages.observe(this, Observer {
                        // Cancel timeout if message received
                        handler.removeCallbacks(timeoutRunnable!!)
                        removeProgress()
                        Log.e("AWS DEMO ACTIVITY", "AWS IOT MESSAGES" + it.toString())

                        val jsonString = it.toString() // Replace with real JSON source
                        if (isValidJsonObject(jsonString)){
                            runOnUiThread {
                                updateGrid(JSONObject(jsonString))
                            }

                        }else{
                            // do not do anything
                        }


                    })
                }

                ConnectionResult.SubscribeConnecting -> {
                    CustomToastManager.show(this,getString(R.string.subscribing_to_the_entered_topic),5000)
                }

                ConnectionResult.Disconnected -> {
                    removeProgress()
                    //this.finish()
                }

                ConnectionResult.Disconnecting -> {
                    showProgressDialog(getString(R.string.disconnecting_from_aws_iot))
                }

                ConnectionResult.DisconnectionError -> {
                    this.finish()
                }
            }
        })
    }

    // Function to start the timeout
    private fun startMessageTimeout() {
        timeoutRunnable = Runnable {
            removeProgress()
            showAlertDialogForRetry()
        }
        handler.postDelayed(timeoutRunnable!!, messageTimeoutMillis)
    }



    // Function to show AlertDialog for topic re-entry
    private fun showAlertDialogForRetry() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.subscription_timeout))
            .setMessage(getString(R.string.no_mess_received_within_thirtysec))
            .setPositiveButton(getString(R.string.retry)) { _, _ ->
                // Reopen the dialog for topic entry
                showMqttTopicDialog()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                this.finish()
            }
            .show()
    }

    private fun updateGrid(jsonObject: JSONObject) {
        val orderedKeys = listOf(getString(R.string.aws_temperature),
            getString(R.string.aws_humidity), getString(R.string.aws_ambient_light),
            getString(R.string.aws_white_light))
        val newItems = mutableListOf<GridItem>()
        var motionItem: GridItem? = null // Use null to indicate no motion yet

        var accelerometerData = ""
        var gyroData = ""

        // Process accelerometer and gyro data first
        jsonObject.keys().forEach { key ->
            val value = jsonObject.get(key)

            when (key) {
                getString(R.string.aws_accelerometer) -> {
                    val acc = value as JSONObject
                    accelerometerData =
                        "Acc: (${acc.getDouble("x")}, ${acc.getDouble("y")}, ${acc.getDouble("z")})"
                }

                getString(R.string.aws_gyro) -> {
                    val gyro = value as JSONObject
                    gyroData =
                        "Gyro: (${gyro.getDouble("x")}, ${gyro.getDouble("y")}, ${gyro.getDouble("z")})"
                }
            }
        }

        // Create Motion item if data is present
        if (accelerometerData.isNotEmpty() && gyroData.isNotEmpty()) {
            val motionData = "$accelerometerData\n$gyroData"
            motionItem = GridItem(
                "Motion",
                motionData,
                R.drawable.icon_dks_917_motion
            )
        }
        // Process other keys according to ordered key
        orderedKeys.forEach { key ->
            if (jsonObject.has(key)) {
                val value = jsonObject.get(key)
                if (value !is JSONObject) {
                    newItems.add(GridItem(key.replaceFirstChar { it.uppercase().replace("_"," ") }, value.toString(), getIconForKey(key)))
                }
            }
        }

        // Find and Extract Items to Reorder
        val itemsToReorder = mutableListOf<GridItem>()
        val itemsToRemove = mutableListOf<GridItem>()

        newItems.forEach { gridItem ->
            when (gridItem.title.lowercase()) {
                "temperature" -> {
                    itemsToReorder.add(gridItem)
                    itemsToRemove.add(gridItem)
                }
                "humidity" -> {
                    itemsToReorder.add(gridItem)
                    itemsToRemove.add(gridItem)
                }
                "ambient light" -> {
                    itemsToReorder.add(gridItem)
                    itemsToRemove.add(gridItem)
                }
                "white light" -> {
                    itemsToReorder.add(gridItem)
                    itemsToRemove.add(gridItem)
                }
            }
        }

        // Remove items from newItems
        newItems.removeAll(itemsToRemove)

        // Add the item into the newItems list according to the index
        if(itemsToReorder.size > 0){
            if(itemsToReorder.size > 0)
                newItems.add(0,itemsToReorder.get(0))
            if(itemsToReorder.size > 1)
                newItems.add(1,itemsToReorder.get(1))
            if(itemsToReorder.size > 2)
                newItems.add(2,itemsToReorder.get(2))
            if(itemsToReorder.size > 3)
                newItems.add(3,itemsToReorder.get(3))
        }


        // Add motion in 5th position if any
        if (motionItem != null) {
            // Ensure there are at least 5 items by adding placeholders if needed
            while (newItems.size < 4) {
                newItems.add(GridItem("", "", R.drawable.icon_dks_917_motion))//placeholder icon
            }
            newItems.add(motionItem)
        }

        // Add LED as the last item
        newItems.add(GridItem(getString(R.string.aws_led), " ", R.drawable.icon_dks_917_led))

        // Update the adapter
        adapter.updateData(newItems)
    }

    private fun getIconForKey(key: String): Int {
        return when (key.lowercase()) {
            getString(R.string.aws_temperature) -> R.drawable.icon_temp
            getString(R.string.aws_humidity) -> R.drawable.icon_environment
            getString(R.string.aws_ambient_light) -> R.drawable.icon_light
            getString(R.string.aws_white_light) -> R.drawable.icon_light
            getString(R.string.motion_demo_title) -> R.drawable.icon_dks_917_motion

            else -> R.drawable.background_grey_box
        }
    }

    private fun initUI() {
        recyclerView = binding.mqttRv
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 2 columns

        adapter = GridAdapter(emptyList(), this,mqttViewModel,this)
        recyclerView.adapter = adapter
        showMqttTopicDialog()

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                //showDisconnectConfirmationDialog()
                if(null != mqttViewModel){
                    mqttViewModel.unsubscribeFromTopic()
                    mqttViewModel.disconnect()
                    finish()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun removeProgress() {
        runOnUiThread {
            if (customProgressDialog?.isShowing() == true) {
                customProgressDialog?.dismiss()
            }
        }
    }

    private fun showProgressDialog(message: String) {
        runOnUiThread {
            customProgressDialog = CustomProgressDialog(this)
            customProgressDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            customProgressDialog!!.setMessage(message)
            customProgressDialog!!.show()
        }

    }

    private fun showMqttTopicDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_mqtt, null)
        builder.setView(dialogView)

        val subscribeEditText = dialogView.findViewById<TextInputEditText>(R.id.editSubTopic)
        val publishEditText = dialogView.findViewById<TextInputEditText>(R.id.editPubTopic)
        val submitButton: MaterialButton = dialogView.findViewById(R.id.submitMqttButton)
        val cancelButton: TextView = dialogView.findViewById(R.id.submitMqttCancelButton)

        builder.setCancelable(false)

        val dialog = builder.create()

        // Load saved topics from SharedPreferences
        loadSavedTopics(subscribeEditText, publishEditText)

        submitButton.setOnClickListener { _ ->
            subscribeTopic = subscribeEditText.text.toString()
            publishTopic = publishEditText.text.toString()

            when {
                subscribeTopic.isBlank() && publishTopic.isBlank() -> {
                    // Both are empty or contain only whitespace
                    CustomToastManager.show(
                        this,
                        getString(R.string.subscribe_and_publish_topics_should_not_be_empty),
                        5000
                    )
                }

                subscribeTopic.isBlank() -> {
                    // Only subscribeTopic is empty or contains only whitespace
                    CustomToastManager.show(
                        this,
                        getString(R.string.subscribe_topic_should_not_be_empty),
                        5000
                    )
                }

                publishTopic.isBlank() -> {
                    // Only publishTopic is empty or contains only whitespace
                    CustomToastManager.show(
                        this,
                        getString(R.string.publish_topic_should_not_be_empty),
                        5000
                    )
                }



                else -> {
                    // Both are valid
                    //mqttViewModel.subscribe(subscribeTopic)
                    if(areMqttTopicsValid(publishTopic, subscribeTopic)){
                        mqttViewModel.connect(subscribeTopic)
                        saveTopics(subscribeTopic, publishTopic)
                        dialog.dismiss()
                    }

                }
            }
        }
        cancelButton.setOnClickListener {
            dialog.dismiss()
            this.finish()
        }

        dialog.show()
    }


    fun areMqttTopicsValid(publishTopic: String, subscribeTopic: String): Boolean {
        // Common Validation for both topics (Non-empty, no leading/trailing slashes)
        fun isValidCommonTopic(topic: String): Boolean {
            if (topic.isBlank() || topic.startsWith("/") || topic.endsWith("/")) return false
            if (topic.split("/").any { it.isEmpty() }) return false  // No empty levels
            return true
        }

        // Validate Publish Topic (No wildcards)
        fun isValidPublishTopic(topic: String): Boolean {
            return isValidCommonTopic(topic) && !topic.contains("+") && !topic.contains("#")
        }

        // Validate Subscribe Topic (Allow wildcards with correct usage)
        fun isValidSubscribeTopic(topic: String): Boolean {
            if (!isValidCommonTopic(topic)) return false
            val levels = topic.split("/")
            for (level in levels) {
                if (level.contains("+") && level.length > 1) return false // '+' must be a single-level
                if (level.contains("#") && levels.last() != level) return false // '#' only at the end
            }
            return true
        }

        // Final Validation
        val isPublishValid = isValidPublishTopic(publishTopic)
        val isSubscribeValid = isValidSubscribeTopic(subscribeTopic)

        if (!isPublishValid) CustomToastManager.show(this,"Invalid Publish Topic: $publishTopic",5000)
        if (!isSubscribeValid) CustomToastManager.show(this,"Invalid Subscribe Topic: $subscribeTopic",5000)

        return isPublishValid && isSubscribeValid
    }

    private fun loadSavedTopics(
        subscribeEditText: TextInputEditText,
        publishEditText: TextInputEditText
    ) {
        val savedSubscribeTopic = sharedPreferences.getString(KEY_SUBSCRIBE_TOPIC, "")
        val savedPublishTopic = sharedPreferences.getString(KEY_PUBLISH_TOPIC, "")

        subscribeEditText.setText(savedSubscribeTopic)
        publishEditText.setText(savedPublishTopic)
    }

    private fun saveTopics(subscribeTopic: String, publishTopic: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_SUBSCRIBE_TOPIC, subscribeTopic)
        editor.putString(KEY_PUBLISH_TOPIC, publishTopic)
        editor.apply()
    }


    override fun onItemClick(
        ledRedButtonControlStatus: Boolean,
        ledGreenButtonControlStatus: Boolean,
        ledBlueButtonControlStatus: Boolean
    ) {
        var mqttMessage: String? = null

        // Handle Red Button
        if (ledRedButtonControlStatus) {
            isRedOn = !isRedOn
            mqttMessage = if (isRedOn) {
                "{\n" +
                        "  \"red\": \"on\",\n" +
                        "  \"green\": \"off\",\n" +
                        "  \"blue\": \"off\"\n" +
                        "}"
            } else {
                "{\n" +
                        "  \"red\": \"off\",\n" +
                        "  \"green\": \"off\",\n" +
                        "  \"blue\": \"off\"\n" +
                        "}"
            }
            // Turn off other colors when Red is toggled
            isGreenOn = false
            isBlueOn = false
        }

        // Handle Green Button
        else if (ledGreenButtonControlStatus) {
            isGreenOn = !isGreenOn
            mqttMessage = if (isGreenOn) {
                "{\n" +
                        "  \"red\": \"off\",\n" +
                        "  \"green\": \"on\",\n" +
                        "  \"blue\": \"off\"\n" +
                        "}"
            } else {
                "{\n" +
                        "  \"red\": \"off\",\n" +
                        "  \"green\": \"off\",\n" +
                        "  \"blue\": \"off\"\n" +
                        "}"
            }
            // Turn off other colors when Green is toggled
            isRedOn = false
            isBlueOn = false
        }

        // Handle Blue Button
        else if (ledBlueButtonControlStatus) {
            isBlueOn = !isBlueOn
            mqttMessage = if (isBlueOn) {
                "{\n" +
                        "  \"red\": \"off\",\n" +
                        "  \"green\": \"off\",\n" +
                        "  \"blue\": \"on\"\n" +
                        "}"
            } else {
                "{\n" +
                        "  \"red\": \"off\",\n" +
                        "  \"green\": \"off\",\n" +
                        "  \"blue\": \"off\"\n" +
                        "}"
            }
            // Turn off other colors when Blue is toggled
            isRedOn = false
            isGreenOn = false
        }

        if (mqttMessage!= null)
        {
            mqttViewModel.publish(topic = publishTopic, message = mqttMessage)
        }
    }

    override fun onCloseButtonClicked(buttonClickedStatus: Boolean) {
        val mqttMessage = "{\n" +
                "  \"red\": \"off\",\n" +
                "  \"green\": \"off\",\n" +
                "  \"blue\": \"off\"\n" +
                "}"
        mqttViewModel.publish(topic = publishTopic, message = mqttMessage)
    }

    override fun onDestroy() {
        backPressedCallback.remove()
        stopMqttForegroundService()
        cancelNotification()
        super.onDestroy()
    }
    private fun Context.stopMqttForegroundService() {
        val intent = Intent(this, MqttForegroundService::class.java)
        this.stopService(intent)
    }
    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

class MqttViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MqttViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MqttViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }


}
