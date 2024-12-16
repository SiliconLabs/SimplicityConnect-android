package com.siliconlabs.bledemo.features.demo.connected_lighting.fragments

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.features.demo.connected_lighting.activities.ConnectedLightingActivity
import com.siliconlabs.bledemo.features.demo.connected_lighting.presenters.ConnectedLightingPresenter
import com.siliconlabs.bledemo.features.demo.connected_lighting.models.TriggerSource
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentLightBinding

//import kotlinx.android.synthetic.main.fragment_light.*

class ConnectedLightingFragment : Fragment(), ConnectedLightingPresenter.View {
    lateinit var presenter: ConnectedLightingPresenter
    private lateinit var binding: FragmentLightBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = ConnectedLightingPresenter(this, activity as ConnectedLightingActivity)

        binding.btnLightbulb.setOnClickListener {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            presenter.onLightClicked()
        }
    }

    override fun showLightState(lightOn: Boolean) {
        if (lightOn) {
            binding.btnLightbulb.setImageResource(R.drawable.light_on)
            binding.tvLightStatus.setText(R.string.light_demo_on)
        } else {
            binding.btnLightbulb.setImageResource(R.drawable.light_off)
            binding.tvLightStatus.setText(R.string.light_demo_off)
        }
    }

    override fun showTriggerSourceDetails(source: TriggerSource?) {

        if (isAdded && !isDetached && !isRemoving) {
            if (source?.iconId == android.R.color.transparent) {
                binding.ivLightChangeSourceLogo.visibility = View.GONE
            } else {
                binding.ivLightChangeSourceLogo.setImageResource(source?.iconId!!)
                binding.ivLightChangeSourceLogo.visibility = View.VISIBLE
            }

            binding.tvLightChangeSourceName.text = getText(source.textResId)
        }
    }

    override fun showTriggerSourceAddress(sourceAddress: String, source: TriggerSource?) {
        binding.tvLightChangeSource
        if (TriggerSource.UNKNOWN == source) {
             binding.tvLightChangeSource.text = getString(R.string.light_demo_changed_source_unknown)
        } else {
             binding.tvLightChangeSource.text = StringBuffer(sourceAddress).reverse().toString()
        }
    }

    override fun showDeviceDisconnectedDialog() {
        AlertDialog.Builder(requireActivity())
            .setMessage(R.string.light_demo_connection_lost)
            .setPositiveButton(R.string.light_demo_connection_lost_button) { dialog, which -> dialog.dismiss() }
            .setOnDismissListener { presenter.leaveDemo() }
            .create()
            .show()
    }
}