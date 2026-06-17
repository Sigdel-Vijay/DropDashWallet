package com.devroid.dropdashwallet;

public class RecentTransfer {
    private String name;
    private double amount;
    private String number;
    private String email;
    private long timestamp;

    // Empty constructor (required for Firebase)
    public RecentTransfer() {
    }

    // Full parameterized constructor
    public RecentTransfer(String name, double amount, String number, String email, long timestamp) {
        this.name = name;
        this.amount = amount;
        this.number = number;
        this.email = email;
        this.timestamp = timestamp;
    }

    // Getter and Setter for name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter and Setter for amount
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    // Getter and Setter for number
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    // Getter and Setter for email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getter and Setter for timestamp
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}