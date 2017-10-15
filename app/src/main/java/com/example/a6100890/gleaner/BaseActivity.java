package com.example.a6100890.gleaner;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuView;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.example.a6100890.gleaner.R;

import cn.bmob.v3.Bmob;

public class BaseActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener{
    private BottomNavigationView mBottomNavigationView;
    private ViewPager mViewPager;
    private MenuItem mMenuItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        //Bmob应用ID: e4623d0c00213bb18860e466c83b5791
//        Bmob.initialize(this,"e4623d0c00213bb18860e466c83b5791");

        initView();


    }

    private void initView() {
        mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_nav);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);

        mBottomNavigationView.setOnNavigationItemSelectedListener(this);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mMenuItem != null) {
                    mMenuItem.setChecked(false);
                } else {
                    mBottomNavigationView.getMenu().getItem(0).setChecked(true);
                }

                mMenuItem = mBottomNavigationView.getMenu().getItem(position);
                mMenuItem.setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        setupViewPager();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_home:
                mViewPager.setCurrentItem(0);
                break;
            case R.id.menu_list:
                mViewPager.setCurrentItem(1);
                break;
            case R.id.menu_message:
                mViewPager.setCurrentItem(2);
                break;
        }
        return false;
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(HomeFragment.newInstance());
        adapter.addFragment(LostListFragment.newInstance());
        adapter.addFragment(MessageFragment.newInsatnce());
        mViewPager.setAdapter(adapter);
    }
}