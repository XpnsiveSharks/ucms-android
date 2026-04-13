package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class AssignTicketRequest {
    @SerializedName("adminId")
    private String adminId;

    /** Self-assign: omits adminId so backend assigns to the calling admin. */
    public AssignTicketRequest() {}

    /** Assign to a specific admin by UUID. */
    public AssignTicketRequest(String adminId) {
        this.adminId = adminId;
    }

    public String getAdminId() { return adminId; }
}
