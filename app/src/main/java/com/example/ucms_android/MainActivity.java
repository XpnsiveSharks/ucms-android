package com.example.ucms_android;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.sync.LiveUpdatePoller;
import com.example.ucms_android.sync.RealtimeStreamManager;
import com.example.ucms_android.ui.admin.AdminDashboardFragment;
import com.example.ucms_android.ui.admin.AdminTicketListFragment;
import com.example.ucms_android.ui.common.AnalyticsFragment;
import com.example.ucms_android.ui.common.SettingsFragment;
import com.example.ucms_android.ui.student.StudentHomeFragment;
import com.example.ucms_android.ui.student.StudentProfileFragment;
import com.example.ucms_android.ui.student.SubmitTicketFragment;
import com.example.ucms_android.ui.student.TicketListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String ROLE_ADMIN = "ADMIN";
    private boolean isAdmin;
    private SessionManager sessionManager;
    private LiveUpdatePoller liveUpdatePoller;
    private RealtimeStreamManager realtimeStreamManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        liveUpdatePoller = new LiveUpdatePoller(this);
        realtimeStreamManager = new RealtimeStreamManager(this);
        
        setContentView(R.layout.activity_main);

        String role = sessionManager.getRole();
        isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        
        bottomNav.getMenu().clear();
        if (isAdmin) {
            bottomNav.inflateMenu(R.menu.menu_admin);

            if (savedInstanceState == null) {
                loadFragment(new AdminDashboardFragment());
                bottomNav.setSelectedItemId(R.id.nav_admin_dashboard);
            }
        } else {
            bottomNav.inflateMenu(R.menu.menu_student);

            if (savedInstanceState == null) {
                loadFragment(new StudentHomeFragment());
                bottomNav.setSelectedItemId(R.id.nav_student_home);
            }
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_admin_dashboard) fragment = new AdminDashboardFragment();
            else if (itemId == R.id.nav_admin_tickets) fragment = new AdminTicketListFragment();
            else if (itemId == R.id.nav_admin_analytics) fragment = new AnalyticsFragment();
            else if (itemId == R.id.nav_admin_settings) fragment = new SettingsFragment();
            else if (itemId == R.id.nav_student_home) fragment = new StudentHomeFragment();
            else if (itemId == R.id.nav_student_tickets) fragment = new TicketListFragment();
            else if (itemId == R.id.nav_student_add) fragment = new SubmitTicketFragment();
            else if (itemId == R.id.nav_student_settings) fragment = new SettingsFragment();
            
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            liveUpdatePoller.start();
            realtimeStreamManager.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (liveUpdatePoller != null) {
            liveUpdatePoller.stop();
        }
        if (realtimeStreamManager != null) {
            realtimeStreamManager.stop();
        }
    }

    public void loadFragment(Fragment fragment) {
        loadFragment(fragment, false);
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    public void setBottomNavVisible(boolean visible) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        if (bottomNav != null) {
            bottomNav.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
