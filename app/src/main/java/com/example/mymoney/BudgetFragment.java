package com.example.mymoney;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.dao.TransactionDao;
import com.example.mymoney.model.CategoryExpense;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class BudgetFragment extends Fragment {

    // ==== Views ====
    private LinearLayout  layoutSavingSection;

    private EditText edtSavedMoney;
    private TextView tvResult, tvSavingPercent;

    private Button  btnEndSaving, btnUpdateSaved, btnRecalc;

    private ProgressBar progressSaving;
    private String goalName;


    // ==== Data ====
    private SharedPreferences prefs;
    private TransactionDao transactionDao;
    private final DecimalFormat df = new DecimalFormat("#,###");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        // ==== MAP VIEW ====
        layoutSavingSection = view.findViewById(R.id.layout_saving_section);


        tvResult = view.findViewById(R.id.tv_budget_result);
        tvSavingPercent = view.findViewById(R.id.tvSavingPercent);

        btnEndSaving = view.findViewById(R.id.btn_end_saving);
        btnUpdateSaved = view.findViewById(R.id.btn_update_saved);
        btnRecalc = view.findViewById(R.id.btn_recalc_budget);

        edtSavedMoney = view.findViewById(R.id.edt_saved_money);
        progressSaving = view.findViewById(R.id.progressSaving);

        prefs = requireContext().getSharedPreferences("budget_prefs", Context.MODE_PRIVATE);
        transactionDao = AppDatabase.getInstance(requireContext()).transactionDao();

        // ==== HIDE DEFAULT ====
        layoutSavingSection.setVisibility(View.GONE);
        btnEndSaving.setVisibility(View.GONE);
        btnUpdateSaved.setVisibility(View.GONE);
        edtSavedMoney.setVisibility(View.GONE);
        progressSaving.setVisibility(View.GONE);
        btnRecalc.setVisibility(View.GONE);

        // ==== BUTTON CLICK ====
        btnEndSaving.setOnClickListener(v -> endSavingAction());
        btnUpdateSaved.setOnClickListener(v -> updateSavedMoney());
        btnRecalc.setOnClickListener(v -> recalcBudgetAutomatically());

        // ==== AUTO MODE HANDLE (PHẢI ĐẶT SAU KHI MAP VIEW) ====
        Bundle args = getArguments();
        if (args != null) {
            goalName = args.getString("goalName", "");
        }


        if (args != null && args.containsKey("target_arg")) {

            long target = args.getLong("target_arg");
            long months = args.getLong("months_arg");
            long income = args.getLong("income_arg");

            Executors.newSingleThreadExecutor().execute(() -> {
                calculateBudget(target, months, income);

                requireActivity().runOnUiThread(() -> {
                    loadSavedPlan();
                });
            });
        } else {
            // Load nếu đang trong chế độ tiết kiệm
            if (prefs.getBoolean("isSaving", false)) {
                loadSavedPlan();
            }
        }

        return view;
    }
    // ============================================================
    // MAIN CALCULATE FUNCTION
    // ============================================================
    private void calculateBudget(long target, long months, long income) {

        long targetVal = floorToThousand(target);
        long monthsVal = months;
        long incomeVal = floorToThousand(income);

        long savingPerMonth = floorToThousand((double) targetVal / monthsVal);
        long maxExpensePerMonth = floorToThousand(incomeVal - savingPerMonth);

        // ==== Lấy dữ liệu 3 tháng gần nhất ====
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -3);
        long fromDate = cal.getTimeInMillis();

        List<CategoryExpense> expenses = transactionDao.getExpensesByCategorySince(fromDate);

        double totalExpense3M = 0;
        for (CategoryExpense ce : expenses) totalExpense3M += ce.total;

        long totalSpent = floorToThousand(totalExpense3M);
        if (totalSpent <= 0) totalSpent = 1;

        // ============================
        // 1) LƯU TOÀN BỘ GIÁ TRỊ
        // ============================
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong(goalName + "_target", targetVal);
        editor.putLong(goalName + "_months", monthsVal);
        editor.putLong(goalName + "_income", incomeVal);
        editor.putLong(goalName + "_savingPerMonth", savingPerMonth);
        editor.putLong(goalName + "_maxExpensePerMonth", maxExpensePerMonth);

        // Lưu LIMIT
        for (CategoryExpense ce : expenses) {
            long spent = floorToThousand(ce.total);
            double ratio = (double) spent / totalSpent;
            long limit = floorToThousand(ratio * maxExpensePerMonth);

            editor.putLong(goalName + "_limit_" + ce.category, limit);
        }

        // APPLY TRƯỚC KHI TẠO SUMMARY !!!
        editor.apply();


        // ============================
        // 2) TẠO SUMMARY – LÚC NÀY prefs đã có LIMIT đúng
        // ============================
        StringBuilder summary = new StringBuilder();
        summary.append("<b>🎯 Kế hoạch tiết kiệm</b><br><br>");
        summary.append("<b>Mục tiêu:</b> ").append(df.format(targetVal)).append(" VND<br>");
        summary.append("<b>Thời gian:</b> ").append(monthsVal).append(" tháng<br>");
        summary.append("<b>Lương:</b> ").append(df.format(incomeVal)).append(" VND<br><br>");
        summary.append("<b>Cần tiết kiệm mỗi tháng:</b> ").append(df.format(savingPerMonth)).append(" VND<br>");
        summary.append("<b>Được tiêu tối đa tháng này:</b> ").append(df.format(maxExpensePerMonth)).append(" VND<br><br>");
        summary.append("<b>🚀 Giới hạn theo thói quen 3 tháng gần nhất:</b><br>");

        for (CategoryExpense ce : expenses) {
            long limit = prefs.getLong(goalName + "_limit_" + ce.category, 0);
            summary.append("• ").append(ce.category).append(": ")
                    .append(df.format(limit)).append(" VND<br>");
        }

        // LƯU SUMMARY
        prefs.edit()
                .putString(goalName + "_summary", summary.toString())
                .putBoolean(goalName + "_isSaving", true)
                .apply();
    }




    private void loadSavedPlan() {

        String summary = prefs.getString(goalName + "_summary", "");
        long startTime = prefs.getLong(goalName + "_savingStart", 0);
        long savedManual = prefs.getLong(goalName + "_savedManual", 0);

        if (summary.isEmpty()) {
            layoutSavingSection.setVisibility(View.VISIBLE);
            return;
        }

        layoutSavingSection.setVisibility(View.VISIBLE);
        btnEndSaving.setVisibility(View.VISIBLE);
        edtSavedMoney.setVisibility(View.VISIBLE);
        btnUpdateSaved.setVisibility(View.VISIBLE);
        progressSaving.setVisibility(View.VISIBLE);

        String startDate = startTime == 0 ? "Chưa bắt đầu" : dateFormat.format(new Date(startTime));

        String finalText =
                summary +
                        "<br><b>Bắt đầu tiết kiệm:</b> " + startDate +
                        "<br><b>Đã tiết kiệm:</b> " + df.format(savedManual) + " VND<br>";

        tvResult.setText(android.text.Html.fromHtml(finalText));
        tvResult.setGravity(Gravity.START);

        long target = prefs.getLong(goalName + "_target", 0);

        int percent = target == 0 ? 0 : (int)((savedManual * 100) / target);
        if (percent > 100) percent = 100;

        progressSaving.setProgress(percent);
        tvSavingPercent.setText(percent + "%");

        Executors.newSingleThreadExecutor().execute(this::checkSavingProgress);
    }

    // ============================================================
    // UPDATE SAVED MONEY
    // ============================================================
    private void updateSavedMoney() {

        String savedStr = edtSavedMoney.getText().toString().trim();

        if (TextUtils.isEmpty(savedStr)) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Lỗi")
                    .setMessage("Vui lòng nhập số tiền hợp lệ.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        long added = floorToThousand(Long.parseLong(savedStr));
        long current = prefs.getLong(goalName + "_savedManual", 0);
        long newTotal = current + added;

        prefs.edit().putLong(goalName + "_savedManual", newTotal).apply();

        edtSavedMoney.setText("");

        loadSavedPlan();
    }




    // ============================================================
    // CHECK PROGRESS
    // ============================================================
    private void checkSavingProgress() {

        long target = prefs.getLong(goalName + "_target", 0);
        long savingStart = prefs.getLong(goalName + "_savingStart", 0);
        long saved = prefs.getLong(goalName + "_savedManual", 0);

        long savingPerMonth = prefs.getLong(goalName + "_savingPerMonth", 0);
        long maxExpensePerMonth = prefs.getLong(goalName + "_maxExpensePerMonth", 0);

        long expenseThisMonth = getExpenseThisMonth();

        boolean exceed = expenseThisMonth > maxExpensePerMonth;

        requireActivity().runOnUiThread(() -> {
            btnRecalc.setVisibility(exceed ? View.VISIBLE : View.GONE);
        });
    }


    // ============================================================
    private long getExpenseThisMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        long from = c.getTimeInMillis();
        return floorToThousand(transactionDao.getTotalExpenseSince(from));
    }


    private void recalcBudgetAutomatically() {

        long income = prefs.getLong("income", 0);
        long target = prefs.getLong("target", 0);
        long months = prefs.getLong("months", 0);

        if (income == 0 || target == 0 || months == 0) return;

        calculateBudget(target, months, income);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Đã tính lại")
                .setMessage("Ngân sách đã được cập nhật theo chi tiêu thực tế.")
                .setPositiveButton("OK", (d, w) -> loadSavedPlan())
                .show();
    }


    private long floorToThousand(double v) {
        return (long) (Math.floor(v / 1000) * 1000);
    }
    public static BudgetFragment newInstance(String goalName, long target, long months, long income) {
        BudgetFragment fragment = new BudgetFragment();
        Bundle args = new Bundle();
        args.putString("goalName", goalName);
        args.putLong("target_arg", target);
        args.putLong("months_arg", months);
        args.putLong("income_arg", income);
        fragment.setArguments(args);
        return fragment;
    }
    private void endSavingAction() {

        long target = prefs.getLong(goalName + "_target", 0);
        long saved = prefs.getLong(goalName + "_savedManual", 0);

        // ============= LƯU VÀO LỊCH SỬ =============
        SharedPreferences historyPrefs = requireContext().getSharedPreferences("SAVING_HISTORY", Context.MODE_PRIVATE);
        Set<String> historySet = historyPrefs.getStringSet("history_list", new HashSet<>());

        String record = goalName + "|" + target + "|" + saved + "|" + System.currentTimeMillis();
        historySet.add(record);

        historyPrefs.edit().putStringSet("history_list", historySet).apply();
        // ============================================

        // Xoá dữ liệu hiện tại
        prefs.edit().clear().apply();

        layoutSavingSection.setVisibility(View.GONE);
        tvResult.setText("Hãy nhập thông tin để tạo kế hoạch tiết kiệm mới.");
        tvResult.setGravity(Gravity.CENTER_HORIZONTAL);
    }


}
