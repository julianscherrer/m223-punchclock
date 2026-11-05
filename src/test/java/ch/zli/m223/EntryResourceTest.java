package ch.zli.m223;

import ch.zli.m223.repository.EntryRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.hasItem;

@QuarkusTest
public class EntryResourceTest {

    @Inject
    EntryRepository entryRepository;

    @BeforeEach
    void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> entryRepository.deleteAll());
    }

    @Test
    public void testEntryEndpoints() {
        given()
          .when().get("/entries")
          .then()
             .statusCode(200)
             .body(is("[]"));

        Long id = given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-29T08:00:00","checkOut":"2026-09-29T17:00:00"}
              """)
          .when().post("/entries")
          .then()
             .statusCode(200)
             .body("checkIn", is("2026-09-29T08:00:00"))
             .body("checkOut", is("2026-09-29T17:00:00"))
             .extract().jsonPath().getLong("id");

        given()
          .when().get("/entries")
          .then()
             .statusCode(200)
             .body("size()", is(1))
             .body("[0].id", is(id.intValue()))
             .body("[0].checkIn", is("2026-09-29T08:00:00"))
             .body("[0].checkOut", is("2026-09-29T17:00:00"));
    }

    @Test
    public void testUpdateEntry() {
        Long id = createEntry("2026-09-29T08:00:00", "2026-09-29T17:00:00");

        given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-30T09:00:00","checkOut":"2026-09-30T18:00:00"}
              """)
          .when().put("/entries/" + id)
          .then()
             .statusCode(200)
             .body("id", is(id.intValue()))
             .body("checkIn", is("2026-09-30T09:00:00"))
             .body("checkOut", is("2026-09-30T18:00:00"));

        given()
          .when().get("/entries")
          .then()
             .statusCode(200)
             .body("find { it.id == " + id + " }.checkIn", is("2026-09-30T09:00:00"))
             .body("find { it.id == " + id + " }.checkOut", is("2026-09-30T18:00:00"));
    }

    @Test
    public void testUpdateUnknownEntryReturns404() {
        given()
          .contentType("application/json")
          .body("""
              {"checkIn":"2026-09-30T09:00:00","checkOut":"2026-09-30T18:00:00"}
              """)
          .when().put("/entries/999999")
          .then()
             .statusCode(404);
    }

    @Test
    public void testDeleteEntry() {
        Long id = createEntry("2026-10-01T08:00:00", "2026-10-01T17:00:00");

        given()
          .when().delete("/entries/" + id)
          .then()
             .statusCode(204);

        given()
          .when().get("/entries")
          .then()
             .statusCode(200)
             .body("id", not(hasItem(id.intValue())));
    }

    @Test
    public void testDeleteUnknownEntryReturns404() {
        given()
          .when().delete("/entries/999999")
          .then()
             .statusCode(404);
    }

    private Long createEntry(String checkIn, String checkOut) {
        return given()
          .contentType("application/json")
          .body("{\"checkIn\":\"" + checkIn + "\",\"checkOut\":\"" + checkOut + "\"}")
          .when().post("/entries")
          .then()
             .statusCode(200)
             .extract().jsonPath().getLong("id");
    }

}