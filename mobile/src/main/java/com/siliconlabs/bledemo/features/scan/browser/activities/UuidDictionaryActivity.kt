package com.siliconlabs.bledemo.features.scan.browser.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.siliconlabs.bledemo.features.scan.browser.dialogs.AboutUuidDictionaryDialog
import com.siliconlabs.bledemo.features.scan.browser.fragments.CharacteristicMappingsFragment
import com.siliconlabs.bledemo.features.scan.browser.fragments.ServiceMappingsFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.activity_uuid_dictionary.*

class UuidDictionaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uuid_dictionary)

        setupViewPager()
        setupActionBar()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mappings_dictionary, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mappings_about -> {
                AboutUuidDictionaryDialog().show(supportFragmentManager, "about_mappings_dictionary_dialog")
            }
            android.R.id.home -> onBackPressed()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupViewPager() {
        view_pager2.adapter = ViewPagerAdapter()

        TabLayoutMediator(uuid_dictionary_tab_layout, view_pager2) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.title_services)
                1 -> getString(R.string.title_characteristics)
                else -> ""
            }
        }.attach()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private inner class ViewPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ServiceMappingsFragment()
                1 -> CharacteristicMappingsFragment()
                else -> ServiceMappingsFragment()
            }
        }
    }
}
