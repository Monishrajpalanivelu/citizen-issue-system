package com.smartcity.issues.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum IssueCategory {

    // Infrastructure & Roads
    POTHOLE("Pothole or Road Damage", "INFRA"),
    ROAD_FLOODING("Road Flooding", "INFRA"),
    BRIDGE_DAMAGE("Bridge/Overpass Damage", "INFRA"),
    PAVEMENT("Broken Pavement / Sidewalk", "INFRA"),

    // Water & Sewage
    WATER_LEAK("Water Pipe Leak", "WATER"),
    NO_WATER_SUPPLY("No Water Supply", "WATER"),
    SEWAGE_OVERFLOW("Sewage Overflow", "WATER"),
    WATER_QUALITY("Water Quality Issue", "WATER"),

    // Electricity & Power
    POWER_OUTAGE("Power Outage", "POWER"),
    STREETLIGHT("Streetlight Not Working", "POWER"),
    ELECTRICAL_HAZARD("Electrical Hazard", "POWER"),

    // Sanitation
    GARBAGE_OVERFLOW("Garbage Not Collected", "SANIT"),
    ILLEGAL_DUMPING("Illegal Dumping", "SANIT"),
    DRAIN_BLOCKED("Blocked Drain", "SANIT"),

    // Parks & Public Spaces
    PARK_DAMAGE("Park/Playground Damage", "PARKS"),
    GRAFFITI("Graffiti / Vandalism", "PARKS"),
    TREE_HAZARD("Fallen/Hazardous Tree", "PARKS"),

    // Public Safety
    SAFETY_HAZARD("Public Safety Hazard", "SAFETY"),
    ABANDONED_VEHICLE("Abandoned Vehicle", "SAFETY"),

    // Transport
    TRAFFIC_SIGNAL("Traffic Signal Issue", "TRANS"),
    ROAD_SIGN("Missing/Damaged Road Sign", "TRANS"),
    PUBLIC_TRANSPORT("Public Transport Issue", "TRANS"),

    // General
    OTHER("Other / General Complaint", "GENERAL");

    private final String displayName;
    private final String departmentCode; // maps directly to Department.code

    IssueCategory(String displayName, String departmentCode) {
        this.displayName = displayName;
        this.departmentCode = departmentCode;
    }
}
