package eu.k5.dread.karate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class RunnerContextGroovyTest {
    RunnerContext context = new RunnerContext();

    @Test
    void expandWithInlineGroovy_returnsExpanded() {
        Assertions.assertEquals("inlineString", context.expand("${=\"inlineString\"}"));
    }

    @Test
    void expandWithInlineGroovyMethodCall_returnsExpanded() {
        Assertions.assertEquals(LocalDate.now().toString(), context.expand("${=java.time.LocalDate.now()}"));
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

    @Test
    void script_resolveTestStepByName() {
        context.requestStep("requestStepName").request("requestContent");
        RunnerContext.ScriptContext script = context.groovyScript("script")
                .script("def testStep = testRunner.testCase.testSteps[\"requestStepName\"]; testStep.httpRequest.requestContent;");
        String result = script.execute();
        Assertions.assertEquals("requestContent", result);
    }

    @Test
    void script_withLogger() {
        RunnerContext.ScriptContext script = context.groovyScript("script")
                .script("log.info(\"test\"); \"result\";");
        String result = script.execute();
        Assertions.assertEquals("result", result);
    }

    
}
