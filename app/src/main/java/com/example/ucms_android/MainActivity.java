package com.example.ucms_android;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.example.ucms_android.ui.admin.AdminDashboardFragment;
import com.example.ucms_android.ui.admin.AdminTicketListFragment;
import com.example.ucms_android.ui.common.AnalyticsFragment;
import com.example.ucms_android.ui.common.SettingsFragment;
import com.example.ucms_android.ui.student.StudentHomeFragment;
import com.example.ucms_android.ui.student.TicketListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
        
        bottomNav.getMenu().clear();
        if (isAdmin) {
            bottomNav.inflateMenu(R.menu.menu_admin);
        } else {
            bottomNav.inflateMenu(R.menu.menu_student);
        }

        if (savedInstanceState == null) {
            Fragment defaultFragment = isAdmin ? new AdminDashboardFragment() : new StudentHomeFragment();
            loadFragment(defaultFragment);
            bottomNav.setSelectedItemId(isAdmin ? R.id.nav_admin_dashboard : R.id.nav_student_home);
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
            // Add placeholders for other student tabs if needed
            else if (itemId == R.id.nav_student_add) fragment = new StudentHomeFragment(); 
            else if (itemId == R.id.nav_student_profile) fragment = new StudentHomeFragment();

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
