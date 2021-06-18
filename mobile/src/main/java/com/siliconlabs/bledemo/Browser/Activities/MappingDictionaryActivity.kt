package com.siliconlabs.bledemo.browser.activities

import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.siliconlabs.bledemo.browser.adapters.ViewPagerAdapter
import com.siliconlabs.bledemo.browser.dialogs.AboutMappingsDictionaryDialog
import com.siliconlabs.bledemo.browser.fragments.CharacteristicMappingsFragment
import com.siliconlabs.bledemo.browser.fragments.ServiceMappingsFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.actionbar.*
import kotlinx.android.synthetic.main.activity_mappings.*

class MappingDictionaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mappings)

        setupViewPager()
        initViewPagerBehavior()
        setSupportActionBar(toolbar)

        tv_characteristics_tab.setOnClickListener {
            setTabSelected(tv_characteristics_tab)
            setTabUnselected(tv_services_tab)
            view_pager.currentItem = 0
        }

        tv_services_tab.setOnClickListener {
            setTabSelected(tv_services_tab)
            setTabUnselected(tv_characteristics_tab)
            view_pager.currentItem = 1
        }

        iv_go_back.setOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mappings_dictionary, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mappings_about -> {
                val dialog = AboutMappingsDictionaryDialog()
                dialog.show(supportFragmentManager, "about_mappings_dictionary_dialog")
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Set Services textview width same as Characteristics
        tv_services_tab.width = tv_characteristics_tab.width
    }

    private fun initViewPagerBehavior() {
        view_pager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    setTabSelected(tv_characteristics_tab)
                    setTabUnselected(tv_services_tab)
                } else if (position == 1) {
                    setTabSelected(tv_services_tab)
                    setTabUnselected(tv_characteristics_tab)
                }
            }

            override fun onPageScrollStateChanged(i: Int) {}
        })
    }

    private fun setTabSelected(textView: TextView?) {
        textView?.background = ContextCompat.getDrawable(this, R.drawable.btn_rounded_white)
        textView?.setTextColor(ContextCompat.getColor(this, R.color.silabs_red))
        textView?.typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun setTabUnselected(textView: TextView?) {
        textView?.background = ContextCompat.getDrawable(this, R.drawable.btn_rounded_red_dark)
        textView?.setTextColor(ContextCompat.getColor(this, R.color.silabs_white))
        textView?.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(CharacteristicMappingsFragment(), getString(R.string.title_Characteristics))
        adapter.addFragment(ServiceMappingsFragment(), getString(R.string.title_Services))
        view_pager?.adapter = adapter
    }

}
