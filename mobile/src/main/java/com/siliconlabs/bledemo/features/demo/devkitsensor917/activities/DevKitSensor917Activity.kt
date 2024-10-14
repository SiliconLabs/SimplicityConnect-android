package com.siliconlabs.bledemo.features.demo.devkitsensor917.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.Activity917DevKitSensorLayoutBinding
import com.siliconlabs.bledemo.features.demo.devkitsensor917.APIInterface
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.ScanResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.model.SensorsResponse
import com.siliconlabs.bledemo.features.demo.devkitsensor917.utils.DevKitSensorChecker
import com.siliconlabs.bledemo.features.demo.devkitsensor917.utils.DevKitSensorControl
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DevKitSensor917Activity : AppCompatActivity() {

    private lateinit var binding: Activity917DevKitSensorLayoutBinding
    private var currentFragment: Fragment? = null
    private val devKitSensorChecker = DevKitSensorChecker()
    private val controls =
        mutableMapOf<DevKitSensorChecker.DevkitSensor917BoardSensor, DevKitSensorControl>()
    private lateinit var devkitSensorCntl: DevKitSensorControl
    private lateinit var listener: ResponseListener
    private var customProgressDialog: CustomProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = Activity917DevKitSensorLayoutBinding.inflate(layoutInflater)
        setSupportActionBar(binding.toolbar)
        setContentView(binding.root)
        val actionBar = supportActionBar
        actionBar!!.setHomeAsUpIndicator(R.drawable.matter_back)
        actionBar.setDisplayHomeAsUpEnabled(true)
        val ipAddress = intent.getStringExtra(IP_ADDRESS)
        if (ipAddress != null) {
            showProgressDialog(this.getString(R.string.dev_kit_progress_bar_message))
            println("IP ADDRESS Imported: $ipAddress")
            initGrid(ipAddress)
            GlobalScope.launch {
                doInSensorBackground(ipAddress)
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private suspend fun doInSensorBackground(ipAddress: String) {
        withContext(Dispatchers.IO) {
            delay(3000L)
            try {
                val url = "http://$ipAddress"
                val retro = Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create()).build()
                val response = retro.create(APIInterface::class.java).getAllSensor()

                println("Response: $response")
                if (response.isSuccessful) {
                    val data = response.body()
                    println("data: $data")
                    withContext(Dispatchers.Main) {
                        if (data != null) {
                            listener.fetchData(data)
                            removeProgress()
                        }
                    }
                } else {
                    removeProgress()
                    runOnUiThread {
                        Toast.makeText(
                             baseContext,
                            "API All Sensors Response failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    Timber.tag(TAG).e("API All Sensors Response failed:${response.message()}")
                }
            } catch (e: Exception) {
                removeProgress()
                Timber.tag(TAG).e("API All Sensors Exception occurred ${e.message}")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initGrid(ipAddress: String) {
        binding.envGrid.apply {
            devKitSensorChecker.devKitSensors.filter {
                it.value == DevKitSensorChecker.DevkitSensorState.WORKING
            }.forEach {
                devkitSensorCntl = DevKitSensorControl(
                    this.context,
                    getString(getTileDescription(it.key)),
                    ContextCompat.getDrawable(this.context, getTileIcon(it.key))
                ).also {

                    addView(it)
                    it.setListener(it.tag, ipAddress)


                }
                listener = devkitSensorCntl
                controls[it.key] = devkitSensorCntl
            }
        }
    }


    @StringRes
    private fun getTileDescription(sensor: DevKitSensorChecker.DevkitSensor917BoardSensor): Int {
        return when (sensor) {
            DevKitSensorChecker.DevkitSensor917BoardSensor.Temperature -> R.string.environment_temp
            DevKitSensorChecker.DevkitSensor917BoardSensor.Humidity -> R.string.environment_humidity
            DevKitSensorChecker.DevkitSensor917BoardSensor.AmbientLight -> R.string.environment_ambient
            DevKitSensorChecker.DevkitSensor917BoardSensor.Motion -> R.string.dev_kit_sensor_917_motion
            DevKitSensorChecker.DevkitSensor917BoardSensor.LED -> R.string.dev_kit_sensor_917_led
//            DevKitSensorChecker.DevkitSensor917BoardSensor.Microphone -> R.string.dev_kit_sensor_917_microphone

            else -> 0
        }
    }

    @DrawableRes
    private fun getTileIcon(sensor: DevKitSensorChecker.DevkitSensor917BoardSensor): Int {
        return when (sensor) {
            DevKitSensorChecker.DevkitSensor917BoardSensor.Temperature -> R.drawable.icon_temp
            DevKitSensorChecker.DevkitSensor917BoardSensor.Humidity -> R.drawable.icon_environment
            DevKitSensorChecker.DevkitSensor917BoardSensor.AmbientLight -> R.drawable.icon_light
            DevKitSensorChecker.DevkitSensor917BoardSensor.Motion -> R.drawable.icon_dks_917_motion
            DevKitSensorChecker.DevkitSensor917BoardSensor.LED -> R.drawable.icon_dks_917_led
//            DevKitSensorChecker.DevkitSensor917BoardSensor.Microphone -> R.drawable.icon_sound

            else -> 0
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

    interface ResponseListener {
        fun fetchData(response: SensorsResponse?)
    }

    companion object {
        const val IP_ADDRESS = "ip_address"
        val TAG = ""
    }
}

