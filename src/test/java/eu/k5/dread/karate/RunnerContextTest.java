package eu.k5.dread.karate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RunnerContextTest {
    RunnerContext context = new RunnerContext();

    @Test
    public void transferDirectWithoutExpression() {
        context.requestStep("test").response("responseValue");
        context.transfer("#test#Response").to("#Project#test");
        RunnerContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("responseValue", property.getValue());
    }

    @Test
    public void transferFormJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"}");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");

        RunnerContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("val", property.getValue());
    }

    @Test
    public void transferFromInvalidJsonWithJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");

        RunnerContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("", property.getValue());
    }

    @Test
    public void transferWithInvalidJpath() {
        context.requestStep("test").response("{ \"value\": \"val\" }");
        context.transfer("#test#Response", "$value", "JSONPATH").to("#Project#test");

        RunnerContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("", property.getValue());
    }


    @Test
    public void expandNullReturnsEmptyString() {
        Assertions.assertEquals("", context.expand(null));
    }

    @Test
    public void expandEmptyReturnsEmptyString() {
        Assertions.assertEquals("", context.expand(""));
    }

    @Test
    public void expandNoExpressionsReturnsValue() {
        Assertions.assertEquals("value", context.expand("value"));
    }

    @Test
    public void expandWithNotExistingProperty_returnsEmpty() {
        Assertions.assertEquals("", context.expand("${#Project#value}"));
    }


    @Test
    public void expandWithExistingProperty_returnsExpanded() {
        context.setProperty("#Project#value", "propertyValue");
        Assertions.assertEquals("propertyValue", context.expand("${#Project#value}"));
    }

    @Test
    public void expandWithInlineGroovy_returnsExpanded() {
        Assertions.assertEquals("inlineString", context.expand("${=\"inlineString\"}"));
    }

    @Test
    public void expandWithInlineGroovyInJson_returnsExpanded() {
        Assertions.assertEquals("{ \"val\": \"inlineString\"}", context.expand("{ \"val\": \"${=\"inlineString\"}\"}"));
    }

    @Test
    public void expandWithInlineInvalidGoovy_returnsExpanded() {
        Assertions.assertEquals("No such property: x for class: Script2", context.expand("${=\"inlineString\"x}"));
    }


    @Test
    public void script() {
        RunnerContext.ScriptContext script = context.groovyScript("name").script("\"Test\"");
        String result = script.execute();
        Assertions.assertEquals("Test", result);
    }

    @Test
    public void scriptSoapUiApi_getPropertyValue() {
        context.setProperty("#TestCase#property", "value");
        String script = "testRunner.testCase.getPropertyValue(\"property\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();
        Assertions.assertEquals("value", scriptContext.result());
    }

    @Test
    public void scriptSoapUiApi_ProjectGetPropertyValue() {
        context.setProperty("#Project#property", "value");
        String script = "testRunner.testCase.testSuite.project.getPropertyValue(\"property\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();
        Assertions.assertEquals("value", scriptContext.result());
    }

    @Test
    public void scriptSoapUiApi_setPropertyValue() {
        String script = "testRunner.testCase.setPropertyValue(\"property\", \"scriptUpdate\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();

        Assertions.assertEquals("scriptUpdate", context.resolvePropertyValue("#TestCase#property"));
    }

    @Test
    public void scriptSoapUiApi_ProjectSetPropertyValue() {
        String script = "testRunner.testCase.testSuite.project.setPropertyValue(\"property\", \"scriptUpdate\")";
        RunnerContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();

        Assertions.assertEquals("scriptUpdate", context.resolvePropertyValue("#Project#property"));

    }

/*
        context.requestStep("test").response("{ \"value\": \"val\" }");
        context.transfer("#test#Response", "$value", "JSONPATH").to("#Project#test");

    }
*/

}
