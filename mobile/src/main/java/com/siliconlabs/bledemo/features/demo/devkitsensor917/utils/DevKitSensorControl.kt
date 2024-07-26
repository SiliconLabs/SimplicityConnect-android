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
import com.siliconlabs.bledemo.features.demo.devkitsensor917.APIClient
import com.siliconlabs.bledemo.features.demo.devkitsensor917.APIInterface
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.LEDResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.LEDStatusResponse
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.model.TemperatureScale
import kotlinx.android.synthetic.main.environmentdemo_tile.view.env_description
import kotlinx.android.synthetic.main.environmentdemo_tile.view.env_icon
import kotlinx.android.synthetic.main.environmentdemo_tile.view.env_value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

open class DevKitSensorControl(
    context: Context,
    description: String?,
    icon: Drawable?
) : LinearLayout(context, null, 0) {
    private var btnRedStatus = false
    private var btnGreenStatus = false
    private var btnBlueStatus = false
    private lateinit var ledImageStatus: ImageView
    private lateinit var onLEDBtn : Button
    private lateinit var offLEDBtn : Button
    private lateinit var redLEDBtn : Button
    private lateinit var greenLEDBtn : Button
    private lateinit var blueLEDBtn  : Button
    constructor(context: Context) : this(context, null, null)

    private val tileView: View = inflate(context, R.layout.sensor_demo_grid_item,this)

    fun setListener(tag: Any) {
        val code = tag.toString()
        tileView.setOnClickListener {
            when (code) {
                temperature -> showTemperatureDialog(code)
                humidity -> showHumidityDialog(code)
                ambientLight -> showAmbientLightDialog(code)
                microphone -> showMicrophoneDialog(code)
                LED -> showLEDControlDialog(code)
                motion -> showMotionDialog(code)
            }


        }
    }

    fun setTemperature(temperature: Float, temperatureType: Int) {
        tileView.env_value.text = String.format(
            if (temperatureType == TemperatureScale.FAHRENHEIT) context.getString(R.string.environment_temp_f) else context.getString(
                R.string.environment_temp_c
            ),
            if (temperatureType == TemperatureScale.FAHRENHEIT) temperature * 1.8f + 32f else temperature
        )
    }

    fun setHumidity(humidity: Int) {
        tileView.env_value.text =
            String.format(context.getString(R.string.environment_humidity_measure), humidity)
    }

    fun setAmbientLight(ambientLight: Float) {
        tileView.env_value.text =
            String.format(context.getString(R.string.devkit_ambient_lx), ambientLight)
    }

    fun setMicrophone(microphone: Float) {
        tileView.env_value.text =
            String.format(context.getString(R.string.devkit_ambient_lx), microphone)
    }

    private fun showMotionDialog(title: String) {
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
        val acceloX = devkitSensorDialog.findViewById(R.id.accelo_x) as TextView
        val acceloY = devkitSensorDialog.findViewById(R.id.accelo_y) as TextView
        val acceloZ = devkitSensorDialog.findViewById(R.id.accelo_z) as TextView
        orientationX.text = String.format(degreeString, 00f)
        orientationY.text = String.format(degreeString, 00f)
        orientationZ.text = String.format(degreeString, 00f)

        val accelerationString = context.getString(R.string.motion_acceleration_g)
        acceloX.text = String.format(accelerationString, 00F)
        acceloY.text = String.format(accelerationString, 00F)
        acceloZ.text = String.format(accelerationString, 00F)

        yesBtn.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                doInMotionOrientationBackground(orientationX, orientationY, orientationZ)
                doInMotionAccelometerBackground(acceloX, acceloY, acceloZ)
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
        apiJob = GlobalScope.launch {
            while (true) {
                doInMotionOrientationBackground(orientationX, orientationY, orientationZ)
                doInMotionAccelometerBackground(acceloX, acceloY, acceloZ)
                delay(TIME_OUT)
            }
        }
    }

    private suspend fun doInMotionAccelometerBackground(
        viewX: TextView,
        viewY: TextView,
        viewZ: TextView
    ) {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getAccelerometerStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        val accelerationString = context.getString(R.string.motion_acceleration_g)
                        viewX.text = String.format(accelerationString, data.x.toFloat())
                        viewY.text = String.format(accelerationString, data.y.toFloat())
                        viewZ.text = String.format(accelerationString, data.z.toFloat())
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred" + e.message)
            }
        }
    }


    private suspend fun doInMotionOrientationBackground(
        viewX: TextView,
        viewY: TextView,
        viewZ: TextView
    ) {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getGyroscopeStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        val degreeString = context.getString(R.string.motion_orientation_degree)
                        viewX.text = String.format(degreeString, data.x.toFloat())
                        viewY.text = String.format(degreeString, data.y.toFloat())
                        viewZ.text = String.format(degreeString, data.z.toFloat())
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred" + e.message)
            }
        }
    }

    private fun showTemperatureDialog(title: String) {
        var apiJob: Job? = null
        val devKitSensorDialog = Dialog(context, R.style.Style_Dialog_Rounded_Corner)
        devKitSensorDialog.setContentView(R.layout.dev_kit_sensor_917_dialog_layout)
        devKitSensorDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        //devKitSensorDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        val header = devKitSensorDialog.findViewById(R.id.header) as TextView
        val subTitle = devKitSensorDialog.findViewById(R.id.subTitle) as TextView
        val yesBtn = devKitSensorDialog.findViewById(R.id.yes_opt) as TextView
        val noBtn = devKitSensorDialog.findViewById(R.id.no_opt) as TextView
        val image = devKitSensorDialog.findViewById(R.id.image) as ImageView
        val dataHolder = devKitSensorDialog.findViewById(R.id.txt_value) as TextView
        dataHolder.text = context.getString(R.string.matter_init_value)
        image.setImageResource(R.drawable.icon_temp)
        header.text = title + space + context.getString(R.string.title_sensor)
        subTitle.text = title
        yesBtn.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                doInTempBackground(dataHolder)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = GlobalScope.launch {
            while (true) {
                doInTempBackground(dataHolder)
                delay(TIME_OUT)
            }
        }

    }

    private suspend fun doInAmbientBackground(view: TextView) {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getAmbitStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        view.text = data.ambient_light_lux + " lx"
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun doInMicroPhoneBackground(view: TextView) {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getMicrophoneStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        view.text = data.microphone_decibel + " dB"
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred")
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private suspend fun doInHumidityBackground(view: TextView) {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getHumidityStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        view.text = data.humidity_percentage + " %"
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun doInTempBackground(view: TextView) {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getTempStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        view.text = data.temperature_celcius + " ℃"
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred")
            }
        }
    }

    init {
        tileView.setTag(description)
        tileView.apply {
            env_description.text = description
//            env_value.text = context.getString(R.string.environment_not_initialized)
            env_icon.setImageDrawable(icon)
//            env_value.visibility = View.INVISIBLE
        }
        layoutParams = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
        }
    }

    private fun showMicrophoneDialog(title: String) {
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
            GlobalScope.launch(Dispatchers.IO) {
                doInMicroPhoneBackground(microphoneHolder)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = GlobalScope.launch {
            while (true) {
                doInMicroPhoneBackground(microphoneHolder)
                delay(TIME_OUT)
            }
        }

    }

    private fun showAmbientLightDialog(title: String) {
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
        abiHolder.textSize = 50F
        image.setImageResource(R.drawable.icon_light)
        header.text = title + space + context.getString(R.string.title_sensor)
        subTitle.text = title

        yesBtn.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                doInAmbientBackground(abiHolder)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = GlobalScope.launch {
            while (true) {
                doInAmbientBackground(abiHolder)
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

    private fun showLEDControlDialog(title: String) {
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
//        switchOffLEDStatusOff()
        GlobalScope.launch {
            getLedSwitchStatus()
        }

        yesBtn.setOnClickListener {
            refreshLEDStatus()
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob.cancel()
            }
        }
        onLEDBtn.setOnClickListener {
            GlobalScope.launch {
                setAllLEDOn(ledImageStatus)
            }
        }

        offLEDBtn.setOnClickListener {
            GlobalScope.launch {
                setAllLEDOff(ledImageStatus)
            }
        }

        redLEDBtn.setOnClickListener {
            btnRedStatus = !btnRedStatus
            setLEDControl()
        }
        greenLEDBtn.setOnClickListener {
            btnGreenStatus = !btnGreenStatus
            setLEDControl()
        }

        blueLEDBtn.setOnClickListener {
            btnBlueStatus = !btnBlueStatus
            setLEDControl()
        }

        devKitSensorDialog.setCanceledOnTouchOutside(false)
        devKitSensorDialog.show()
    }

    private fun switchOffLEDStatusOff() {
        GlobalScope.launch {
            setLEDStatusOff(ledImageStatus)
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

    private fun refreshLEDStatus() {
        GlobalScope.launch(Dispatchers.IO) {
            getAllLEDStatus(ledImageStatus)
        }
    }

    private fun setBtnColorBackground() {
        if(btnRedStatus)
            redLEDBtn.setBackgroundResource(R.drawable.button_background_red_box)
        else
            redLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)

        if(btnGreenStatus)
            greenLEDBtn.setBackgroundResource(R.drawable.button_background_green_box)
        else
            greenLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)

        if(btnBlueStatus)
            blueLEDBtn.setBackgroundResource(R.drawable.button_background_blue_box)
        else
            blueLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)

        if(btnRedStatus && btnGreenStatus && btnBlueStatus) {
            onLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
            offLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
        } else {
            onLEDBtn.setBackgroundResource(R.drawable.button_background_grey_box)
            offLEDBtn.setBackgroundResource(R.drawable.button_background_soft_black_box)
        }

    }

    private fun setLEDControl(){
        GlobalScope.launch {
            setLEDCtrl(
                ledImageStatus,
                btnRedStatus,
                btnGreenStatus,
                btnBlueStatus
            )
        }
    }

    private suspend fun setLEDCtrl(view: ImageView, red: Boolean, green: Boolean, blue: Boolean) {
        withContext(Dispatchers.Default) {
            val ledStatus = APIClient.getInstance().create(APIInterface::class.java)
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

    private suspend fun setAllLEDOff(view: ImageView) {
        withContext(Dispatchers.Default) {
            val ledStatus = APIClient.getInstance().create(APIInterface::class.java)
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

    private suspend fun setAllLEDOn(view: ImageView) {
        withContext(Dispatchers.Default) {
            val ledStatus = APIClient.getInstance().create(APIInterface::class.java)
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
                                imageForLightOn(view, btnRedStatus, btnBlueStatus, btnGreenStatus)
                            } else {
                                btnRedStatus = true
                                btnBlueStatus = true
                                btnGreenStatus = true
                                setOnBtnBackground()
                                imageForLightOn(view, btnRedStatus, btnBlueStatus, btnGreenStatus)
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

    private suspend fun getLedSwitchStatus() {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getLedSwitchStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        if(data.status_led == ON)
                            switchOffLEDStatusOff()
                        else
                            refreshLEDStatus()
                    }
                } else {
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred")
            }
        }
    }
    private suspend fun setLEDStatusOff(view: ImageView) {
        withContext(Dispatchers.Default) {
            val ledStatus = APIClient.getInstance().create(APIInterface::class.java)
            val body = mapOf(
                STATUS_LED to OFF
            )
            ledStatus.setLEDStatusOff(body).enqueue(object : Callback<LEDStatusResponse> {
                override fun onResponse(call: Call<LEDStatusResponse>, response: Response<LEDStatusResponse>) {
                    println("Response: $response")
                    if (response.isSuccessful) {
                        val data = response.body()
                        println("data: $data")
                        if (data != null) {
                            //refresh the current LED status
                            refreshLEDStatus()
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
    private suspend fun getAllLEDStatus(view: ImageView) {
        withContext(Dispatchers.Default) {
            try {
                val apiService = APIClient.getInstance().create(APIInterface::class.java)
                val response = apiService.getLedStatus()
                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    if (data != null) {
                        btnRedStatus = data.red == ON
                        btnGreenStatus = data.green == ON
                        btnBlueStatus = data.blue == ON
                        imageForLightOn(view, btnRedStatus, btnBlueStatus, btnGreenStatus)
                        setBtnColorBackground()
                    }
                } else {
                    //df
                    Timber.tag(TAG).e("Response failed:")
                }
            } catch (e: Exception) {
                // Handle the exception
                Timber.tag(TAG).e("Exception occurred")
            }
        }
    }

    private fun showHumidityDialog(title: String) {
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
        image.setImageResource(R.drawable.icon_environment)
        header.text = title + space + context.getString(R.string.title_sensor)
        subTitle.text = title
        yesBtn.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                doInHumidityBackground(humidityHolder)
            }
        }
        noBtn.setOnClickListener {
            devKitSensorDialog.dismiss()
            if (apiJob != null) {
                apiJob?.cancel()
            }
        }
        devKitSensorDialog.show()

        apiJob = GlobalScope.launch {
            while (true) {
                doInHumidityBackground(humidityHolder)
                delay(TIME_OUT)
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
        const val TIME_OUT = 3000L
        const val OFF = "off"
        const val ON = "on"
        const val RED = "red"
        const val GREEN = "green"
        const val BLUE = "blue"
        const val STATUS_LED = "status_led"
        private val TAG = DevKitSensorControl.javaClass.simpleName.toString()
    }
}