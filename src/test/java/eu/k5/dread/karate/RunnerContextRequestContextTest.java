package eu.k5.dread.karate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RunnerContextRequestContextTest {

    RunnerContext context = new RunnerContext();
    RunnerContext.RequestContext request = context.requestStep("test");

    @Test
    public void urlSetAndGet() {
        request.url("urlValue");
        Assertions.assertEquals("urlValue", request.url());
    }

    @Test
    @Disabled
    public void urlSetAndReadExpanded() {

    }

    @Test
    public void requestSetAndGet() {
        request.request("{}");
        Assertions.assertEquals("{}", request.request());
    }


    @Test
    public void assertJsonExists_existsInJson() {
        request.response("{\"id\": \"val\"}");
        Assertions.assertTrue(request.assertJsonExists("$.id"));
    }

}
