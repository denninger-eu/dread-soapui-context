package eu.k5.dread.soapui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DreadAssertionTest {
    SoapuiContext context = new SoapuiContext();

    @Test
    public void jsonExists_withExistingJson_true() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.response("{\"key\":\"value\"}");
        Assertions.assertTrue(step.assertJsonExists("$.key"));
    }

    @Test
    public void jsonExists_withNotExistingJson_false() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.response("{\"key\":\"value\"}");
        Assertions.assertFalse(step.assertJsonExists("$.key2"));
    }


    @Test
    public void assertJsonPathExists_foundAsExpected() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.response("{\"key\":\"value\"}");
        step.assertJsonPathExists("$.key", "true");
    }
    @Test
    public void assertJsonPathExists_withNotExistingJson_notFound_causesIllegalArgument() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.response("{\"key\":\"value\"}");
        Assertions.assertThrows(IllegalArgumentException.class, () -> step.assertJsonPathExists("$.key2", "true"));
    }

    @Test
    public void assertJsonPathExists_withNotExistingJson_notFoundAsExpected() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.response("{\"key\":\"value\"}");
        step.assertJsonPathExists("$.key2", "false");
    }

    @Test
    public void jsonExists_withItemInArray_true() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.response("[{\"key\":\"value\"}]");
        Assertions.assertTrue(step.assertJsonExists("$[0].key"));
    }

    @Test
    public void jsonExists_withEntity_true() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.response("[{\"key\":\"value\"}]");
        Assertions.assertTrue(step.assertJsonExists("$[0]"));
    }

    @Test
    public void assertStatus_noError() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.status(200);
        step.assertStatus(202, 200);
    }

    @Test
    public void assertStatusWithString_noError() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.status(200);
        step.assertStatus("202,, 200");
    }

    @Test
    public void assertStatusWithLinebreaks_noError() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.status(200);
        step.assertStatus("202\n 200");
    }

    @Test
    public void assertStatus_fail() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.status(200);
        Assertions.assertThrows(IllegalArgumentException.class, () -> step.assertStatus(202, 201));
    }

    @Test
    public void assertStatus_withString_fail() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.status(200);
        Assertions.assertThrows(IllegalArgumentException.class, () -> step.assertStatus("202, 201"));
    }

    @Test
    public void assertInvalidStatus_withString_ok() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.status(200);
        step.assertInvalidStatus("202, 201");
    }

    @Test
    public void assertInvalidStatus_withString_fail() {
        SoapuiContext.RestRequestContext step = context.requestStep("step");
        step.status(200);
        Assertions.assertThrows(IllegalArgumentException.class, () -> step.assertInvalidStatus("200, 201"));
    }
}
