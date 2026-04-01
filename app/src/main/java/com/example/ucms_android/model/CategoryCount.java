package com.example.ucms_android.model;
import com.google.gson.annotations.SerializedName;
public class CategoryCount {
    @SerializedName("categoryId")
    private Long categoryId;
    @SerializedName("categoryName")
    private String categoryName;
    @SerializedName("ticketCount")
    private long ticketCount;

    public CategoryCount(String name, long count) {
        this.categoryName = name;
        this.ticketCount = count;
    }

    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public long getTicketCount() { return ticketCount; }
}