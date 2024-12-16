package com.siliconlabs.bledemo.features.demo.blinky.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentBlinkyBinding
import com.siliconlabs.bledemo.features.demo.blinky.models.LightState
import com.siliconlabs.bledemo.features.demo.blinky.viewmodels.BlinkyViewModel

//import kotlinx.android.synthetic.main.fragment_blinky.*

class BlinkyFragment : Fragment(R.layout.fragment_blinky) {
    private lateinit var viewModel: BlinkyViewModel
    private val layout by viewBinding(FragmentBlinkyBinding::bind)


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
         layout.ivLightBulb.setImageResource(R.drawable.light_on)
    }

    private fun switchLightOff() {
        layout.ivLightBulb.setImageResource(R.drawable.light_off)
    }

    private fun setButtonImage(isPressed: Boolean) {

        if (isPressed) {
            layout.ivButton.setImageResource(R.drawable.ic_button_on)
        } else {
            layout.ivButton.setImageResource(R.drawable.ic_button_off)
        }
    }

    private fun handleClickEvents() {
        layout.ivLightBulb.setOnClickListener {
            viewModel.changeLightState()
        }
    }
}