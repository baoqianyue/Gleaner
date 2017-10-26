package com.example.a6100890.gleaner;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.a6100890.gleaner.communication.ChatActivity;
import com.hyphenate.EMContactListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.EaseUI;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.ui.EaseContactListFragment;
import com.hyphenate.easeui.ui.EaseConversationListFragment;
import com.hyphenate.exceptions.HyphenateException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BaseActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private BottomNavigationView mBottomNavigationView;
    private ViewPager mViewPager;
    private MenuItem mMenuItem;
    private Toolbar mToolbar;
    private Boolean isLogin = false;
    private EaseContactListFragment mEaseContactListFragment;

    private static final String TAG = "BsaeActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        if (getIntent() != null) {
            isLogin = getIntent().getBooleanExtra("isLogin", false);
            Log.d(TAG, "onCreate: getintent"+isLogin);
        }

        if (!isLogin) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        }

        //Bmob应用ID: e4623d0c00213bb18860e466c83b5791
//        Bmob.initialize(this,"e4623d0c00213bb18860e466c83b5791");
        //环信 1129171011178759#gleaner
        initView();
        initEasemob();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        Log.d(TAG, "onCreateOptionsMenu: ");
        return true;
    }


    private void initView() {
        mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_nav);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);

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

        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.mipmap.littlemenu);


    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        mEaseContactListFragment = new EaseContactListFragment();
        adapter.addFragment(HomeFragment.newInstance());
        adapter.addFragment(LostListFragment.newInstance());
        adapter.addFragment(mEaseContactListFragment);

        mViewPager.setAdapter(adapter);
    }

    /**
     * 初始化环信框架
     */
    private void initEasemob() {
        EMOptions options = new EMOptions();
        EaseUI.getInstance().init(this, options);
//        默认添加好友认证
//        options.setAcceptInvitationAlways(false);

        EMClient.getInstance().init(getApplicationContext(), options);
        //在做打包混淆时关闭debug模式
        EMClient.getInstance().setDebugMode(true);

        //如果有其他服务启动活动,保证此初始化模块只进行一次
        int pid = android.os.Process.myPid();
        String processAppName = getAppName(pid);
        if (processAppName == null || !processAppName.equalsIgnoreCase(getPackageName())) {
            Log.d(TAG, "enter the service process!");
            return;
        }

        //添加好友监听
        EMClient.getInstance().contactManager().setContactListener(new EMContactListener() {
            @Override
            public void onContactAdded(final String s) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "onContactAdded " + s);
                        mEaseContactListFragment.setContactsMap(getContacts());
                        mEaseContactListFragment.refresh();
                    }
                }).start();
            }

            @Override
            public void onContactDeleted(String s) {

            }

            @Override
            public void onContactInvited(String s, String s1) {

            }

            @Override
            public void onFriendRequestAccepted(String s) {

            }

            @Override
            public void onFriendRequestDeclined(String s) {

            }
        });

        //联系人列表设置联系人
        new Thread(new Runnable() {
            @Override
            public void run() {
                mEaseContactListFragment.setContactsMap(getContacts());
            }
        }).start();

        //为了解决easeui包中的EaseContactListFragment无法关联到Gleaner,所以在这里设置点击联系人的监听
        //在优化包时可以考虑吧EaseContactListFragment放在Gleaner内,把此方法放到EaseContactListFragment中
        mEaseContactListFragment.setContactListItemClickListener(new EaseContactListFragment.EaseContactListItemClickListener() {
            @Override
            public void onListItemClicked(EaseUser user) {
                startActivity(new Intent(BaseActivity.this, ChatActivity.class).putExtra(EaseConstant.EXTRA_USER_ID, user.getUsername()));
            }
        });
    }

    /**
     * 获取正在运行的appName
     *
     * @param pID
     * @return
     */
    private String getAppName(int pID) {
        String processName = null;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List list = am.getRunningAppProcesses();
        Iterator i = list.iterator();
        PackageManager pm = getPackageManager();

        //对 am.getRunningAppProcesses() 获得的list遍历
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) i.next();
            try {
                if (info.pid == pID) {
                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return processName;
    }

    private Map<String, EaseUser> getContacts() {
        Map<String, EaseUser> map = new HashMap<>();
        try {
            List<String> userNames = EMClient.getInstance().contactManager().getAllContactsFromServer();
            for (String userId : userNames) {
                map.put(userId, new EaseUser(userId));
            }
        } catch (HyphenateException e) {
            e.printStackTrace();
        }

        return map;
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


    /**
     * 菜单项的点击事件处理
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_user:
                Log.d(TAG, "onOptionsItemSelected: user icon");
                break;
            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected: android.R.id.home");
            default:
        }
        return true;
    }
}