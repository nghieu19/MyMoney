package com.example.mymoney;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymoney.adapter.SavingGoalAdapter;
import com.example.mymoney.model.SavingGoal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SavingGoalFragment extends Fragment {

    private RecyclerView recyclerSavingGoals;
    private SavingGoalAdapter adapter;
    private List<SavingGoal> goalList = new ArrayList<>();
    private ImageView btnAddGoal;

    private SharedPreferences prefs;

    // ============================
    // Các biến tạm cho wizard 3 bước
    // ============================
    private String tempGoalName;
    private int tempGoalAmount;
    private int tempMonths;
    private int tempIncome;
    private Button btnSavingHistory;

    private int tempFood, tempHome, tempTransport, tempRelation, tempEntertainment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_saving_goal, container, false);
        btnSavingHistory = view.findViewById(R.id.btnSavingHistory);

        btnSavingHistory.setOnClickListener(v -> openSavingHistory());


        recyclerSavingGoals = view.findViewById(R.id.recyclerSavingGoals);
        btnAddGoal = view.findViewById(R.id.btnAddGoal);

        prefs = requireContext().getSharedPreferences("SAVING_GOALS", Context.MODE_PRIVATE);

        adapter = new SavingGoalAdapter(goalList, goal -> {

            if (goal.getType().equals("manual")) {
                // mở SavingProgressFragment
                openProgressScreen(
                        goal.getName(),
                        goal.getTargetAmount(),
                        0,0,0,0,0
                );

            } else {
                // mở BudgetFragment (auto mode)
                openBudgetFragmentFromList(goal);
            }

        });


        recyclerSavingGoals.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSavingGoals.setAdapter(adapter);

        loadGoalsFromPrefs();
        btnAddGoal.setOnClickListener(v -> showAddGoalDialog());

        return view;
    }

    // ============================================================
    private void loadGoalsFromPrefs() {
        goalList.clear();

        Set<String> rawSet = prefs.getStringSet("goal_list", new HashSet<>());
        if (rawSet != null) {
            for (String item : rawSet) {
                String[] arr = item.split("\\|");
                String type = arr.length >= 4 ? arr[3] : "manual";

                goalList.add(new SavingGoal(
                        arr[0],
                        Integer.parseInt(arr[1]),
                        Integer.parseInt(arr[2]),
                        type
                ));

            }
        }

        adapter.notifyDataSetChanged();
    }

    private void saveGoalsToPrefs() {
        Set<String> outSet = new HashSet<>();

        for (SavingGoal g : goalList) {
            String record = g.getName() + "|" + g.getTargetAmount() + "|" + g.getCurrentSaved() + "|" + g.getType();
            outSet.add(record);
        }

        prefs.edit().putStringSet("goal_list", outSet).apply();
    }

    // ============================================================
    // STEP 1 — nhập tên
    private void showAddGoalDialog() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_goal_step1, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        EditText editGoalName = view.findViewById(R.id.editGoalName);
        Button btnNext = view.findViewById(R.id.btnNextStep);

        btnNext.setOnClickListener(v -> {
            String name = editGoalName.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Bạn chưa nhập tên mục tiết kiệm", Toast.LENGTH_SHORT).show();
                return;
            }

            tempGoalName = name;
            dialog.dismiss();
            showBasicSavingInfoDialog();
        });

        dialog.show();
    }

    // ============================================================
    // STEP 2 — nhập số tiền mục tiêu + số tháng + lương
    private void showBasicSavingInfoDialog() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_saving_basic, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        EditText inputGoalAmount = view.findViewById(R.id.inputGoalAmount);
        EditText inputMonths = view.findViewById(R.id.inputMonths);
        EditText inputIncome = view.findViewById(R.id.inputSalary);

        Button btnNext = view.findViewById(R.id.btnBasicNext);

        btnNext.setOnClickListener(v -> {

            if (inputGoalAmount.getText().toString().isEmpty()
                    || inputMonths.getText().toString().isEmpty()
                    || inputIncome.getText().toString().isEmpty()) {

                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            tempGoalAmount = Integer.parseInt(inputGoalAmount.getText().toString());
            tempMonths = Integer.parseInt(inputMonths.getText().toString());
            tempIncome = Integer.parseInt(inputIncome.getText().toString());

            dialog.dismiss();
            showChooseMethodDialog();
        });

        dialog.show();
    }

    // ============================================================
    // STEP 3 — chọn cách thiết lập hạn mức
    private void showChooseMethodDialog() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_choose_method, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        LinearLayout optionManual = view.findViewById(R.id.optionManual);
        LinearLayout optionAuto = view.findViewById(R.id.optionAuto);

        optionManual.setOnClickListener(v -> {
            dialog.dismiss();
            showManualLimitDialog();
        });

        optionAuto.setOnClickListener(v -> {
            dialog.dismiss();
            openBudgetFragment();
        });

        dialog.show();
    }

    // ============================================================
    // STEP 4 — nhập limit thủ công
    private void showManualLimitDialog() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_set_limit, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        EditText edtFood = view.findViewById(R.id.limitFood);
        EditText edtHome = view.findViewById(R.id.limitHome);
        EditText edtTransport = view.findViewById(R.id.limitTransport);
        EditText edtRelation = view.findViewById(R.id.limitRelationship);
        EditText edtEntertain = view.findViewById(R.id.limitEntertainment);

        Button btnStart = view.findViewById(R.id.btnStartSaving);

        btnStart.setOnClickListener(v -> {

            if (edtFood.getText().toString().isEmpty()
                    || edtHome.getText().toString().isEmpty()
                    || edtTransport.getText().toString().isEmpty()
                    || edtRelation.getText().toString().isEmpty()
                    || edtEntertain.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đủ 5 danh mục", Toast.LENGTH_SHORT).show();
                return;
            }

            tempFood = Integer.parseInt(edtFood.getText().toString());
            tempHome = Integer.parseInt(edtHome.getText().toString());
            tempTransport = Integer.parseInt(edtTransport.getText().toString());
            tempRelation = Integer.parseInt(edtRelation.getText().toString());
            tempEntertainment = Integer.parseInt(edtEntertain.getText().toString());

            dialog.dismiss();

            addGoalToList(tempGoalName, tempGoalAmount, "manual");

            openProgressScreen(tempGoalName, tempGoalAmount,
                    tempFood, tempHome, tempTransport, tempRelation, tempEntertainment);
        });

        dialog.show();
    }

    // ============================================================
    private void addGoalToList(String name, int goalAmount, String type) {
        goalList.add(new SavingGoal(name, goalAmount, 0, type));
        saveGoalsToPrefs();
        adapter.notifyDataSetChanged();
    }


    // ============================================================
    private void openProgressScreen(String name, int targetAmount,
                                    int food, int home, int transport,
                                    int relation, int entertain) {

        Fragment fragment = SavingProgressFragment.newInstance(name, targetAmount);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ============================================================
    // ⭐⭐ HÀM SILVER BULLET — FIX AUTO MODE ⭐⭐
    private void openBudgetFragment() {
        addGoalToList(tempGoalName, tempGoalAmount, "auto");
        BudgetFragment fragment = BudgetFragment.newInstance(
                tempGoalName,     // ⭐ THÊM
                tempGoalAmount,
                tempMonths,
                tempIncome
        );


        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
    private void openBudgetFragmentFromList(SavingGoal goal) {

        int months = prefs.getInt(goal.getName() + "_months", 1);
        int income = prefs.getInt(goal.getName() + "_income", 0);

        BudgetFragment fragment = BudgetFragment.newInstance(
                goal.getName(),
                goal.getTargetAmount(),
                months,
                income
        );

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
    private void openSavingHistory() {
        Fragment fragment = new SavingHistoryFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }


}
