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

    private String tempGoalName;
    private int tempGoalAmount;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_saving_goal, container, false);

        recyclerSavingGoals = view.findViewById(R.id.recyclerSavingGoals);
        btnAddGoal = view.findViewById(R.id.btnAddGoal);

        prefs = requireContext().getSharedPreferences("SAVING_GOALS", Context.MODE_PRIVATE);

        adapter = new SavingGoalAdapter(goalList, goal -> {
            openProgressScreen(
                    goal.getName(),
                    goal.getTargetAmount(),
                    0,0,0,0,0  // bạn muốn truyền limit thật thì thêm vào
            );
        });

        recyclerSavingGoals.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSavingGoals.setAdapter(adapter);

        loadGoalsFromPrefs();    // ⭐ LOAD dữ liệu đã lưu

        btnAddGoal.setOnClickListener(v -> showAddGoalDialog());

        return view;
    }

    // ============================================================
    // LOAD từ SharedPreferences
    // ============================================================
    private void loadGoalsFromPrefs() {
        goalList.clear();

        Set<String> rawSet = prefs.getStringSet("goal_list", new HashSet<>());
        if (rawSet != null) {
            for (String item : rawSet) {
                // format: name|target|saved
                String[] arr = item.split("\\|");
                goalList.add(new SavingGoal(arr[0], Integer.parseInt(arr[1]), Integer.parseInt(arr[2])));
            }
        }

        adapter.notifyDataSetChanged();
    }

    // ============================================================
    // SAVE vào SharedPreferences
    // ============================================================
    private void saveGoalsToPrefs() {
        Set<String> outSet = new HashSet<>();

        for (SavingGoal g : goalList) {
            String record = g.getName() + "|" + g.getTargetAmount() + "|" + g.getCurrentSaved();
            outSet.add(record);
        }

        prefs.edit().putStringSet("goal_list", outSet).apply();
    }

    // ============================================================
    // DIALOGS STEPS
    // ============================================================

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

    private void showBasicSavingInfoDialog() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_saving_basic, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        EditText inputGoalAmount = view.findViewById(R.id.inputGoalAmount);
        Button btnNext = view.findViewById(R.id.btnBasicNext);

        btnNext.setOnClickListener(v -> {

            if (inputGoalAmount.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập số tiền mục tiêu", Toast.LENGTH_SHORT).show();
                return;
            }

            tempGoalAmount = Integer.parseInt(inputGoalAmount.getText().toString());

            dialog.dismiss();
            showChooseMethodDialog();
        });

        dialog.show();
    }

    private void showChooseMethodDialog() {
        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_choose_method, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        LinearLayout optionManual = view.findViewById(R.id.optionManual);

        optionManual.setOnClickListener(v -> {
            dialog.dismiss();
            showManualLimitDialog();
        });

        dialog.show();
    }

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

            dialog.dismiss();

            // ⭐ LƯU MỤC TIẾT KIỆM
            addGoalToList(tempGoalName, tempGoalAmount);

            // ⭐ Mở màn theo dõi
            openProgressScreen(
                    tempGoalName,
                    tempGoalAmount,
                    0,0,0,0,0 // bạn muốn thêm limit thì sửa thêm vào sau
            );
        });

        dialog.show();
    }

    // ============================================================
    // ADD + SAVE
    // ============================================================
    private void addGoalToList(String name, int goalAmount) {

        goalList.add(new SavingGoal(name, goalAmount, 0));

        saveGoalsToPrefs();   // ⭐ QUAN TRỌNG: LƯU LẠI
        adapter.notifyDataSetChanged();
    }

    private void openProgressScreen(String name, int targetAmount,
                                    int food, int home, int transport,
                                    int relation, int entertain) {

        Fragment fragment = new SavingProgressFragment(
                name,
                targetAmount,
                food, home, transport, relation, entertain
        );

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
