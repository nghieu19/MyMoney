package com.example.mymoney.model;

public class SavingGoal {

    private String name;
    private int targetAmount;
    private int currentSaved;
    private String type; // "manual" hoặc "auto"

    // ===== Constructor =====
    public SavingGoal(String name, int targetAmount, int currentSaved, String type) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentSaved = currentSaved;
        this.type = type;
    }

    // ===== Getter/Setter =====
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
