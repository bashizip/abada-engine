package com.abada.engine.dto;

/**
 * Represents a typed variable value for the variable management API.
 * Includes both the value and its type information.
 */
public record VariableValue(
        Object value,
        String type) {
    /**
     * Create a VariableValue from a Java object, inferring the type.
     */
    public static VariableValue from(Object obj) {
        if (obj == null) {
            return new VariableValue(null, "Null");
        }

        String typeName = switch (obj.getClass().getSimpleName()) {
            case "Integer" -> "Integer";
            case "Long" -> "Long";
            case "Double" -> "Double";
            case "Float" -> "Float";
            case "Boolean" -> "Boolean";
            case "String" -> "String";
            default -> obj.getClass().getSimpleName();
        };

        return new VariableValue(obj, typeName);
    }

    /**
     * Convert this VariableValue back to a Java object.
     */
    public Object toObject() {
        return value;
    }
}
