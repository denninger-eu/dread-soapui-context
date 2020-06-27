package eu.k5.dread.soapui;

import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RestRequestStepTest {

    SoapuiContext context = new SoapuiContext();
    SoapuiContext.RestRequestContext request = context.requestStep("test");

    @Test
    void urlSetAndGet() {
        request.url("urlValue");
        RestRequestStepTest[] x = {new RestRequestStepTest(), new RestRequestStepTest()};
        Assertions.assertEquals("urlValue", request.url());
    }

    @Test
    void urlSetAndReadExpanded() {
        context.setProperty("#Global#baseUrl", "base");
        request.url("${#Global#baseUrl}/url");
        Assertions.assertEquals("base/url", request.url());
    }

    @Test
    void responseSetArrayAndGet() {
        JSONArray array = new JSONArray();
        array.add(1);
        request.response(array);
        Assertions.assertEquals("[1]", request.response());
    }

    @Test
    void requestSetAndGet() {
        request.request("{}");
        Assertions.assertEquals("{}", request.request());
    }

    @Test
    void assertJsonExists_existsInJson() {
        request.response("{\"id\": \"val\"}");
        Assertions.assertTrue(request.assertJsonExists("$.id"));
    }

}
