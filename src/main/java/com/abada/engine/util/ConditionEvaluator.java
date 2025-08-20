package com.abada.engine.util;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public class ConditionEvaluator {
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    public static boolean evaluate(String expression, Map<String, Object> variables) {
        Bindings bindings = engine.createBindings();
        bindings.putAll(variables);
        try {
            Object result = engine.eval(expression, bindings);
            return Boolean.TRUE.equals(result);
        } catch (ScriptException e) {
            throw new RuntimeException("Error evaluating condition: " + expression, e);
        }
    }
}