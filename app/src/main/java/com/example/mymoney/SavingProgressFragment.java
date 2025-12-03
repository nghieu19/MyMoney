package com.example.mymoney;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mymoney.database.AppDatabase;
import com.example.mymoney.database.dao.TransactionDao;
import com.example.mymoney.model.CategoryExpense;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class SavingProgressFragment extends Fragment {

    private String goalName;
    private int goalAmount;
    private int totalSaved = 0;

    private ProgressBar progressBar;
    private TextView txtTotalProgress;
    private LinearLayout categoryContainer;
    private EditText inputSavedMoney;
    private Button btnSaveProgress;

    private List<CategoryExpense> expensesSinceStart;

    // Format tiền
    private final DecimalFormat df = new DecimalFormat("#,###");

    public SavingProgressFragment() {}

    public static SavingProgressFragment newInstance(String name, int amount) {
        SavingProgressFragment f = new SavingProgressFragment();
        Bundle b = new Bundle();
        b.putString("goalName", name);
        b.putInt("goalAmount", amount);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_saving_goal_progress, container, false);

        readArguments();
        mapViews(v);

        loadSavedAmount();

        // 🔥 Lấy ngày bắt đầu tiết kiệm
        // (ở đây nếu bạn có lưu riêng cho từng goal thì sửa key lại cho đúng,
        // còn nếu chưa lưu thì savingStart = 0 -> lấy toàn bộ lịch sử)
        SharedPreferences prefsBudget =
                requireContext().getSharedPreferences("budget_prefs", Context.MODE_PRIVATE);
        long savingStart = prefsBudget.getLong("savingStart", 0);

        // 🔥 Lấy dữ liệu chi tiêu kể từ ngày bắt đầu (y hệt BudgetFragment)
        loadExpensesSinceSavingStart(savingStart, this::setupUI);

        return v;
    }

    private void readArguments() {
        Bundle a = getArguments();
        if (a == null) return;

        goalName = a.getString("goalName", "");
        goalAmount = a.getInt("goalAmount", 0);
    }

    private void mapViews(View v) {
        progressBar = v.findViewById(R.id.progressGoal);
        txtTotalProgress = v.findViewById(R.id.txtTotalProgress);
        categoryContainer = v.findViewById(R.id.categoryContainer);
        inputSavedMoney = v.findViewById(R.id.inputSavedMoney);
        btnSaveProgress = v.findViewById(R.id.btnSaveProgress);
    }

    // Đọc tổng tiền đã tiết kiệm cho goal này từ SAVING_GOALS
    private void loadSavedAmount() {
        SharedPreferences prefs =
                requireContext().getSharedPreferences("SAVING_GOALS", Context.MODE_PRIVATE);

        Set<String> rawSet = prefs.getStringSet("goal_list", new HashSet<>());

        for (String item : rawSet) {
            String[] arr = item.split("\\|");
            if (arr.length >= 3 && arr[0].equals(goalName)) {
                totalSaved = Integer.parseInt(arr[2]);
                break;
            }
        }
    }

    // 🔥 LẤY DỮ LIỆU GIỐNG BUDGETFRAGMENT
    private void loadExpensesSinceSavingStart(long savingStart, Runnable callback) {
        Executors.newSingleThreadExecutor().execute(() -> {

            TransactionDao dao = AppDatabase.getInstance(requireContext()).transactionDao();

            expensesSinceStart = dao.getExpensesByCategorySince(savingStart);

            requireActivity().runOnUiThread(callback);
        });
    }

    private void setupUI() {

        // ====== 1. Hiển thị tiến độ tiết kiệm ======
        int remain = Math.max(goalAmount - totalSaved, 0);

        String progressText =
                "Mục tiêu: " + df.format(goalAmount) + " VND\n" +
                        "Đã tiết kiệm: " + df.format(totalSaved) + " VND\n" +
                        "Còn thiếu: " + df.format(remain) + " VND";

        txtTotalProgress.setText(progressText);

        progressBar.setMax(goalAmount);
        progressBar.setProgress(totalSaved);

        // ====== 2. Hiển thị chi tiêu / limit ======
        categoryContainer.removeAllViews();

        TextView title = new TextView(getContext());
        title.setText("📌 Chi tiêu kể từ khi bắt đầu tiết kiệm:");
        title.setTextSize(18);
        title.setPadding(0, 0, 0, 20);
        categoryContainer.addView(title);

        // Lấy limit chi tiêu đã tính ở BudgetFragment (lưu trong budget_prefs)
        SharedPreferences budgetPrefs =
                requireContext().getSharedPreferences("budget_prefs", Context.MODE_PRIVATE);

        if (expensesSinceStart != null) {
            for (CategoryExpense ce : expensesSinceStart) {
                long spent = (long) ce.total;
                long limit = budgetPrefs.getLong("limit_" + ce.category, 0);
                addCategory(ce.category, spent, limit);
            }
        }

        // ====== 3. Cập nhật số tiền tiết kiệm ======
        btnSaveProgress.setOnClickListener(b -> {
            String val = inputSavedMoney.getText().toString().trim();
            if (TextUtils.isEmpty(val)) return;

            int add;
            try {
                add = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                inputSavedMoney.setError("Số tiền không hợp lệ");
                return;
            }

            totalSaved += add;
            saveUpdatedGoal(totalSaved);

            // cập nhật lại UI
            int newRemain = Math.max(goalAmount - totalSaved, 0);
            String newText =
                    "Mục tiêu: " + df.format(goalAmount) + " VND\n" +
                            "Đã tiết kiệm: " + df.format(totalSaved) + " VND\n" +
                            "Còn thiếu: " + df.format(newRemain) + " VND";
            txtTotalProgress.setText(newText);
            progressBar.setProgress(totalSaved);

            inputSavedMoney.setText("");
        });
    }

    private void addCategory(String name, long spent, long limit) {
        TextView tv = new TextView(getContext());

        String line;
        if (limit > 0) {
            line = "• " + name + ": " +
                    df.format(spent) + " / " +
                    df.format(limit) + " VND";
        } else {
            line = "• " + name + ": " +
                    df.format(spent) + " VND (chưa đặt giới hạn)";
        }

        tv.setText(line);
        tv.setTextSize(16);
        tv.setPadding(0, 12, 0, 12);
        categoryContainer.addView(tv);
    }

    // lưu lại tổng tiền đã tiết kiệm của goal hiện tại
    private void saveUpdatedGoal(int newValue) {
        SharedPreferences prefs =
                requireContext().getSharedPreferences("SAVING_GOALS", Context.MODE_PRIVATE);

        Set<String> raw = prefs.getStringSet("goal_list", new HashSet<>());
        Set<String> newSet = new HashSet<>();

        for (String item : raw) {
            String[] arr = item.split("\\|");
            if (arr.length >= 3 && arr[0].equals(goalName)) {
                newSet.add(arr[0] + "|" + arr[1] + "|" + newValue);
            } else {
                newSet.add(item);
            }
        }

        prefs.edit().putStringSet("goal_list", newSet).apply();
    }
}
