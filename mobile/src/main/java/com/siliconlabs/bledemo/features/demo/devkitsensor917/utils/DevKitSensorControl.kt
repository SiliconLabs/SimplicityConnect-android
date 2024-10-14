package com.siliconlabs.bledemo.features.demo.devkitsensor917.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.devkitsensor917.APIInterface
import com.siliconlabs.bledemo.features.demo.devkitsensor917.activities.DevKitSensor917Activity
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.AccelerometerGyroScopeResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.AmbientLightResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.HumidityResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.LEDResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.LEDStatusResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.SensorsResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.TempResponse
import kotlinx.android.synthetic.main.environmentdemo_tile.view.env_description
import kotlinx.android.synthetic.main.environmentdemo_tile.view.env_icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber


open class DevKitSensorControl(
    context: Context,
    description: String?,
    icon: Drawable?
) : LinearLayout(context, null, 0), DevKitSensor917Activity.ResponseListener {
    private var btnRedStatus = false
    private var btnGreenStatus = false
    private var btnBlueStatus = false
    private lateinit var ledImageStatus: ImageView
    private lateinit var onLEDBtn: Button
    private lateinit var offLEDBtn: Button
    private lateinit var redLEDBtn: Button
    private lateinit var greenLEDBtn: Button
    private lateinit var blueLEDBtn: Button

    private var tempResponse: String? = null
    private var ambiResponse: String? = null
    private var humiResponse: String? = null
    private var acceloResponse: AccelerometerGyroScopeResponse? = null
    private var gyroResponse: AccelerometerGyroScopeResponse? = null

    constructor(context: Context) : this(context, null, null)

    private val tileView: View = inflate(context, R.layout.sensor_demo_grid_item, this)

    fun setListener(tag: Any, ipAddress: String) {
        val code = tag.toString()
        tileView.setOnClickListener {
            when (code) {
                temperature -> showTemperatureDialog(code, ipAddress)
                humidity -> showHumidityDialog(code, ipAddress)
                ambientLight -> showAmbientLightDialog(code, ipAddress)
                microphone -> showMicrophoneDialog(code, ipAddress)
                LED -> showLEDControlDialog(code, ipAddress)
                motion -> showMotionDialog(code, ipAddress)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showMotionDialog(title: String, ipAddress: String) {
        var apiJob: Job? = null
        val devkitSensorDialog = Dialog(context, R.style.Style_Dialog_Rounded_Corner)
        devkitSensorDialog.setContentView(R.layout.dev_kit_sesnor_917_motion_control_dialog_layout)
        devkitSensorDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val header = devkitSensorDialog.findViewById(R.id.header) as TextView
        val yesBtn = devkitSensorDialog.findViewById(R.id.yes_opt) as TextView
        val noBtn = devkitSensorDialog.findViewById(R.id.no_opt) as TextView
        val orientationX = devkitSensorDialog.findViewById(R.id.orient_x) as TextView
        val orientationY = devkitSensorDialog.findViewById(R.id.orientation_y) as TextView
        val orientationZ = devkitSensorDialog.findViewById(R.id.orientation_z) as TextView
        val degreeString = context.getString(R.string.motion_orientation_degree)
        if (gyroResponse != null && gyroResponse!!.x.isNotEmpty()) {
            orientationX.text = String.format(degreeString, gyroResponse!!.x.toFloat())
        } else {
            orientationX.text = String.format(degreeString, 00f)
        }
        if (gyroResponse != null && gyroResponse!!.y.isNotEmpty()) {
            orientationX.text = String.format(degreeString, gyroResponse!!.y.toFloat())
        } else {
            orientationY.text = String.format(degreeString, 00f)
        }
        if (gyroResponse != null && gyroResponse!!.z.isNotEmpty()) {
            orientationX.text = String.format(degreeString, gyroResponse!!.z.toFloat())
        } else {
            orientationZ.text = String.format(degreeString, 00f)
        }

        val accelerationString = context.getString(R.string.motion_acceleration_g)
        val acceloX = devkitSensorDialog.findViewById(R.id.accelo_x) as TextView
        val acceloY = devkitSensorDialog.findViewById(R.id.accelo_y) as TextView
        val acceloZ = devkitSensorDialog.findViewById(R.id.accelo_z) as TextView
        if (acceloResponse != null && acceloResponse!!.x.isNotEmpty()) {
            acceloX.text = String.format(accelerationString, acceloResponse!!.x.toFloat())
        } else {
            acceloX.text = String.format(accelerationString, 00F)
        }
        if (acceloResponse != null && acceloResponse!!.y.isNotEmpty()) {
            acceloY.text = String.format(accelerationString, acceloResponse!!.y.toFloat())
        } else {
            acceloY.text = String.format(accelerationString, 00F)
        }
        if (acceloResponse != null && acceloResponse!!.z.isNotEmpty()) {
            acceloZ.text = String.format(accelerationString, acceloResponse!!.z.toFloat())
        } else {
            acceloZ.text = String.format(accelerationString, 00F)
        }

        yesBtn.setOnClickListener {
            yesBtn.isClickable = false
            yesBtn.isEnabled = false
            CoroutineScope(Dispatchers.IO).launch {
                doInMotionOrientationBackground(
                    orientationX,
                    orientationY,
                    orientationZ,
                    yesBtn,
                    ipAddress
                )
                doInMotionAccelerometerBackground(acceloX, acceloY, acceloZ, ipAddress)
            }
        }
        noBtn.setOnClickListener {
            devkitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        header.text = title + space + context.getString(R.string.title_sensor)
        devkitSensorDialog.show()
        apiJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                doInMotionOrientationBackground(
                    orientationX,
                    orientationY,
                    orientationZ,
                    yesBtn,
                    ipAddress
                )
                doInMotionAccelerometerBackground(acceloX, acceloY, acceloZ, ipAddress)
                delay(TIME_OUT)
            }
        }
    }


    private suspend fun doInMotionAccelerometerBackground(
        viewX: TextView,
        viewY: TextView,
        viewZ: TextView, ipAddress: String
    ) {
        withContext(Dispatchers.Default) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getAccelerometerStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    withContext(Dispatchers.Main) {
                        if (data != null) {
                            val accelerationString =
                                context.getString(R.string.motion_acceleration_g)
                            viewX.text = String.format(accelerationString, data.x.toFloat())
                            viewY.text = String.format(accelerationString, data.y.toFloat())
                            viewZ.text = String.format(accelerationString, data.z.toFloat())
                        }
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed: ")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred ${e.message}")
            }
        }
    }


    private suspend fun doInMotionOrientationBackground(
        viewX: TextView,
        viewY: TextView,
        viewZ: TextView, btn: TextView, ipAddress: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getGyroscopeStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    withContext(Dispatchers.Main) {
                        if (data != null) {
                            val degreeString = context.getString(R.string.motion_orientation_degree)
                            viewX.text = String.format(degreeString, data.x.toFloat())
                            viewY.text = String.format(degreeString, data.y.toFloat())
                            viewZ.text = String.format(degreeString, data.z.toFloat())
                        }
                        btn.isClickable = true
                        btn.isEnabled = true
                    }
                } else {
                    //df
                    Timber.tag(TAG)
                        .e("MotionOrientation API Response failed: ${response.message()}")
                    btn.isClickable = true
                    btn.isEnabled = true
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("MotionOrientation API Exception occurred ${e.message}")
                btn.isClickable = true
                btn.isEnabled = true
            } finally {
                btn.isClickable = true
                btn.isEnabled = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showTemperatureDialog(title: String, ipAddress: String) {
        var apiJob: Job? = null
        val devKitSensorDialog = Dialog(context, R.style.Style_Dialog_Rounded_Corner)
        devKitSensorDialog.setContentView(R.layout.dev_kit_sensor_917_dialog_layout)
        devKitSensorDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val header = devKitSensorDialog.findViewById(R.id.header) as TextView
        val subTitle = devKitSensorDialog.findViewById(R.id.subTitle) as TextView
        val yesBtn = devKitSensorDialog.findViewById(R.id.yes_opt) as TextView
        val noBtn = devKitSensorDialog.findViewById(R.id.no_opt) as TextView
        val image = devKitSensorDialog.findViewById(R.id.image) as ImageView
        val dataHolder = devKitSensorDialog.findViewById(R.id.txt_value) as TextView
        dataHolder.text = context.getString(R.string.matter_init_value)
        // println("tempResponse :${tempResponse!!.temperature_celcius}")
        if (tempResponse != null && tempResponse!!.isNotEmpty()) {
            dataHolder.text = tempResponse + " ℃"
        }
        image.setImageResource(R.drawable.icon_temp)
        header.text = title + space + context.getString(R.string.title_sensor)
        subTitle.text = title
        yesBtn.setOnClickListener {
            yesBtn.isEnabled = false
            yesBtn.isClickable = false
            CoroutineScope(Dispatchers.IO).launch {
                doInTempBackground(dataHolder, yesBtn, ipAddress)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                doInTempBackground(dataHolder, yesBtn, ipAddress)
                delay(TIME_OUT)
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private suspend fun doInAmbientBackground(view: TextView, btn: TextView, ipAddress: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getAmbitStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    withContext(Dispatchers.Main) {
                        if (data != null) {
                            view.text = data.ambient_light_lux + " lx"
                        }
                        btn.isClickable = true
                        btn.isEnabled = true
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("API Ambient Response failed: ${response.message()}")
                    btn.isClickable = true
                    btn.isEnabled = true
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("API Ambient Exception occurred :${e.message}")
                btn.isClickable = true
                btn.isEnabled = true
            } finally {
                btn.isClickable = true
                btn.isEnabled = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun doInMicroPhoneBackground(view: TextView, ipAddress: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getMicrophoneStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    withContext(Dispatchers.Main) {
                        if (data != null) {
                            view.text = data.microphone_decibel + " dB"
                        }
                    }
                } else {
                    Timber.tag(TAG).e("API Microphone Response failed:${response.message()}")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("API Microphone Exception occurred ${e.message}")
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private suspend fun doInHumidityBackground(view: TextView, btn: TextView, ipAddress: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getHumidityStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    withContext(Dispatchers.Main) {
                        if (data != null) {
                            view.text = data.humidity_percentage + " %"
                        }
                        btn.isEnabled = true
                        btn.isClickable = true
                    }
                } else {
                    btn.isEnabled = true
                    btn.isClickable = true
                    //df
                    Timber.tag(TAG).e("API Humidity Response failed:${response.message()}")
                }
            } catch (e: Exception) {
                btn.isEnabled = true
                btn.isClickable = true
                // Handle the exception
                Timber.tag(TAG).e("API Humidity Exception occurred ${e.message} ")
            } finally {
                btn.isEnabled = true
                btn.isClickable = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun doInTempBackground(view: TextView, btn: TextView, ipAddress: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getTempStatus()

                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    withContext(Dispatchers.Main) {
                        if (data != null) {
                            view.text = data.temperature_celcius + " ℃"
                        }
                        btn.isClickable = true
                        btn.isEnabled = true
                    }
                } else {
                    btn.isClickable = true
                    btn.isEnabled = true
                    Timber.tag(TAG).e("API Temperature Response failed:${response.message()}")
                }
            } catch (e: Exception) {
                btn.isClickable = true
                btn.isEnabled = true
                Timber.tag(TAG).e("API Temperature Exception occurred ${e.message}")
            } finally {
                btn.isClickable = true
                btn.isEnabled = true
            }
        }
    }

    init {
        tileView.setTag(description)
        tileView.apply {
            env_description.text = description
            env_icon.setImageDrawable(icon)
        }
        layoutParams = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMicrophoneDialog(title: String, ipAddress: String) {
        var apiJob: Job? = null
        val devKitSensorDialog = Dialog(context, R.style.Style_Dialog_Rounded_Corner)
        devKitSensorDialog.setContentView(R.layout.dev_kit_sensor_917_dialog_layout)
        devKitSensorDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val header = devKitSensorDialog.findViewById(R.id.header) as TextView
        val subTitle = devKitSensorDialog.findViewById(R.id.subTitle) as TextView
        val yesBtn = devKitSensorDialog.findViewById(R.id.yes_opt) as TextView
        val noBtn = devKitSensorDialog.findViewById(R.id.no_opt) as TextView
        val image = devKitSensorDialog.findViewById(R.id.image) as ImageView
        val microphoneHolder = devKitSensorDialog.findViewById(R.id.txt_value) as TextView
        microphoneHolder.text = context.getString(R.string.dev_kit_sensor_micro_init_value)
        image.setImageResource(R.drawable.icon_sound)
        header.text = title + space + context.getString(R.string.title_sensor)
        subTitle.text = title
        yesBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                doInMicroPhoneBackground(microphoneHolder, ipAddress)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                doInMicroPhoneBackground(microphoneHolder, ipAddress)
                delay(TIME_OUT)
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun showAmbientLightDialog(title: String, ipAddress: String) {
        var apiJob: Job? = null
        val devKitSensorDialog = Dialog(context, R.style.Style_Dialog_Rounded_Corner)
        devKitSensorDialog.setContentView(R.layout.dev_kit_sensor_917_dialog_layout)
        devKitSensorDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val header = devKitSensorDialog.findViewById(R.id.header) as TextView
        val subTitle = devKitSensorDialog.findViewById(R.id.subTitle) as TextView
        val yesBtn = devKitSensorDialog.findViewById(R.id.yes_opt) as TextView
        val noBtn = devKitSensorDialog.findViewById(R.id.no_opt) as TextView
        val image = devKitSensorDialog.findViewById(R.id.image) as ImageView
        val abiHolder = devKitSensorDialog.findViewById(R.id.txt_value) as TextView

        abiHolder.text = context.getString(R.string.dev_kit_sensor_ambient_init_value)
        if (ambiResponse != null && ambiResponse!!.isNotEmpty()) {
            abiHolder.text = ambiResponse + " lx"
        }
        abiHolder.textSize = 50F
        image.setImageResource(R.drawable.icon_light)
        header.text = title + space + context.getString(R.string.title_sensor)
        subTitle.text = title

        yesBtn.setOnClickListener {
            yesBtn.isEnabled = false
            yesBtn.isClickable = false
            CoroutineScope(Dispatchers.IO).launch {
                doInAmbientBackground(abiHolder, yesBtn, ipAddress)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                doInAmbientBackground(abiHolder, yesBtn, ipAddress)
                delay(TIME_OUT)
            }
        }

    }


    /* LED functionality Implementation */
    private fun imageForLightOn(view: ImageView, red: Boolean, blue: Boolean, green: Boolean) {

        view.setImageResource(R.drawable.light_on)
        var idColor = 0
        if (red && blue && green) {
            idColor = R.color.silabs_yellow
        } else if (red && blue) {
            idColor = R.color.silabs_magenta
        } else if (blue && green) {
            idColor = R.color.silabs_cyan_led
        } else if (green && red) {
            idColor = R.color.silabs_yellow_led
        } else if (blue) {
            idColor = R.color.silabs_blue
        } else if (red) {
            idColor = R.color.silabs_red
        } else if (green) {
            idColor = R.color.silabs_green
        } else {
            idColor = R.color.grey
        }
        view.setColorFilter(
            ContextCompat.getColor(context, idColor),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    @SuppressLint("SetTextI18n")
    private fun showLEDControlDialog(title: String, ipAddress: String) {
        val apiJob: Job? = null
        val devKitSensorDialog = Dialog(context, R.style.Style_Dialog_Rounded_Corner)
        devKitSensorDialog.setContentView(
            R.layout.dev_kit_sesnor_917_led_control_dialog_layout
        )
        devKitSensorDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val header = devKitSensorDialog.findViewById(R.id.header) as TextView
        val subTitle = devKitSensorDialog.findViewById(R.id.subTitle) as TextView
        val yesBtn = devKitSensorDialog.findViewById(R.id.yes_opt) as TextView
        val noBtn = devKitSensorDialog.findViewById(R.id.no_opt) as TextView
        onLEDBtn = devKitSensorDialog.findViewById(R.id.onButton) as Button
        offLEDBtn = devKitSensorDialog.findViewById(R.id.offButton) as Button
        redLEDBtn = devKitSensorDialog.findViewById(R.id.redButton) as Button
        greenLEDBtn = devKitSensorDialog.findViewById(R.id.greenButton) as Button
        blueLEDBtn = devKitSensorDialog.findViewById(R.id.blueButton) as Button
        ledImageStatus = devKitSensorDialog.findViewById(R.id.imageLight) as ImageView

        header.text = title + space + context.getString(R.string.title_control)
        subTitle.text = title
        subTitle.visibility = View.GONE

        //LED Switch Off condition
        imageForLightOn(view = ledImageStatus, red = false, blue = false, green = false)

        CoroutineScope(Dispatchers.IO).launch {
            getLedSwitchStatus(ipAddress)
        }

        yesBtn.setOnClickListener {
            refreshLEDStatus(ipAddress)
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob.cancel()
            }
        }
        onLEDBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                setAllLEDOn(ledImageStatus, ipAddress)
            }
        }

        offLEDBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                setAllLEDOff(ledImageStatus, ipAddress)
            }
        }

        redLEDBtn.setOnClickListener {
            btnRedStatus = !btnRedStatus
            setLEDControl(ipAddress)
        }
        greenLEDBtn.setOnClickListener {
            btnGreenStatus = !btnGreenStatus
            setLEDControl(ipAddress)
        }

        blueLEDBtn.setOnClickListener {
            btnBlueStatus = !btnBlueStatus
            setLEDControl(ipAddress)
        }

        devKitSensorDialog.setCanceledOnTouchOutside(false)
        devKitSensorDialog.show()
    }

    private fun switchOffLEDStatusOff(ipAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            setLEDStatusOff(ledImageStatus, ipAddress)
        }
    }

    private fun setOnBtnBackground() {
        onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
        offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        redLEDBtn.setBackgroundResource(R.drawable.button_background_red_box)
        greenLEDBtn.setBackgroundResource(R.drawable.button_background_green_box)
        blueLEDBtn.setBackgroundResource(R.drawable.button_background_blue_box)
    }


    private fun setOffBtnBackground() {
        onLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        offLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
        redLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        greenLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        blueLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
    }

    private fun refreshLEDStatus(ipAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            getAllLEDStatus(ledImageStatus, ipAddress)
        }
    }

    private fun setBtnColorBackground() {
        if (btnRedStatus) {
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            redLEDBtn.setBackgroundResource(R.drawable.button_background_red_box)
        } else {
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            redLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        }
        if (btnGreenStatus) {
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            greenLEDBtn.setBackgroundResource(R.drawable.button_background_green_box)
        } else {
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            greenLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        }
        if (btnBlueStatus) {
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            blueLEDBtn.setBackgroundResource(R.drawable.button_background_blue_box)
        } else {
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            blueLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        }
        if (btnRedStatus && btnGreenStatus && btnBlueStatus) {
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        } else if (btnRedStatus && btnGreenStatus) {
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            redLEDBtn.setBackgroundResource(R.drawable.button_background_red_box)
            greenLEDBtn.setBackgroundResource(R.drawable.button_background_green_box)
        } else if (btnRedStatus && btnBlueStatus) {
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            redLEDBtn.setBackgroundResource(R.drawable.button_background_red_box)
            blueLEDBtn.setBackgroundResource(R.drawable.button_background_blue_box)
        } else if (btnGreenStatus && btnBlueStatus) {
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            blueLEDBtn.setBackgroundResource(R.drawable.button_background_blue_box)
            greenLEDBtn.setBackgroundResource(R.drawable.button_background_green_box)
        } else if (!btnRedStatus && !btnGreenStatus && !btnBlueStatus) {
            onLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            offLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
        } else {
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
        }

    }

    private fun setLEDControl(ipAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            setLEDCtrl(
                ledImageStatus,
                btnRedStatus,
                btnGreenStatus,
                btnBlueStatus, ipAddress
            )
        }
    }

    private suspend fun setLEDCtrl(
        view: ImageView, red: Boolean, green: Boolean, blue: Boolean,
        ipAddress: String
    ) {
        withContext(Dispatchers.Default) {

            val url = "http://$ipAddress"
            val retro = Retrofit.Builder().baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create()).build()
            val ledStatus = retro.create(APIInterface::class.java)
            val params: MutableMap<String, String> = HashMap<String, String>()
            var statusRed: String = OFF
            if (red) {
                statusRed = ON
            }
            params.put(RED, statusRed)
            var statusGreen = OFF
            if (green) {
                statusGreen = ON
            }
            params.put(GREEN, statusGreen)
            var statusBlue = OFF
            if (blue) {
                statusBlue = ON
            }
            params.put(BLUE, statusBlue)

            ledStatus.setRedLEDOn(params).enqueue(object : Callback<LEDResponse> {
                override fun onResponse(call: Call<LEDResponse>, response: Response<LEDResponse>) {
                    println("Response: $response")
                    if (response.isSuccessful) {
                        val data = response.body()
                        println("data: $data")

                        if (data != null) {
                            if (data.red.equals(OFF) && data.green.equals(OFF)
                                && data.blue.equals(OFF)
                            ) {
                                imageForLightOn(view, false, false, false)
                            } else {
                                imageForLightOn(view, red, blue, green)
                            }
                            setBtnColorBackground()
                        }
                    } else {

                    }
                }

                override fun onFailure(call: Call<LEDResponse>, t: Throwable) {
                    btnRedStatus = false
                    btnBlueStatus = false
                    btnGreenStatus = false
                    setOffBtnBackground()
                    imageForLightOn(view, false, false, false)
                    Toast.makeText(
                        context,
                        "Failed to connect. Check your Wi-Fi connection",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
        }
    }

    private suspend fun setAllLEDOff(view: ImageView, ipAddress: String) {
        withContext(Dispatchers.IO) {
            val url = "http://$ipAddress"
            val retro = Retrofit.Builder().baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create()).build()
            val ledStatus = retro.create(APIInterface::class.java)
            val body = mapOf(
                RED to OFF,
                GREEN to OFF,
                BLUE to OFF
            )
            ledStatus.setAllLEdsOnOff(body).enqueue(object : Callback<LEDResponse> {

                override fun onResponse(call: Call<LEDResponse>, response: Response<LEDResponse>) {
                    println("Response: $response")
                    if (response.isSuccessful) {
                        val data = response.body()
                        println("data: $data")
                        if (data != null) {
                            if (data.red.equals(OFF) && data.green.equals(OFF)
                                && data.blue.equals(OFF)
                            ) {
                                btnRedStatus = false
                                btnBlueStatus = false
                                btnGreenStatus = false

                                setOffBtnBackground()
                                imageForLightOn(view, btnRedStatus, btnBlueStatus, btnGreenStatus)
                            } else {
                                imageForLightOn(view, false, false, false)
                                setOffBtnBackground()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<LEDResponse>, t: Throwable) {
                    btnRedStatus = false
                    btnBlueStatus = false
                    btnGreenStatus = false
                    setOffBtnBackground()
                    imageForLightOn(view, false, false, false)
                    Toast.makeText(
                        context,
                        "Failed to connect. Check your Wi-Fi connection",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

        }
    }

    private suspend fun setAllLEDOn(view: ImageView, ipAddress: String) {
        withContext(Dispatchers.IO) {
            val url = "http://$ipAddress"
            val retro = Retrofit.Builder().baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create()).build()
            val ledStatus = retro.create(APIInterface::class.java)
            val body = mapOf(
                RED to ON,
                GREEN to ON,
                BLUE to ON
            )
            ledStatus.setAllLEdsOnOff(body).enqueue(object : Callback<LEDResponse> {
                override fun onResponse(call: Call<LEDResponse>, response: Response<LEDResponse>) {
                    println("Response: $response")
                    if (response.isSuccessful) {
                        val data = response.body()
                        println("data: $data")
                        if (data != null) {
                            if (data.red.equals(OFF) && data.green.equals(OFF)
                                && data.blue.equals(OFF)
                            ) {
                                btnRedStatus = false
                                btnBlueStatus = false
                                btnGreenStatus = false
                                setOffBtnBackground()
                                imageForLightOn(
                                    view,
                                    btnRedStatus,
                                    btnBlueStatus,
                                    btnGreenStatus
                                )
                            } else {
                                btnRedStatus = true
                                btnBlueStatus = true
                                btnGreenStatus = true
                                setOnBtnBackground()
                                imageForLightOn(
                                    view,
                                    btnRedStatus,
                                    btnBlueStatus,
                                    btnGreenStatus
                                )
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<LEDResponse>, t: Throwable) {
                    btnRedStatus = false
                    btnBlueStatus = false
                    btnGreenStatus = false
                    setOffBtnBackground()
                    imageForLightOn(view, false, false, false)
                    Toast.makeText(
                        context,
                        "Failed to connect. Check your Wi-Fi connection",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
        }
    }

    private suspend fun getLedSwitchStatus(ipAddress: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getLedSwitchStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        if (data.status_led == ON)
                            switchOffLEDStatusOff(ipAddress)
                        else
                            refreshLEDStatus(ipAddress)
                    }
                } else {
                    Timber.tag(TAG).e("getLedSwitch Response failed: ${response.message()}")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("getLedSwitch Exception occurred ${e.message}")
            }
        }
    }

    private suspend fun setLEDStatusOff(view: ImageView, ipAddress: String) {
        withContext(Dispatchers.Default) {
            val url = "http://$ipAddress"
            val retro = Retrofit.Builder().baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create()).build()
            val ledStatus = retro.create(APIInterface::class.java)
            val body = mapOf(
                STATUS_LED to OFF
            )
            ledStatus.setLEDStatusOff(body).enqueue(object : Callback<LEDStatusResponse> {
                override fun onResponse(
                    call: Call<LEDStatusResponse>,
                    response: Response<LEDStatusResponse>
                ) {
                    println("Response: $response")
                    if (response.isSuccessful) {
                        val data = response.body()
                        println("data: $data")
                        if (data != null) {
                            //refresh the current LED status
                            refreshLEDStatus(ipAddress)
                        }
                    }
                }

                override fun onFailure(call: Call<LEDStatusResponse>, t: Throwable) {
                    btnRedStatus = false
                    btnBlueStatus = false
                    btnGreenStatus = false
                    setOffBtnBackground()
                    imageForLightOn(view, false, false, false)
                    Toast.makeText(
                        context,
                        "Failed to connect. Check your Wi-Fi connection",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
        }
    }

    private suspend fun getAllLEDStatus(view: ImageView, ipAddress: String) {
        withContext(Dispatchers.Default) {
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getLedStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        withContext(Dispatchers.Main) {
                            btnRedStatus = data.red == ON
                            btnGreenStatus = data.green == ON
                            btnBlueStatus = data.blue == ON
                            imageForLightOn(view, btnRedStatus, btnBlueStatus, btnGreenStatus)
                            setBtnColorBackground()
                        }
                    }
                } else {
                    Timber.tag(TAG).e("getAllLEDStatus Response failed: ${response.message()}")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("getAllLEDStatus Exception occurred: ${e.message}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showHumidityDialog(title: String, ipAddress: String) {
        var apiJob: Job? = null
        val devKitSensorDialog = Dialog(context, R.style.Style_Dialog_Rounded_Corner)
        devKitSensorDialog.setContentView(
            R.layout.dev_kit_sensor_917_dialog_layout
        )
        devKitSensorDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val header = devKitSensorDialog.findViewById(R.id.header) as TextView
        val subTitle = devKitSensorDialog.findViewById(R.id.subTitle) as TextView
        val yesBtn = devKitSensorDialog.findViewById(R.id.yes_opt) as TextView
        val noBtn = devKitSensorDialog.findViewById(R.id.no_opt) as TextView
        val image = devKitSensorDialog.findViewById(R.id.image) as ImageView
        val humidityHolder = devKitSensorDialog.findViewById(R.id.txt_value) as TextView
        humidityHolder.text = context.getString(R.string.dev_kit_sensor_humidity_init_value)
        if (humiResponse != null && humiResponse!!.isNotEmpty()) {
            humidityHolder.text = humiResponse + " %"
        }
        image.setImageResource(R.drawable.icon_environment)
        header.text = title + space + context.getString(R.string.title_sensor)
        subTitle.text = title
        yesBtn.setOnClickListener {
            yesBtn.isEnabled = false
            yesBtn.isClickable = false
            CoroutineScope(Dispatchers.IO).launch {
                doInHumidityBackground(humidityHolder, yesBtn, ipAddress)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                doInHumidityBackground(humidityHolder, yesBtn, ipAddress)
                delay(TIME_OUT)
            }
        }

    }


    override fun fetchData(response: SensorsResponse?) {
        //temperatureResponse = tempResponse
        println("----------------${response!!.led.blue}")
        if (response != null) {
            //Temp
            if (response.temperature != null && response.temperature.temperature_celcius.isNotEmpty()) {
                println("----------------${response.temperature.temperature_celcius}")
                tempResponse = response.temperature.temperature_celcius
            }
            //Humi
            if (response.humidity != null && response.humidity.humidity_percentage.isNotEmpty()) {
                println("----------------${response.humidity.humidity_percentage}")
                humiResponse = response.temperature.temperature_celcius
            }
            //Ambi
            if (response.light != null && response.light.ambient_light_lux.isNotEmpty()) {
                println("----------------${response.light.ambient_light_lux}")
                ambiResponse = response.light.ambient_light_lux
            }
        }

    }

    companion object {
        const val space = " "
        const val temperature = "Temperature"
        const val humidity = "Humidity"
        const val ambientLight = "Ambient Light"
        const val microphone = "Microphone"
        const val LED = "LED"
        const val motion = "Motion"
        const val TIME_OUT = 2000L
        const val OFF = "off"
        const val ON = "on"
        const val RED = "red"
        const val GREEN = "green"
        const val BLUE = "blue"
        const val STATUS_LED = "status_led"
        private val TAG = Companion::class.java.simpleName.toString()
    }


}