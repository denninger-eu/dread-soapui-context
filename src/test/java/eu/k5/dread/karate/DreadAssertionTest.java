package eu.k5.dread.karate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DreadAssertionTest {
    RunnerContext context = new RunnerContext();

    @Test
    public void jsonExists_withExistingJson_true() {
        RunnerContext.RestRequestContext step = context.requestStep("step");
        step.response("{\"key\":\"value\"}");
        Assertions.assertTrue(step.assertJsonExists("$.key"));
    }

    @Test
    public void jsonExists_withNotExistingJson_false() {
        RunnerContext.RestRequestContext step = context.requestStep("step");
        step.response("{\"key\":\"value\"}");
        Assertions.assertFalse(step.assertJsonExists("$.key2"));
    }

    @Test
    public void jsonExists_withItemInArray_true() {
        RunnerContext.RestRequestContext step = context.requestStep("step");
        step.response("[{\"key\":\"value\"}]");
        Assertions.assertTrue(step.assertJsonExists("$[0].key"));
    }

    @Test
    public void jsonExists_withEntity_true() {
        RunnerContext.RestRequestContext step = context.requestStep("step");
        step.response("[{\"key\":\"value\"}]");
        Assertions.assertTrue(step.assertJsonExists("$[0]"));
    }
}
