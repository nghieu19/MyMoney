package com.example.mymoney.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mymoney.R;
import com.example.mymoney.model.SavingGoal;

import java.util.List;

public class SavingGoalAdapter extends RecyclerView.Adapter<SavingGoalAdapter.ViewHolder> {

    public interface OnGoalClickListener {
        void onGoalClick(SavingGoal goal);
    }

    private List<SavingGoal> goals;
    private OnGoalClickListener listener;

    public SavingGoalAdapter(List<SavingGoal> goals, OnGoalClickListener listener) {
        this.goals = goals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saving_goal, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavingGoal goal = goals.get(position);

        holder.txtName.setText(goal.getName());
        holder.txtAmount.setText(goal.getCurrentSaved() + " / " + goal.getTargetAmount());

        holder.progressBar.setMax(goal.getTargetAmount());
        holder.progressBar.setProgress(goal.getCurrentSaved());

        // CLICK
        holder.itemView.setOnClickListener(v -> listener.onGoalClick(goal));
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtName, txtAmount;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtGoalName);
            txtAmount = itemView.findViewById(R.id.txtGoalAmount);
            progressBar = itemView.findViewById(R.id.progressGoalItem);
        }
    }
}
