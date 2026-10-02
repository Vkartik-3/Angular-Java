package com.example.order.presentation;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class OrderResourceTest {

    static final String CUSTOMER_ID = "550e8400-e29b-41d4-a716-446655440001";

    private String token;

    @BeforeEach
    void generateToken() {
        token = Jwt.claims()
                .subject(CUSTOMER_ID)
                .claim("preferred_username", "test-customer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .sign();
    }

    @Test
    void create_emptyItems_returns400() {
        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{\"items\":[]}")
                .when().post("/orders")
                .then().statusCode(400);
    }

    @Test
    void create_zeroQuantity_returns400() {
        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {"items":[{"productId":"%s","quantity":0,"price":34.99}]}
                        """.formatted(UUID.randomUUID()))
                .when().post("/orders")
                .then().statusCode(400);
    }

    @Test
    void create_priceZero_returns400() {
        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {"items":[{"productId":"%s","quantity":1,"price":0.00}]}
                        """.formatted(UUID.randomUUID()))
                .when().post("/orders")
                .then().statusCode(400);
    }

    @Test
    void create_nullProductId_returns400() {
        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {"items":[{"productId":null,"quantity":1,"price":34.99}]}
                        """)
                .when().post("/orders")
                .then().statusCode(400);
    }

    @Test
    void create_missingItemsField_returns400() {
        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/orders")
                .then().statusCode(400);
    }

    @Test
    void getById_unknownOrderId_returns404() {
        given().auth().oauth2(token)
                .when().get("/orders/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void create_noToken_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"items":[{"productId":"%s","quantity":1,"price":34.99}]}
                        """.formatted(UUID.randomUUID()))
                .when().post("/orders")
                .then().statusCode(401);
    }
}