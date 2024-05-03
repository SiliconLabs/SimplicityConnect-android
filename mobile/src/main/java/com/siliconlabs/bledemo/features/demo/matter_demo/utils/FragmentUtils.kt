package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterDoorFragment
import java.util.Locale

object FragmentUtils {
    @JvmStatic
    fun <T> getHost(source: Fragment, hostClass: Class<T>): T {
        val activity = source.activity
        val parentFragment = source.parentFragment
        return when {
            hostClass.isInstance(parentFragment) -> hostClass.cast(parentFragment)
            hostClass.isInstance(activity) -> hostClass.cast(activity)
            else -> {
                val activityName = activity?.let { it::class.java.simpleName }
                val parentName = parentFragment?.let { it::class.java.simpleName }
                val exceptionString = String.format(
                    Locale.ROOT,
                    "Neither the parent Fragment $parentName nor the host Activity" +
                            " $activityName of ${source::class.java.simpleName} implement" +
                            " ${hostClass.simpleName}."
                )
                throw IllegalStateException(exceptionString)
            }
        }
    }
    fun isPasswordValid(password: String?): Boolean {
        val minLength = 8
        val maxLength = 100

        return password != null && password.length >= minLength && password.length <= maxLength
    }

    fun showMessageDialog(fragment:Fragment, msg: String, dismissListener: () -> Unit = {}) {
        val activity = fragment.activity

        if (activity != null && !activity.isFinishing) {
            if (fragment is MatterDoorFragment) {
                activity.runOnUiThread {
                    if (fragment.isAdded) {
                        val dialog = MessageDialogFragment()
                        dialog.setMessage(msg)
                        val transaction: FragmentTransaction = fragment.requireActivity().supportFragmentManager.beginTransaction()
                        dialog.show(transaction, "MessageDialog")
                    }
                }
            }
        } else {
            // Handle the case where the activity is null or has finished
            // You can log an error message or take appropriate action here.
        }


}}