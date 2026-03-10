package com.example.ucms_android;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ucms_android.session.SessionManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        
        // LOGIN BYPASS FOR TESTING
        setContentView(R.layout.activity_main);

        // FORCED TO ADMIN ROLE FOR UI TESTING
        String role = ROLE_ADMIN; 
        isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        
        // Re-inflate menu specifically for Admin
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.menu_admin);

        if (savedInstanceState == null) {
            // Load Admin Dashboard by default
            loadFragment(new AdminDashboardFragment());
            bottomNav.setSelectedItemId(R.id.nav_admin_dashboard);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            // Admin Navigation only
            if (itemId == R.id.nav_admin_dashboard) fragment = new AdminDashboardFragment();
            else if (itemId == R.id.nav_admin_tickets) fragment = new AdminTicketListFragment();
            else if (itemId == R.id.nav_admin_analytics) fragment = new AnalyticsFragment();
            else if (itemId == R.id.nav_admin_settings) fragment = new SettingsFragment();
            
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
