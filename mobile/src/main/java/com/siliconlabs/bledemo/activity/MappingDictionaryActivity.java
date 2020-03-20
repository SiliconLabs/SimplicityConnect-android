package com.siliconlabs.bledemo.activity;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.adapters.ViewPagerAdapter;
import com.siliconlabs.bledemo.fragment.CharacteristicMappingsFragment;
import com.siliconlabs.bledemo.fragment.ServiceMappingsFragment;

public class MappingDictionaryActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mappings);

        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewPager viewPager = findViewById(R.id.view_pager);
        setupViewPager(viewPager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new CharacteristicMappingsFragment(), getString(R.string.title_Characteristics));
        adapter.addFragment(new ServiceMappingsFragment(), getString(R.string.title_Services));
        viewPager.setAdapter(adapter);

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

}
