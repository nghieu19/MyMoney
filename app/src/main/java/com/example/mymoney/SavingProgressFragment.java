package com.example.mymoney;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Set;

public class SavingProgressFragment extends Fragment {

    private String goalName;
    private int goalAmount;

    private int limitFood, limitHome, limitTransport, limitRelationship, limitEntertainment;

    private int spentFood = 0;
    private int spentHome = 0;
    private int spentTransport = 0;
    private int spentRelationship = 0;
    private int spentEntertainment = 0;

    private int totalSaved = 0;  // sẽ load từ SharedPreferences

    private ProgressBar progressBar;
    private TextView txtTotalProgress;
    private LinearLayout categoryContainer;
    private EditText inputSavedMoney;
    private Button btnSaveProgress;

    public SavingProgressFragment(
            String goalName,
            int goalAmount,
            int food, int home, int transport,
            int relationship, int entertainment
    ) {
        this.goalName = goalName;
        this.goalAmount = goalAmount;

        this.limitFood = food;
        this.limitHome = home;
        this.limitTransport = transport;
        this.limitRelationship = relationship;
        this.limitEntertainment = entertainment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_saving_goal_progress, container, false);

        progressBar        = view.findViewById(R.id.progressGoal);
        txtTotalProgress   = view.findViewById(R.id.txtTotalProgress);
        categoryContainer  = view.findViewById(R.id.categoryContainer);
        inputSavedMoney    = view.findViewById(R.id.inputSavedMoney);
        btnSaveProgress    = view.findViewById(R.id.btnSaveProgress);

        loadSavedAmount();  // ⭐ Lấy số tiền đã lưu thật từ SharedPreferences
        setupUI();

        return view;
    }

    // ============================================================
    // LOAD saved từ SharedPreferences
    // ============================================================
    private void loadSavedAmount() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("SAVING_GOALS", Context.MODE_PRIVATE);

        Set<String> rawSet = prefs.getStringSet("goal_list", new HashSet<>());

        for (String item : rawSet) {
            String[] arr = item.split("\\|");
            if (arr[0].equals(goalName)) {
                totalSaved = Integer.parseInt(arr[2]); // ⭐ load currentSaved
                break;
            }
        }
    }

    private void setupUI() {

        txtTotalProgress.setText(totalSaved + " / " + goalAmount);

        progressBar.setMax(goalAmount);
        progressBar.setProgress(totalSaved);

        addCategory("Ăn uống", spentFood, limitFood);
        addCategory("Nhà cửa", spentHome, limitHome);
        addCategory("Di chuyển", spentTransport, limitTransport);
        addCategory("Tình cảm", spentRelationship, limitRelationship);
        addCategory("Giải trí", spentEntertainment, limitEntertainment);

        btnSaveProgress.setOnClickListener(v -> {
            if (inputSavedMoney.getText().toString().trim().isEmpty()) return;

            int add = Integer.parseInt(inputSavedMoney.getText().toString());
            totalSaved += add;

            saveUpdatedGoal(totalSaved); // ⭐ LƯU VÀO SharedPreferences

            txtTotalProgress.setText(totalSaved + " / " + goalAmount);
            progressBar.setProgress(totalSaved);

            inputSavedMoney.setText("");
        });
    }

    // ============================================================
    // UPDATE + SAVE tiết kiệm
    // ============================================================
    private void saveUpdatedGoal(int newSavedValue) {

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("SAVING_GOALS", Context.MODE_PRIVATE);

        Set<String> rawSet = prefs.getStringSet("goal_list", new HashSet<>());
        Set<String> newSet = new HashSet<>();

        for (String item : rawSet) {
            String[] arr = item.split("\\|");
            String name   = arr[0];
            int target    = Integer.parseInt(arr[1]);
            int saved     = Integer.parseInt(arr[2]);

            if (name.equals(goalName)) {
                saved = newSavedValue;   // ⭐ cập nhật số tiền mới
            }

            newSet.add(name + "|" + target + "|" + saved);
        }

        prefs.edit().putStringSet("goal_list", newSet).apply();
    }

    private void addCategory(String name, int spent, int limit) {
        TextView tv = new TextView(getContext());
        tv.setText(name + ": " + spent + " / " + limit);
        tv.setTextSize(16);
        tv.setPadding(0, 12, 0, 12);
        categoryContainer.addView(tv);
    }
}
