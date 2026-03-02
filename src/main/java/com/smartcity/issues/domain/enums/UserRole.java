package com.smartcity.issues.domain.enums;

public enum UserRole {
    CITIZEN,   // Can submit and view their own issues
    STAFF,     // Department staff - can update assigned issues
    ADMIN      // Full access - can assign, manage, view all
}
