package eu.k5.dread.soapui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SoapuiContextTest {
    SoapuiContext context = new SoapuiContext(this);

    @Test
    void transferDirectWithoutExpression() {
        context.requestStep("test").response("responseValue");
        context.transfer("#test#Response").to("#Project#test");
        SoapuiContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("responseValue", property.getValue());
    }

    @Test
    void transfer_directWithoutExpressionCaseInsensitive() {
        context.requestStep("test").response("responseValue");
        context.transfer("#test#response").to("#Project#test");
        SoapuiContext.Property property = context.resolveProperty("#project#test");
        Assertions.assertEquals("responseValue", property.getValue());
    }

    @Test
    void transferFormJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"}");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");

        SoapuiContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("val", property.getValue());
    }

    @Test
    void transferFormJsonWithArrayPathSelectFirstEndsWith() {
        context.requestStep("test").response("[{ \"value\": \"val\", \"id\":\"idval\"}, { \"value\": \"val\", \"id\":\"idval\"}]");
        context.transfer("#test#Response", "$[?(@.value == 'val')].id[0]", "JSONPATH").to("#Project#test");

        SoapuiContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("idval", property.getValue());
    }

    @Test
    void transferFormJsonWithArrayPathSelectFirstInline() {
        context.requestStep("test").response("[{ \"value\": \"val\", \"id\":\"idval\"}, { \"value\": \"val\", \"id\":\"idval\"}]");
        context.transfer("#test#Response", "$[?(@.value == 'val')][0].id", "JSONPATH").to("#Project#test");

        SoapuiContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("idval", property.getValue());
    }

    @Test
    void transferFormJsonWithArrayPathSelectFirstInline_noResult() {
        context.requestStep("test").response("[{ \"value\": \"val\", \"id\":\"idval\"}, { \"value\": \"val\", \"id\":\"idval\"}]");
        context.transfer("#test#Response", "$[?(@.value == 'valX')][0].id", "JSONPATH").to("#Project#test");

        SoapuiContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("[]", property.getValue());
    }

    @Test
    void transferFromInvalidJsonWithJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");

        SoapuiContext.Property property = context.resolveProperty("#Project#test");
        Assertions.assertEquals("", property.getValue());
    }

    @Test
    void transferIntoJsonWithJsonPath() {
        context.requestStep("test").response("{ \"value\": \"val\"");
        context.transfer("#test#Response", "$.value", "JSONPATH").to("#Project#test");
    }


    @Test
    void transferIntoValidJsonPath() {
        context.setProperty("#Project#test", "insertedValue");
        context.requestStep("test").response("{ \"value\": \"before\" }");
        context.transfer("#Project#test").to("#test#Response", "$.value", "JSONPATH");

        SoapuiContext.Property property = context.resolveProperty("#test#Response");
        Assertions.assertEquals("{\"value\":\"insertedValue\"}", property.getValue());
    }

    @Test
    void transferJsonIntoValidJsonPath() {
        context.setProperty("#Project#test", "{\"key\": \"insertedValue\"}");
        context.requestStep("test").response("{ \"value\": \"before\" }");
        context.transfer("#Project#test").to("#test#Response", "$.value", "JSONPATH");

        SoapuiContext.Property property = context.resolveProperty("#test#Response");
        Assertions.assertEquals("{\"value\":{\"key\":\"insertedValue\"}}", property.getValue());
    }

    @Test
    void transferInvalidJsonIntoValidJsonPath() {
        context.setProperty("#Project#test", "{\"key\" \"insertedValue\"}");
        context.requestStep("test").response("{ \"value\": \"before\" }");
        context.transfer("#Project#test").to("#test#Response", "$.value", "JSONPATH");

        SoapuiContext.Property property = context.resolveProperty("#test#Response");
        Assertions.assertEquals("{\"value\":\"{\\\"key\\\" \\\"insertedValue\\\"}\"}", property.getValue());
    }


    @Test
    void transferFromValidXPath() {
        context.requestStep("test").response("<xml><element>value</element></xml>");
        context.transfer("#test#Response", "//element", "XPATH").to("#Project#test");

        Assertions.assertEquals("value", context.resolveProperty("#Project#test").getValue());
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

    @Test
    void expandPropertiesWithOutPrefix() {
        context.propertiesStep("name").setProperty("key", "value");
        SoapuiContext.Property property = context.resolveProperty("#key");
        Assertions.assertEquals("value", property.getValue());
    }

    @Test
    void expand_recursiveProperties_expandsRecursive() {
        SoapuiContext.PropertyHolder properties = context.propertiesStep("properties");
        properties.setProperty("property", "${=\"value\"}");
        properties.setProperty("indirect", "${#properties#property}");
        String indirect = context.expand("${#properties#indirect}");

        Assertions.assertEquals("value", indirect);
    }

    @Test
    @Disabled("will cause stackOverflow")
    void expand_recursiveProperties_expandsRecursive_overflowDetection() {
        SoapuiContext.PropertyHolder properties = context.propertiesStep("properties");
        properties.setProperty("first", "${#properties#second}");
        properties.setProperty("second", "${#properties#first}");
        String indirect = context.expand("${#properties#first}");

        Assertions.assertEquals("value", indirect);
    }

    @Test
    void expand_doubleExpand_createSameResult() {
        context.propertiesStep("properties").setProperty("uuid", "${=java.util.UUID.randomUUID().toString()}");
        String first = context.expand("${#properties#uuid}");
        String second = context.expand("${#properties#uuid}");
        Assertions.assertEquals(first, second);
    }

    @Test
    void read_existingFile_returnsContent() {
        Assertions.assertEquals("testcontent", context.read("test.txt"));
    }

    @Test
    void read_missingFile() {
        try {
            context.read("notexisting.txt");
            Assertions.fail("Should cause exception");
        } catch (IllegalArgumentException exception) {

        }
    }
}