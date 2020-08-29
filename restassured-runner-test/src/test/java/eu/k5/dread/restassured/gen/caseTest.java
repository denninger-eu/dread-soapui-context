package eu.k5.dread.restassured.gen;

import eu.k5.dread.junit.Dependent;
import eu.k5.dread.soapui.SoapuiContext;
import eu.k5.dread.soapui.SoapuiContext.PropertyHolder;
import eu.k5.dread.soapui.SoapuiContext.RestRequestContext;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(Dependent.DependsOnTestWatcher.class)
@TestMethodOrder(Dependent.DependsOnMethodOrder.class)
public class caseTest {
    private SoapuiContext context;

    @BeforeAll
    public void init() {
        this.context = new SoapuiContext(this);
        this.context.setProperty("#TestCase#baseUrl", "crud01");
        initProperties();
        initcreateResource();
        initget_Resource();
        initupdateResource();
        context.groovyScript("Groovy").script(context.read("Groovy.groovy"));
        context.groovyScript("Groovy2").script(context.read("Groovy2.groovy"));
    }

    private void initProperties() {
        PropertyHolder step = context.propertiesStep("Properties");
        step.setProperty("key", "keyValue");
        step.setProperty("date", "${=java.time.LocalDateTime.now()}");
        step.setProperty("dynamicScript", "\"test\"");
    }

    private void initcreateResource() {
        RestRequestContext request = context.requestStep("createResource");
        request.url("${#TestCase#baseUrl}/resources").request("{ \"value\": \"${=\"String\"x}\"}\n");
    }

    @Test
    @DisplayName("createResource")
    public void createResource() {
        RestRequestContext request = context.requestStep("createResource");
        Response response = given()
                .header("headerP", "headerV")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .body(request.request())
                .post(request.url())
                .then()
                .extract().response();
        request.status(response.statusCode()).response(response.asString());
        request.assertStatus(200);
        request.assertJsonPathExists("$.id", "true");
    }

    @Test
    @DisplayName("transfer")
    @Dependent.DependsOn("createResource")
    public void transfer() {
        context.transfer("#createResource#Response", "$.id", "JSONPATH").to("#Project#projectProperty");
        context.transfer("#createResource#Response", "$.id", "JSONPATH").to("#TestSuite#suiteProperty");
        context.transfer("#createResource#Response", "$.id", "JSONPATH").to("#TestCase#caseProperty");
        context.transfer("#Properties#dynamicScript").to("#Groovy2#script");
    }

    private void initget_Resource() {
        RestRequestContext request = context.requestStep("get Resource");
        request.url("${#TestCase#baseUrl}/resources/${#Project#projectProperty}");
    }

    @Test
    @DisplayName("get Resource")
    @Dependent.DependsOn("transfer")
    public void get_Resource() {
        RestRequestContext request = context.requestStep("get Resource");
        Response response = given()
                .header("Accept", "application/json")
                .get(request.url())
                .then()
                .extract().response();
        request.status(response.statusCode()).response(response.asString());
        request.assertJsonPathExists("$.id", "true");
    }

    private void initupdateResource() {
        RestRequestContext request = context.requestStep("updateResource");
        request.url("${#TestCase#baseUrl}/resources/${#Project#projectProperty}").request("{ \"id\":\"${#Project#projectProperty}\", \"value\": \"updated\" }\n");
    }

    @Test
    @DisplayName("updateResource")
    @Dependent.DependsOn("get_Resource")
    public void updateResource() {
        RestRequestContext request = context.requestStep("updateResource");
        Response response = given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .body(request.request())
                .put(request.url())
                .then()
                .extract().response();
        request.status(response.statusCode()).response(response.asString());
        request.assertJsonPathExists("$.id", "true");
    }

    @Test
    @DisplayName("Groovy")
    @Dependent.DependsOn("updateResource")
    public void Groovy() {
        context.groovyScript("Groovy").execute();
    }

    @Test
    @DisplayName("Groovy2")
    @Dependent.DependsOn("Groovy")
    public void Groovy2() {
        context.groovyScript("Groovy2").execute();
    }

    @Test
    @DisplayName("TransferScriptResult")
    @Dependent.DependsOn("Groovy2")
    public void TransferScriptResult() {
        context.transfer("#Groovy2#result").to("#TestSuite#suiteProperty");
    }

}