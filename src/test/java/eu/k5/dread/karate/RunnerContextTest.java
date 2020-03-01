package eu.k5.dread.karate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RunnerContextTest {
    RunnerContext context = new RunnerContext();

    @Test
    void transferDirectWithoutExpression() {
        context.requestStep("test").response("responseValue");
        context.transfer("#test#Response").to("#Project#test");
        RunnerContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("responseValue", property.getValue());
    }

    @Test
    void transferFormJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"}");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");

        RunnerContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("val", property.getValue());
    }

    @Test
    void transferFromInvalidJsonWithJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");

        RunnerContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("", property.getValue());
    }

    @Test
    void transferIntoJsonWithJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");
    }

    @Test
    void transferWithInvalidJpath() {
        context.setProperty("#Project#test", "insertedValue");
        context.requestStep("test").response("{ \"value\": \"before\" }");
        context.transfer("#Project#test").to("#test#Response", "$.value", "JSONPATH");

        RunnerContext.Property property = context.resolveProperty("#test#Response");
        Assertions.assertEquals("{\"value\":\"insertedValue\"}", property.getValue());
    }


    @Test
    void expandNullReturnsEmptyString() {
        Assertions.assertEquals("", context.expand(null));
    }

    @Test
    void expandEmptyReturnsEmptyString() {
        Assertions.assertEquals("", context.expand(""));
    }

    @Test
    void expandNoExpressionsReturnsValue() {
        Assertions.assertEquals("value", context.expand("value"));
    }

    @Test
    void expandWithNotExistingProperty_returnsEmpty() {
        Assertions.assertEquals("", context.expand("${#Project#value}"));
    }

    @Test
    void expandWithExistingProperty_returnsExpanded() {
        context.setProperty("#Project#value", "propertyValue");
        Assertions.assertEquals("propertyValue", context.expand("${#Project#value}"));
    }


}
