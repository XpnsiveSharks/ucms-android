package com.example.ucms_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.example.ucms_android.ui.admin.AdminDashboardFragment;
import com.example.ucms_android.ui.admin.AdminTicketListFragment;
import com.example.ucms_android.ui.common.AnalyticsFragment;
import com.example.ucms_android.ui.common.SettingsFragment;
import com.example.ucms_android.ui.student.TicketListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private static final String ROLE_ADMIN = "ADMIN";
    private boolean isAdmin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        String role = sessionManager.getRole();
        isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        
        // Load the correct menu resource
        bottomNav.getMenu().clear();
        if (isAdmin) {
            bottomNav.inflateMenu(R.menu.menu_admin);
            bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        } else {
            bottomNav.inflateMenu(R.menu.menu_student);
            bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
        }

        if (savedInstanceState == null) {
            loadFragment(getDefaultFragment());
            // Select first item based on role
            bottomNav.setSelectedItemId(isAdmin ? R.id.nav_admin_dashboard : R.id.nav_student_home);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = getFragmentForItem(item.getItemId());
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private Fragment getDefaultFragment() {
        return isAdmin ? new AdminDashboardFragment() : new TicketListFragment();
    }

    private Fragment getFragmentForItem(int itemId) {
        // Handle Admin Items
        if (itemId == R.id.nav_admin_dashboard) return new AdminDashboardFragment();
        if (itemId == R.id.nav_admin_tickets) return new AdminTicketListFragment();
        if (itemId == R.id.nav_admin_analytics) return new AnalyticsFragment();
        if (itemId == R.id.nav_admin_settings) return new SettingsFragment();

        // Handle Student Items
        if (itemId == R.id.nav_student_home) return new TicketListFragment();
        // Add other student fragments here when ready (Add, Profile, etc)
        
        return null;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
