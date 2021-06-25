package com.siliconlabs.bledemo.connected_lighting.fragments

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.connected_lighting.activities.ConnectedLightingActivity
import com.siliconlabs.bledemo.connected_lighting.presenters.ConnectedLightingPresenter
import com.siliconlabs.bledemo.connected_lighting.models.TriggerSource
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.fragment_light.*

class ConnectedLightingFragment : Fragment(), ConnectedLightingPresenter.View {
    lateinit var presenter: ConnectedLightingPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_light, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = ConnectedLightingPresenter(this, activity as ConnectedLightingActivity)

        btn_lightbulb.setOnClickListener {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            presenter.onLightClicked()
        }
    }

    override fun showLightState(lightOn: Boolean) {
        if (lightOn) {
            btn_lightbulb.setImageResource(R.drawable.light_on)
            tv_light_status.setText(R.string.light_demo_on)
        } else {
            btn_lightbulb.setImageResource(R.drawable.light_off)
            tv_light_status.setText(R.string.light_demo_off)
        }
    }

    override fun showTriggerSourceDetails(source: TriggerSource?) {
        if (isAdded && !isDetached && !isRemoving) {
            if(source?.iconId == android.R.color.transparent) {
                iv_light_change_source_logo.visibility = View.GONE
            } else {
                iv_light_change_source_logo.setImageResource(source?.iconId!!)
                iv_light_change_source_logo.visibility = View.VISIBLE
            }
            tv_light_change_source_name.text = getText(source.textResId)
        }
    }

    override fun showTriggerSourceAddress(sourceAddress: String, source: TriggerSource?) {
        if (TriggerSource.UNKNOWN == source) {
            tv_light_change_source.text = getString(R.string.light_demo_changed_source_unknown)
        } else {
            tv_light_change_source.text = StringBuffer(sourceAddress).reverse().toString()
        }
    }

    override fun showDeviceDisconnectedDialog() {
        AlertDialog.Builder(activity!!)
                .setMessage(R.string.light_demo_connection_lost)
                .setPositiveButton(R.string.light_demo_connection_lost_button) { dialog, which -> dialog.dismiss() }
                .setOnDismissListener { presenter.leaveDemo() }
                .create()
                .show()
    }
}