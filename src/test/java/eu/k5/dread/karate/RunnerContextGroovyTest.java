package eu.k5.dread.karate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RunnerContextGroovyTest {
    RunnerContext context = new RunnerContext();

    @Test
    void expandWithInlineGroovy_returnsExpanded() {
        Assertions.assertEquals("inlineString", context.expand("${=\"inlineString\"}"));
    }

    @Test
    void expandWithInlineGroovyInJson_returnsExpanded() {
        Assertions.assertEquals("{ \"val\": \"inlineString\"}", context.expand("{ \"val\": \"${=\"inlineString\"}\"}"));
    }

    @Test
    void expandWithInlineInvalidGoovy_returnsExpanded() {
        Assertions.assertEquals("No such property: x for class: Script2", context.expand("${=\"inlineString\"x}"));
    }


    @Test
    void script() {
        RunnerContext.ScriptContext script = context.groovyScript("name").script("\"Test\"");
        String result = script.execute();
        Assertions.assertEquals("Test", result);
    }

    @Test
    void scriptSoapUiApi_getPropertyValue() {
        context.setProperty("#TestCase#property", "value");
        String script = "testRunner.testCase.getPropertyValue(\"property\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();
        Assertions.assertEquals("value", scriptContext.result());
    }

    @Test
    void scriptSoapUiApi_ProjectGetPropertyValue() {
        context.setProperty("#Project#property", "value");
        String script = "testRunner.testCase.testSuite.project.getPropertyValue(\"property\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();
        Assertions.assertEquals("value", scriptContext.result());
    }

    @Test
    void scriptSoapUiApi_setPropertyValue() {
        String script = "testRunner.testCase.setPropertyValue(\"property\", \"scriptUpdate\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();

        Assertions.assertEquals("scriptUpdate", context.resolvePropertyValue("#TestCase#property"));
    }

    @Test
    void scriptSoapUiApi_ProjectSetPropertyValue() {
        String script = "testRunner.testCase.testSuite.project.setPropertyValue(\"property\", \"scriptUpdate\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();

        Assertions.assertEquals("scriptUpdate", context.resolvePropertyValue("#Project#property"));
    }
}