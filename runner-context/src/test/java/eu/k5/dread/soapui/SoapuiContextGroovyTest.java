package eu.k5.dread.soapui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class SoapuiContextGroovyTest {
    SoapuiContext context = new SoapuiContext();

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
        String errorText = "No such property: x for class";
        Assertions.assertEquals(errorText, context.expand("${=\"inlineString\"x}").substring(0, errorText.length()));
    }


    @Test
    void script() {
        SoapuiContext.ScriptContext script = context.groovyScript("name").script("\"Test\"");
        String result = script.execute();
        Assertions.assertEquals("Test", result);
    }

    @Test
    void script_createWithSameName_returnsFirst() {
        Assertions.assertSame(context.groovyScript("name"), context.groovyScript("name"));
    }

    @Test
    void scriptSoapUiApi_getPropertyValue() {
        context.setProperty("#TestCase#property", "value");
        String script = "testRunner.testCase.getPropertyValue(\"property\")";
        SoapuiContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();
        Assertions.assertEquals("value", scriptContext.result());
    }

    @Test
    void scriptSoapUiApi_ProjectGetPropertyValue() {
        context.setProperty("#Project#property", "value");
        String script = "testRunner.testCase.testSuite.project.getPropertyValue(\"property\")";
        SoapuiContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();
        Assertions.assertEquals("value", scriptContext.result());
    }

    @Test
    void scriptSoapUiApi_setPropertyValue() {
        String script = "testRunner.testCase.setPropertyValue(\"property\", \"scriptUpdate\")";
        SoapuiContext.ScriptContext scriptContext = context.groovyScript("name");
        scriptContext.script(script).execute();

        Assertions.assertEquals("scriptUpdate", context.resolvePropertyValue("#TestCase#property"));
    }

    @Test
    void scriptSoapUiApi_ProjectSetPropertyValue() {
        String script = "testRunner.testCase.testSuite.project.setPropertyValue(\"property\", \"scriptUpdate\")";
        SoapuiContext.ScriptContext scriptContext = context.groovyScript("name");
        String result = scriptContext.script(script).execute();

        System.out.println(result);
        Assertions.assertEquals("scriptUpdate", context.resolvePropertyValue("#Project#property"));
    }

    @Test
    void script_resolveTestStepByName() {
        context.requestStep("requestStepName").request("requestContent");
        SoapuiContext.ScriptContext script = context.groovyScript("script")
                .script("def testStep = testRunner.testCase.testSteps[\"requestStepName\"]; testStep.httpRequest.requestContent;");
        String result = script.execute();
        Assertions.assertEquals("requestContent", result);
    }

    @Test
    void script_expand() {
        context.requestStep("requestStepName").request("requestContent");
        SoapuiContext.ScriptContext script = context.groovyScript("script")
                .script("context.expand('${#requeststepName#request}')");
        String result = script.execute();
        Assertions.assertEquals("requestContent", result);
    }

    @Test
    void script_withLogger() {
        SoapuiContext.ScriptContext script = context.groovyScript("script")
                .script("log.info(\"test\"); \"result\";");
        String result = script.execute();
        Assertions.assertEquals("result", result);
    }


    @Test
    void script_withSlurper() {
        context.requestStep("request").request("{ \"name\": \"John Doe\" }");
        SoapuiContext.ScriptContext script = context.groovyScript("script").script(
                "import groovy.json.JsonSlurper; " +
                        " def jsonSlurper = new JsonSlurper();" +
                        " def json = testRunner.testCase.testSteps['request'].httpRequest.requestContent;" +
                        " log.info(json);" +
                        " def object = jsonSlurper.parseText(json);" +
                        " testRunner.testCase.testSteps['request'].httpRequest.requestContent = object.name;" +
                        " object.name;");

        String result = script.execute();
        Assertions.assertEquals("John Doe", context.resolveProperty("#request#Request").getValue());

    }

    @Test
    void script_useResponse_withSlurper() {
        context.requestStep("request").response("{ \"name\": \"John Doe\" }");
        SoapuiContext.ScriptContext script = context.groovyScript("script").script(
                "import groovy.json.JsonSlurper; " +
                        " def jsonSlurper = new JsonSlurper();" +
                        " def json = testRunner.testCase.testSteps['request'].testRequest.response.responseContent;" +
                        " log.info(json);" +
                        " def object = jsonSlurper.parseText(json);" +
                        " testRunner.testCase.testSteps['request'].httpRequest.requestContent = object.name;" +
                        " object.name;");

        String result = script.execute();
        Assertions.assertEquals("John Doe", context.resolveProperty("#request#Request").getValue());

    }

}
