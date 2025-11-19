# Service Provider Interface (SPI) Design

This document explains the Service Provider Interface (SPI) pattern used within the Abada Engine, specifically for the implementation of embedded Service Tasks.

---

### What is an SPI?

**SPI** stands for **Service Provider Interface**. It is an architectural pattern that enables a framework or application to be extended with custom functionality provided by its users.

It represents a form of "inversion of control" or a "plugin" model:

*   An **API** (Application Programming Interface) is when **your code calls the engine's code**. For example, when your application calls `abadaEngine.startProcess(...)`.
*   An **SPI** is when **the engine's code calls your code**. For example, when the engine encounters a service task and needs to execute your custom business logic.

### The `JavaDelegate` SPI

The Abada Engine uses this pattern to allow developers to create their own custom logic for service tasks that run embedded within the engine. The SPI is defined by two core interfaces located in the `com.abada.engine.spi` package.

#### 1. `JavaDelegate` Interface

This is the **contract**. It defines a single method, `execute(DelegateExecution execution)`, that all service task delegate classes must implement.

```java
public interface JavaDelegate {
    void execute(DelegateExecution execution) throws Exception;
}
```

The engine guarantees that it will call this method on any class specified in a service task's `camunda:class` attribute.

#### 2. `DelegateExecution` Interface

This is the **bridge**. It is a safe, controlled context object that the engine passes to your delegate. It allows your code to securely interact with the state of the running process (e.g., get/set variables) without having access to the engine's internal, more complex objects, which could be dangerous to modify directly.

```java
public interface DelegateExecution {
    String getProcessInstanceId();
    Map<String, Object> getVariables();
    Object getVariable(String name);
    void setVariable(String name, Object value);
}
```

### How It Works in Practice

1.  **You (The Service Provider)**: You write a standard Java class that implements the `JavaDelegate` interface. This is where you place your business logic.

    ```java
    public class MyCustomLogic implements JavaDelegate {
        @Override
        public void execute(DelegateExecution execution) {
            // Your business logic here...
            String orderId = (String) execution.getVariable("orderId");
            // ...call an external service, do calculations, etc.
            execution.setVariable("status", "Processed");
        }
    }
    ```

2.  **The BPMN File (The Configuration)**: You tell the engine which class to use for a specific service task using the `camunda:class` attribute.

    ```xml
    <bpmn:serviceTask id="MyTask" name="Process Order"
        camunda:class="com.mycompany.project.MyCustomLogic" />
    ```

3.  **The Abada Engine (The Framework)**: When the process reaches the "MyTask" service task, the engine:
    *   Reads the `camunda:class` attribute.
    *   Uses Java Reflection to find and create an instance of `com.mycompany.project.MyCustomLogic`.
    *   Calls the `execute()` method on that instance, passing it the `DelegateExecution` context.
    *   Waits for your method to finish, and then automatically moves to the next step in the process.

### Analogy: The Power Outlet

Think of an electrical outlet in a wall:

*   The **outlet** is the **SPI**. It's a standard, well-defined interface (two prongs, specific voltage).
*   The **power grid** behind the wall is the **Abada Engine**. It's a complex system that provides the service (electricity).
*   Your **lamp, toaster, or phone charger** is the **Service Provider** (your `JavaDelegate` class). You can plug anything you want into the outlet as long as it conforms to the standard.

You don't need to know how the power grid works to use it; you just need to build a plug that fits the standard outlet. Similarly, you don't need to know how the Abada Engine's internal loops work; you just need to write a class that implements the `JavaDelegate` interface.
