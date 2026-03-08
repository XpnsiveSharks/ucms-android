package com.example.ucms_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.ucms_android.auth.TokenManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.example.ucms_android.ui.admin.AdminDashboardFragment;
import com.example.ucms_android.ui.admin.AdminTicketListFragment;
import com.example.ucms_android.ui.common.AnalyticsFragment;
import com.example.ucms_android.ui.common.SettingsFragment;
import com.example.ucms_android.ui.student.TicketListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String ROLE_ADMIN = "ADMIN";
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TokenManager tokenManager = TokenManager.getInstance();
        if (!tokenManager.hasToken(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        String role = tokenManager.getRole(this);
        isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);

        if (!isAdmin) {
            MenuItem analyticsItem = bottomNav.getMenu().findItem(R.id.nav_analytics);
            if (analyticsItem != null) {
                analyticsItem.setVisible(false);
            }
        }

        if (savedInstanceState == null) {
            loadFragment(getDefaultFragment());
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
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
        if (itemId == R.id.nav_dashboard) {
            return isAdmin ? new AdminDashboardFragment() : new TicketListFragment();
        } else if (itemId == R.id.nav_tickets) {
            return isAdmin ? new AdminTicketListFragment() : new TicketListFragment();
        } else if (itemId == R.id.nav_analytics) {
            return new AnalyticsFragment();
        } else if (itemId == R.id.nav_settings) {
            return new SettingsFragment();
        }
        return null;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
