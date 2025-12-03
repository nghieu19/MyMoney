package com.example.mymoney.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category")
public class Category {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "type")
    private String type; // expense / income

    @ColumnInfo(name = "icon")
    private String icon;

    // ⭐ Constructor bắt buộc của Room
    public Category() {}

    // ⭐ Constructor dành cho Seed Data
    public Category(String name, String description, String type, String icon) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.icon = icon;
    }

    // ===== Getter / Setter =====
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
