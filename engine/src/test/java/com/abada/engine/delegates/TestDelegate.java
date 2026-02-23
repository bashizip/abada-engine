package com.abada.engine.delegates;

import com.abada.engine.spi.DelegateExecution;
import com.abada.engine.spi.JavaDelegate;

/**
 * A simple delegate for testing service task execution.
 */
public class TestDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Retrieve an existing variable
        Integer counter = (Integer) execution.getVariable("counter");
        if (counter == null) {
            counter = 0;
        }

        // Modify the variable
        execution.setVariable("counter", counter + 1);

        // Set a new variable to prove execution
        execution.setVariable("delegateExecuted", true);
    }
}
