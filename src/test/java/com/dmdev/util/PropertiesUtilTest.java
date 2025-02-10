package com.dmdev.util;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PropertiesUtilTest {

  @ParameterizedTest
  @MethodSource("applicationProperties")
  void get_keyExist_returnValue(String key, String expectedValue) {
    String actualResult = PropertiesUtil.get(key);
    Assertions.assertEquals(expectedValue, actualResult);
  }

  static Stream<Arguments> applicationProperties() {
    return Stream.of(
        arguments("db.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"),
        arguments("db.user", "sa"),
        arguments("db.password", ""),
        arguments("db.driver", "org.h2.Driver")
    );
  }
}
