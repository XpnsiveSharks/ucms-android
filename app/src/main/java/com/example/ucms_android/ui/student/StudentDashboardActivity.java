package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class StudentDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dashboard is not yet implemented — route directly to ticket list
        startActivity(new Intent(this, TicketListActivity.class));
        finish();
    }
}
