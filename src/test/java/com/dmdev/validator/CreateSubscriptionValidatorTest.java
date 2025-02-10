package com.dmdev.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CreateSubscriptionValidatorTest {

  private final CreateSubscriptionValidator validator = CreateSubscriptionValidator.getInstance();

  @Test
  void validate_userValid_returnEmptyErrorList() {
    CreateSubscriptionDto subscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("Extra")
        .provider(Provider.APPLE.name())
        .expirationDate(Instant.now().plus(Duration.ofDays(30)))
        .build();
    ValidationResult actualResult = validator.validate(subscriptionDto);

    assertThat(actualResult.getErrors()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("invalidCreateSubscriptionDto")
  void validate_userInvalid_throwsError(CreateSubscriptionDto subscriptionDto, Error expectedError) {
    ValidationResult actualResult = validator.validate(subscriptionDto);

    assertThat(actualResult.getErrors()).hasSize(1);

    Error actualError = actualResult.getErrors().get(0);
    assertEquals(expectedError, actualError);
  }

  static Stream<Arguments> invalidCreateSubscriptionDto() {
    return Stream.of(
        arguments(
            CreateSubscriptionDto.builder()
                .userId(null)
                .name("Platinum")
                .provider(Provider.APPLE.name())
                .expirationDate(Instant.now().plus(Duration.ofDays(30)))
                .build(),
            Error.of(100, "userId is invalid")
        ),
        arguments(
            CreateSubscriptionDto.builder()
                .userId(1)
                .name("")
                .provider(Provider.APPLE.name())
                .expirationDate(Instant.now().plus(Duration.ofDays(30)))
                .build(),
            Error.of(101, "name is invalid")
        ),
        arguments(
            CreateSubscriptionDto.builder()
                .userId(1)
                .name("Extra")
                .provider("non-existing provider")
                .expirationDate(Instant.now().plus(Duration.ofDays(30)))
                .build(),
            Error.of(102, "provider is invalid")
        ),
        arguments(
            CreateSubscriptionDto.builder()
                .userId(1)
                .name("Extra")
                .provider(Provider.APPLE.name())
                .expirationDate(Instant.now().minus(Duration.ofDays(1)))
                .build(),
            Error.of(103, "expirationDate is invalid")
        ),
        arguments(
            CreateSubscriptionDto.builder()
                .userId(1)
                .name("Extra")
                .provider(Provider.APPLE.name())
                .expirationDate(null)
                .build(),
            Error.of(103, "expirationDate is invalid")
        )
    );
  }
}
