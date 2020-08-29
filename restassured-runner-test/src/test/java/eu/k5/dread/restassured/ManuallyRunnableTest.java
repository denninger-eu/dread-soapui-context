package eu.k5.dread.restassured;

import eu.k5.dread.junit.Dependent;
import eu.k5.dread.soapui.SoapuiContext;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;

@ExtendWith(Dependent.DependsOnTestWatcher.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(Dependent.DependsOnMethodOrder.class)
public class ManuallyRunnableTest {

    private static final String PREFIX = "crud01/resources";

    private SoapuiContext context;

    private SoapuiContext.RestRequestContext create;
    private SoapuiContext.RestRequestContext read;

    @BeforeAll
    void init() {
        System.out.println("init");
        context = new eu.k5.dread.soapui.SoapuiContext();

        SoapuiContext.PropertyHolder properites = context.propertiesStep("Properites");

        create = context.requestStep("create").url(PREFIX).request("{ \"payload\": \"String\"}");
        read = context.requestStep("read").url(PREFIX + "/${#Project#projectProperty}");
    }


    @Test
    public void create(TestReporter reporter) {

        System.out.println(reporter.getClass().getName());
        reporter.publishEntry("create", "GET " + create.url());
        ValidatableResponse validatableResponse = given().header("Content-Type", "application/json").body(create.request()).post(create.url()).then().statusCode(200);
        Response response = validatableResponse.extract().response();

        String response1 = response.asString();
        System.out.println(response1);
        create.response(response1);


    }

    @Test
    @DisplayName("transfer")
    @Dependent.DependsOn("create")
    public void transfer() {
        context.transfer("#create#Response", "$.id", "JSONPATH").to("#Project#projectProperty");
        //   System.out.println(transferd);
    }

    @Test
    @Dependent.DependsOn("transfer")
    public void read() {


        System.out.println(read.url());

        Response response = given()
                .header("Accept", "application/json")
                .get(read.url())
                .then().statusCode(200).extract().response();
        read.status(response.statusCode()).response(response.asString());
        read.assertJsonPathExists(".id", "true");
    }
}
