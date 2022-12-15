package com.siliconlabs.bledemo.home_screen.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.BuildConfig
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var _binding: FragmentSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingsBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.action_settings)
        initTexts()
    }

    private fun initTexts() {
        _binding.apply {
            dialogHelpVersionText.text = getString(R.string.version_text, BuildConfig.VERSION_NAME)
            silabsProductsWireless.addLink(LINK_MORE_INFO)
            silabsSupport.addLink(LINK_SUPPORT)
            githubSiliconlabsEfrconnect.addLink(LINK_SOURCECODE)
            docsSilabsBluetoothLatest.addLink(LINK_DOCUMENTATION)
            docsSilabsReleaseNotes.addLink(LINK_RELEASE_NOTES)
            usersGuideEfrconnect.addLink(LINK_USERS_GUIDE)
            helpTextPlaystore.linkToWebpage(LINK_GOOGLE_PLAY_STORE)
        }
    }

    private fun TextView.addLink(url: String) {
        this.text = url
        this.linkToWebpage(url)

    }

    private fun View.linkToWebpage(url: String) {
        setOnClickListener {
            val uri = Uri.parse("https://$url")
            val launchBrowser = Intent(Intent.ACTION_VIEW, uri)
            startActivity(launchBrowser)
        }
    }

    companion object {
        private const val LINK_MORE_INFO = "silabs.com/products/wireless"
        private const val LINK_SOURCECODE = "github.com/SiliconLabs/EFRConnect-android"
        private const val LINK_USERS_GUIDE = "docs.silabs.com/bluetooth/latest/miscellaneous/mobile/efr-connect-mobile-app"
        private const val LINK_SUPPORT = "silabs.com/support"
        private const val LINK_RELEASE_NOTES = "silabs.com/documents/public/release-notes/efr-connect-release-notes.pdf"
        private const val LINK_DOCUMENTATION = "docs.silabs.com/bluetooth/latest"
        private const val LINK_GOOGLE_PLAY_STORE = "play.google.com/store/apps/developer?id=Silicon+Laboratories"
    }

}