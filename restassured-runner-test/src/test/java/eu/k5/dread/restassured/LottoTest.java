package eu.k5.dread.restassured;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

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

        int[] array = new int[0];


        String body = get("/lotto").then() //
                .body("lotto.lottoId", equalTo(5)) //
                .extract().body().asString();

        given().body(body).post("/lotto").then().body("changed", equalTo("value"));
    }
}
