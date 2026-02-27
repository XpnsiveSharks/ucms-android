package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class Category {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    public Long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}
