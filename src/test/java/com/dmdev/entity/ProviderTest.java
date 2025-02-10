package com.dmdev.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProviderTest {

  private final List<String> EXISTING_PROVIDER_NAMES = Arrays.stream(Provider.values())
      .map(Provider::name)
      .toList();

  @ParameterizedTest
  @MethodSource("validProviderNames")
  void findByName_providerNameExist_returnProvider(String name) {
    assertAll(
        () -> assertDoesNotThrow(() -> Provider.findByName(name)),
        () -> assertThat(EXISTING_PROVIDER_NAMES).contains(name.toUpperCase())
    );
  }

  @ParameterizedTest
  @MethodSource("invalidProviderNames")
  void findByName_providerNameNotExist_throwsNoSuchElementExc(String name) {
    assertAll(
        () -> assertThrows(NoSuchElementException.class, () -> Provider.findByName(name)),
        () -> assertThat(EXISTING_PROVIDER_NAMES).doesNotContain(name.toUpperCase())
    );
  }

  @ParameterizedTest
  @MethodSource("validProviderNames")
  void findByNameOptValidNameReturnProvider(String name) {
    assertThat(Provider.findByNameOpt(name)).isPresent();
  }

  @ParameterizedTest
  @MethodSource("invalidProviderNames")
  void findByNameOptInvalidNameIsNotPresent(String name) {
    assertThat(Provider.findByNameOpt(name)).isEmpty();
  }

  static Stream<String> validProviderNames() {
    return Arrays.stream(Provider.values()).map(Provider::name);
  }

  static Stream<String> invalidProviderNames() {
    return Stream.of("Meta", "Evil Corp", "Amazon");
  }
}