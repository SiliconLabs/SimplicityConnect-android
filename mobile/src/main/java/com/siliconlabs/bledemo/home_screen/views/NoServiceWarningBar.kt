package com.siliconlabs.bledemo.home_screen.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentManager
import com.siliconlabs.bledemo.databinding.NoServiceWarningBarBinding
import com.siliconlabs.bledemo.home_screen.dialogs.WarningBarInfoDialog


abstract class NoServiceWarningBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    protected val _binding = NoServiceWarningBarBinding.inflate(LayoutInflater.from(context), this, true)
    private var fragmentManager: FragmentManager? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        initTexts()
        initClickListeners()
    }

    abstract fun initTexts()
    abstract fun initClickListeners()

    protected fun showAppSettingsScreen() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .apply { data = Uri.fromParts("package", context.packageName, null) }
            .also { context.startActivity(it) }
    }

    protected fun showAppNotificationSettingsScreen() {
        val intent = Intent()
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
        context.startActivity(intent)
    }

    protected fun showInfoDialog(type: WarningBarInfoDialog.Type) {
        fragmentManager?.let {
            WarningBarInfoDialog(type).show(it, "warning_bar_info_dialog")
        }
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager
    }
}