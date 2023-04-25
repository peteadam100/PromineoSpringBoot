
/**
 * 
 */
package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.concurrent.ConcurrentRuntimeException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.jdbc.JdbcTestUtils;
import com.promineotech.jeep.Constants;
import com.promineotech.jeep.controller.support.FetchJeepTestSupport;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.DefaultJeepSalesService;
import com.promineotech.jeep.service.JeepSalesService;

class FetchJeepTest extends FetchJeepTestSupport {

  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  // @formatter:off
  @Sql(scripts = {
      "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"
      }, 
      config = @SqlConfig(encoding = "utf-8"))
  // @formatter:on
  @Nested
  class tests_that_do_not_pollute_the_application_context extends FetchJeepTestSupport {
    // @Autowired
    // private JdbcTemplate jdbcTemplate;
    //
    // @Test
    // void testDb() {
    // int numrows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "customers");
    // System.out.println("row count = " + numrows);
    // }
    //
    // @Disabled
    @Test
    void test_that_jeeps_are_returned_when_a_valid_model_and_trim_are_supplied() {
      // Given: a valid model, trim, and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Sport";
      String uri = String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);

      // When: a connection is made to the URI
      ResponseEntity<List<Jeep>> response = getRestTemplate().exchange(uri, HttpMethod.GET, null,
          new ParameterizedTypeReference<>() {});

      // Then: a success (OK - 200) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      // And: the actual list returned is the same as the expected list
      List<Jeep> actual = response.getBody();
      List<Jeep> expected = buildExpected();


      // BAD APPROACH HERE. SHOULDN'T MODIFY THE DATA COMING BACK LIKE THIS, EVEN THOUGH IT WOULD
      // PASS THE TEST
      // remove the PK because this is unknown when coming from the database
      // actual.forEach(jeep -> jeep.setModelPK(null));

      // System.out.println(expected);
      assertThat(actual).isEqualTo(expected);

    }

    @Test
    void test_that_an_error_message_is_returned_when_an_unknown_trim_is_supplied() {
      // Given: a valid model, trim, and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Unknown Value";
      String uri = String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);

      // When: a connection is made to the URI
      ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, HttpMethod.GET,
          null, new ParameterizedTypeReference<>() {});

      // Then: a not found (404) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

      // And: an error message is returned
      Map<String, Object> error = response.getBody();

      assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
    void test_that_an_error_message_is_returned_when_an_invalid_value_is_supplied(String model,
        String trim, String reason) {
      // Given: a valid model, trim, and URI
      String uri = String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);

      // When: a connection is made to the URI
      ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, HttpMethod.GET,
          null, new ParameterizedTypeReference<>() {});

      // Then: a bad request (400) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

      // And: an error message is returned
      Map<String, Object> error = response.getBody();

      assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);

    }
  }

  static Stream<Arguments> parametersForInvalidInput() {
    // @formatter:off
    return Stream.of(
        arguments("WRANGLER", "@#$%*", "Trim contains non-alpha-numeric characters"),
        arguments("WRANGLER", "C".repeat(Constants.TRIM_MAX_LENGTH + 1), "Trim length too long"),
        arguments("INVALID", "Sport", "Model is not enum value")
    );
    // @formatter:on
  }



  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  // @formatter:off
  @Sql(scripts = {
      "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"
      }, 
      config = @SqlConfig(encoding = "utf-8"))
  // @formatter:on
  @Nested
  class tests_that_pollute_the_application_context extends FetchJeepTestSupport {
    @MockBean
    private JeepSalesService jeepSalesService;

    @Test
    void test_that_an_unplanned_error_results_in_a_500_status() {
      // Given: a valid model, trim, and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Invalid Trim";
      String uri = String.format("%s?model=%s&trim=%s", getBaseUriForJeeps(), model, trim);
      
      // Force some kind of error just to throw an internal server error (500)
      doThrow(new RuntimeException("Ouch")).when(jeepSalesService).fetchJeeps(model, trim);

      // When: a connection is made to the URI
      ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, HttpMethod.GET,
          null, new ParameterizedTypeReference<>() {});

      // Then: a internal server error (500) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

      // And: an error message is returned
      Map<String, Object> error = response.getBody();

      assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}


