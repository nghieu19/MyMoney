package com.example.mymoney.model;

public class SavingGoal {

    private String name;
    private int targetAmount;   // số tiền mục tiêu cần tiết kiệm
    private int currentSaved;   // số tiền đã tiết kiệm

    public SavingGoal(String name, int targetAmount, int currentSaved) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentSaved = currentSaved;
    }

    public String getName() {
        return name;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getCurrentSaved() {
        return currentSaved;
    }

    public void setCurrentSaved(int currentSaved) {
        this.currentSaved = currentSaved;
    }
}
