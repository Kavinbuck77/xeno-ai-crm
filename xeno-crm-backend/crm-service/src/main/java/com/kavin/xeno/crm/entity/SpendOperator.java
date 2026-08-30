package com.kavin.xeno.crm.entity;

public enum SpendOperator {
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EQUAL,
    BETWEEN;

    public static SpendOperator fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String clean = text.trim().toUpperCase();
        for (SpendOperator op : SpendOperator.values()) {
            if (op.name().equals(clean)) {
                return op;
            }
        }
        // Aliases / alternative representations
        switch (clean) {
            case "GT":
            case ">":
                return GREATER_THAN;
            case "GTE":
            case "GE":
            case ">=":
                return GREATER_THAN_OR_EQUAL;
            case "LT":
            case "<":
                return LESS_THAN;
            case "LTE":
            case "LE":
            case "<=":
                return LESS_THAN_OR_EQUAL;
            case "EQ":
            case "EXACT":
            case "=":
            case "==":
                return EQUAL;
            case "RANGE":
                return BETWEEN;
            default:
                return null;
        }
    }
}
