package eu.k5.dread.restassured;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

public class LottoTest {

    @Test
    public void simpleGet() {
        get("/lotto").then().body("lotto.lottoId", equalTo(5));
    }

    @Test
    public void getWithParams() {
        Response response = given().params("name", "ReturnName").get("/withParams");
        String as = response.getBody().print();
        response.then().statusCode(200).body("name", equalTo("ReturnName"));
    }


    @Test
    public void postWithBody() {
        String body = get("/lotto").then() //
                .body("lotto.lottoId", equalTo(5)) //
                .extract().body().asString();

        given().body(body).post("/lotto").then().body("changed", equalTo("value"));
    }
}
