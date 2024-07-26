package com.siliconlabs.bledemo.features.demo.devkitsensor917.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.Activity917DevKitSensorLayoutBinding
import com.siliconlabs.bledemo.features.demo.devkitsensor917.utils.DevKitSensorChecker
import com.siliconlabs.bledemo.features.demo.devkitsensor917.utils.DevKitSensorControl

class DevKitSensor917Activity : AppCompatActivity() {

    private lateinit var binding: Activity917DevKitSensorLayoutBinding
    private var currentFragment: Fragment? = null
    private val devKitSensorChecker = DevKitSensorChecker()
    private val controls =
        mutableMapOf<DevKitSensorChecker.DevkitSensor917BoardSensor, DevKitSensorControl>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = Activity917DevKitSensorLayoutBinding.inflate(layoutInflater)
        setSupportActionBar(binding.toolbar)
        setContentView(binding.root)
        val actionBar = supportActionBar
        actionBar!!.setHomeAsUpIndicator(R.drawable.matter_back)
        actionBar.setDisplayHomeAsUpEnabled(true)
        initGrid()
    }
//
//    override fun onBackPressed() {
//        if (getFragmentManager().getBackStackEntryCount() > 0)
//            getFragmentManager().popBackStack();
//        else
//            super.onBackPressed();
//    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initGrid() {
        binding.envGrid.apply {
            devKitSensorChecker.devKitSensors.filter {
                it.value == DevKitSensorChecker.DevkitSensorState.WORKING
            }.forEach {
                    controls[it.key] = DevKitSensorControl(
                    this.context,
                    getString(getTileDescription(it.key)),
                    ContextCompat.getDrawable(this.context, getTileIcon(it.key))
                ).also {

                    addView(it)
                    it.setListener(it.tag)

                }
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


}

