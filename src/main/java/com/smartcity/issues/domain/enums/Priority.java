package com.smartcity.issues.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Priority {
    LOW      ("Low - Can wait",              1),
    MEDIUM   ("Medium - Address soon",       2),
    HIGH     ("High - Urgent attention",     3),
    CRITICAL ("Critical - Immediate action", 4);

    private final String description;
    private final int level;

    /** Auto-score priority based on category */
    public static Priority fromCategory(IssueCategory category) {
        return switch (category) {
            case ELECTRICAL_HAZARD, SEWAGE_OVERFLOW,
                 SAFETY_HAZARD, BRIDGE_DAMAGE       -> CRITICAL;
            case POWER_OUTAGE, NO_WATER_SUPPLY,
                 ROAD_FLOODING, WATER_LEAK           -> HIGH;
            case POTHOLE, DRAIN_BLOCKED,
                 STREETLIGHT, TRAFFIC_SIGNAL         -> MEDIUM;
            default                                  -> LOW;
        };
    }
}
