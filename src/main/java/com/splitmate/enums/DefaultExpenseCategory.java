package com.splitmate.enums;

public enum DefaultExpenseCategory {

    // Food
    FOOD("Food"),
    DRINKS("Drinks"),
    GROCERIES("Groceries"),

    // Transportation
    TRANSPORTATION("Transportation"),
    FLIGHT("Flight"),
    TRAIN("Train"),


    // Travel
    ACCOMMODATION("Accommodation"),
    TICKETS("Tickets"),
    TOURS("Tours"),
    OUTDOOR_ACTIVITIES("Outdoor Activities"),

    // Entertainment
    ENTERTAINMENT("Entertainment"),

    // Shopping
    SHOPPING("Shopping"),

    // Daily life
    UTILITIES("Utilities"),

    // Other
    OTHER("Other");

    private final String displayName;

    DefaultExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
