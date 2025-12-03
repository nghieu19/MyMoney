package com.example.mymoney.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;

/**
 * Model dùng cho thống kê chi tiêu theo danh mục.
 * Dùng để nhận dữ liệu từ Room (Projection từ Transaction + Category).
 */
public class CategoryExpense {
    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "total")
    public double total;
}

