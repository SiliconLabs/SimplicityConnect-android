package com.siliconlabs.bledemo.features.scan.browser.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.siliconlabs.bledemo.features.scan.browser.dialogs.AboutUuidDictionaryDialog
import com.siliconlabs.bledemo.features.scan.browser.fragments.CharacteristicMappingsFragment
import com.siliconlabs.bledemo.features.scan.browser.fragments.ServiceMappingsFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ActivityUuidDictionaryBinding


class UuidDictionaryActivity : AppCompatActivity() {
    //private val binding by viewBinding(ActivityUuidDictionaryBinding::bind)  //activity_uuid_dictionary
    private lateinit var binding: ActivityUuidDictionaryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUuidDictionaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                AboutUuidDictionaryDialog().show(
                    supportFragmentManager,
                    "about_mappings_dictionary_dialog"
                )
            }

            android.R.id.home -> onBackPressed()
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupViewPager() {

        binding.viewPager2.adapter = ViewPagerAdapter()


        TabLayoutMediator(binding.uuidDictionaryTabLayout, binding.viewPager2) { tab, position ->
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
