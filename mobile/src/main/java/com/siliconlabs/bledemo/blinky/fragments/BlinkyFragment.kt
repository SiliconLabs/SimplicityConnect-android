package com.siliconlabs.bledemo.blinky.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.blinky.models.LightState
import com.siliconlabs.bledemo.blinky.viewmodels.BlinkyViewModel
import kotlinx.android.synthetic.main.fragment_blinky.*

class BlinkyFragment : Fragment(R.layout.fragment_blinky) {
    private lateinit var viewModel: BlinkyViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(BlinkyViewModel::class.java)

        observeChanges()
        handleClickEvents()
    }

    private fun observeChanges() {
        viewModel.isButtonPressed.observe(viewLifecycleOwner, Observer { isPressed ->
            setButtonImage(isPressed)
        })

        viewModel.lightState.observe(viewLifecycleOwner, Observer {
            when (it) {
                LightState.ON -> switchLightOn()
                else -> switchLightOff()
            }
        })
    }

    private fun switchLightOn() {
        iv_light_bulb.setImageResource(R.drawable.light_on)
    }

    private fun switchLightOff() {
        iv_light_bulb.setImageResource(R.drawable.light_off)
    }

    private fun setButtonImage(isPressed: Boolean) {
        if (isPressed) {
            iv_button.setImageResource(R.drawable.ic_button_on)
        } else {
            iv_button.setImageResource(R.drawable.ic_button_off)
        }
    }

    private fun handleClickEvents() {
        iv_light_bulb.setOnClickListener {
            viewModel.changeLightState()
        }
    }
}