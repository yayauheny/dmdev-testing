package com.dmdev.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.dmdev.dao.SubscriptionDao;
import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.exception.SubscriptionException;
import com.dmdev.exception.ValidationException;
import com.dmdev.mapper.CreateSubscriptionMapper;
import com.dmdev.validator.CreateSubscriptionValidator;
import com.dmdev.validator.ValidationResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class SubscriptionServiceTest {

  private SubscriptionDao subscriptionDao;
  private CreateSubscriptionMapper createSubscriptionMapper;
  private CreateSubscriptionValidator createSubscriptionValidator;
  private Clock clock = Clock.fixed(Instant.parse("2024-02-10T10:00:00Z"), ZoneOffset.UTC);
  private SubscriptionService subscriptionService;

  @BeforeEach
  void setup() {
    subscriptionDao = Mockito.spy(SubscriptionDao.class);
    createSubscriptionMapper = Mockito.spy(CreateSubscriptionMapper.class);
    createSubscriptionValidator = Mockito.spy(CreateSubscriptionValidator.class);
    subscriptionService = new SubscriptionService(
        subscriptionDao,
        createSubscriptionMapper,
        createSubscriptionValidator,
        clock
    );
  }

  @ParameterizedTest
  @MethodSource("invalidCreateSubscriptions")
  void upsertPassedInvalidSubscriptionDtoThrowsValidationExc(
      CreateSubscriptionDto subscriptionDto) {
    assertThrows(ValidationException.class, () -> subscriptionService.upsert(subscriptionDto));
  }

  @Test
  void upsert_notFoundSubscriptionsByUserId_createSubscription() {
    CreateSubscriptionDto createSubscriptionDto = createSubscriptionDto(1, "Gold");
    Subscription expectedSubscription = Subscription.builder()
        .id(1)
        .userId(createSubscriptionDto.getUserId())
        .name(createSubscriptionDto.getName())
        .provider(Provider.APPLE)
        .expirationDate(createSubscriptionDto.getExpirationDate())
        .status(Status.ACTIVE)
        .build();

    doReturn(Collections.emptyList()).when(subscriptionDao).findByUserId(1);
    doReturn(new ValidationResult()).when(createSubscriptionValidator)
        .validate(createSubscriptionDto);
    doReturn(expectedSubscription).when(createSubscriptionMapper)
        .map(createSubscriptionDto);
    doReturn(expectedSubscription).when(subscriptionDao)
        .upsert(expectedSubscription);

    Subscription actualResult = subscriptionService.upsert(createSubscriptionDto);

    verify(createSubscriptionMapper).map(createSubscriptionDto);
    verify(subscriptionDao).upsert(any());

    assertThat(actualResult)
        .isNotNull()
        .isEqualTo(expectedSubscription);
  }

  @Test
  void upsert_subscriptionsEqualUserIdDiffName_createSubscription() {
    CreateSubscriptionDto createSubscriptionDto = createSubscriptionDto(1, "Gold");
    CreateSubscriptionDto firstCreateSubscription = createSubscriptionDto(1, "Platinum");
    Subscription expectedSubscription = Subscription.builder()
        .userId(createSubscriptionDto.getUserId())
        .name(createSubscriptionDto.getName())
        .provider(Provider.APPLE)
        .expirationDate(createSubscriptionDto.getExpirationDate())
        .status(Status.ACTIVE)
        .build();
    Subscription firstSubscription = Subscription.builder()
        .id(1)
        .userId(firstCreateSubscription.getUserId())
        .name(firstCreateSubscription.getName())
        .provider(Provider.APPLE)
        .expirationDate(firstCreateSubscription.getExpirationDate())
        .status(Status.ACTIVE)
        .build();

    doReturn(List.of(firstSubscription)).when(subscriptionDao).findByUserId(1);
    doReturn(new ValidationResult()).when(createSubscriptionValidator)
        .validate(createSubscriptionDto);
    doReturn(expectedSubscription).when(createSubscriptionMapper).map(createSubscriptionDto);
    doReturn(expectedSubscription).when(subscriptionDao).upsert(expectedSubscription);

    Subscription actualResult = subscriptionService.upsert(createSubscriptionDto);

    verify(createSubscriptionMapper).map(createSubscriptionDto);
    verify(subscriptionDao).upsert(expectedSubscription);
    assertThat(actualResult).isEqualTo(expectedSubscription);
  }

  @Test
  void upsert_subscriptionsEqualUserIdNameDiffProvider_createSubscription() {
    CreateSubscriptionDto createSubscriptionDto = CreateSubscriptionDto.builder()
        .userId(1)
        .name("Gold")
        .provider(Provider.GOOGLE.name())
        .expirationDate(Instant.parse("2025-04-09T12:00:00Z"))
        .build();
    CreateSubscriptionDto firstCreateSubscription = CreateSubscriptionDto.builder()
        .userId(1)
        .name("Gold")
        .provider(Provider.APPLE.name())
        .expirationDate(Instant.parse("2025-04-09T12:00:00Z"))
        .build();
    Subscription expectedSubscription = Subscription.builder()
        .userId(createSubscriptionDto.getUserId())
        .name(createSubscriptionDto.getName())
        .provider(Provider.GOOGLE)
        .expirationDate(createSubscriptionDto.getExpirationDate())
        .status(Status.ACTIVE)
        .build();
    Subscription firstSubscription = Subscription.builder()
        .id(1)
        .userId(firstCreateSubscription.getUserId())
        .name(firstCreateSubscription.getName())
        .provider(Provider.APPLE)
        .expirationDate(firstCreateSubscription.getExpirationDate())
        .status(Status.ACTIVE)
        .build();

    doReturn(List.of(firstSubscription)).when(subscriptionDao).findByUserId(1);
    doReturn(new ValidationResult()).when(createSubscriptionValidator)
        .validate(createSubscriptionDto);
    doReturn(expectedSubscription).when(createSubscriptionMapper).map(createSubscriptionDto);
    doReturn(expectedSubscription).when(subscriptionDao).upsert(expectedSubscription);

    Subscription actualResult = subscriptionService.upsert(createSubscriptionDto);

    verify(createSubscriptionMapper).map(createSubscriptionDto);
    verify(subscriptionDao).upsert(expectedSubscription);
    assertThat(actualResult).isEqualTo(expectedSubscription);
  }

  @Test
  void upsert_subscriptionsEqualUserIdNameProvider_updateSubscription() {
    CreateSubscriptionDto createSubscriptionDto = createSubscriptionDto(1, "Gold");
    CreateSubscriptionDto firstCreateSubscription = createSubscriptionDto(1, "Platinum");
    Subscription expectedSubscription = Subscription.builder()
        .userId(1)
        .name(createSubscriptionDto.getName())
        .provider(Provider.APPLE)
        .expirationDate(createSubscriptionDto.getExpirationDate().plus(Duration.ofDays(100)))
        .status(Status.ACTIVE)
        .build();
    Subscription firstSubscription = Subscription.builder()
        .id(1)
        .userId(firstCreateSubscription.getUserId())
        .name(firstCreateSubscription.getName())
        .provider(Provider.APPLE)
        .expirationDate(firstCreateSubscription.getExpirationDate())
        .status(Status.CANCELED)
        .build();

    doReturn(List.of(firstSubscription)).when(subscriptionDao).findByUserId(1);
    doReturn(new ValidationResult()).when(createSubscriptionValidator)
        .validate(createSubscriptionDto);
    doReturn(expectedSubscription).when(createSubscriptionMapper).map(createSubscriptionDto);
    doReturn(expectedSubscription).when(subscriptionDao).upsert(expectedSubscription);

    Subscription actualResult = subscriptionService.upsert(createSubscriptionDto);

    verify(subscriptionDao).upsert(expectedSubscription);
    assertThat(actualResult).isEqualTo(expectedSubscription);
  }

  @Test
  void cancel_subscriptionIsActive_subscriptionIsCancelled() {
    Subscription subscription = Subscription.builder()
        .id(1)
        .userId(1)
        .name("Gold")
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.parse("2025-04-09T12:00:00Z"))
        .status(Status.ACTIVE)
        .build();
    Subscription expectedResult = Subscription.builder()
        .id(1)
        .userId(1)
        .name("Gold")
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.parse("2025-04-09T12:00:00Z"))
        .status(Status.CANCELED)
        .build();

    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(1);
    doReturn(expectedResult).when(subscriptionDao).update(expectedResult);

    subscriptionService.cancel(1);

    verify(subscriptionDao).update(expectedResult);
  }

  @Test
  void cancel_subscriptionNotFound_throwsIllegalArgumentException() {
    doReturn(Optional.empty()).when(subscriptionDao).findById(1);
    assertThrows(IllegalArgumentException.class, () -> subscriptionService.cancel(1));
  }

  @Test
  void cancel_expiredSubscription_throwsSubscriptionException() {
    Subscription subscription = Subscription.builder()
        .id(1)
        .userId(1)
        .name("Gold")
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.parse("2025-04-09T12:00:00Z"))
        .status(Status.EXPIRED)
        .build();

    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(1);

    assertThrows(SubscriptionException.class, () -> subscriptionService.cancel(1));
  }

  @Test
  void expire_subscriptionNotFound_throwsIllegalArgumentException() {
    doReturn(Optional.empty()).when(subscriptionDao).findById(1);
    assertThrows(IllegalArgumentException.class, () -> subscriptionService.expire(1));
  }

  @Test
  void expire_expiredSubscription_throwsSubscriptionException() {
    Subscription subscription = Subscription.builder()
        .id(1)
        .userId(1)
        .name("Gold")
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.now(clock))
        .status(Status.EXPIRED)
        .build();

    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(1);

    assertThrows(SubscriptionException.class, () -> subscriptionService.expire(1));
  }

  @Test
  void expire_subscriptionIsActive_subscriptionIsExpired() {
    Subscription subscription = Subscription.builder()
        .id(1)
        .userId(1)
        .name("Gold")
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.now(clock).plus(Duration.ofDays(10)))
        .status(Status.ACTIVE)
        .build();
    Subscription expectedResult = Subscription.builder()
        .id(1)
        .userId(1)
        .name("Gold")
        .provider(Provider.GOOGLE)
        .expirationDate(Instant.now(clock))
        .status(Status.EXPIRED)
        .build();

    doReturn(Optional.of(subscription)).when(subscriptionDao).findById(1);
    doReturn(expectedResult).when(subscriptionDao).update(expectedResult);

    subscriptionService.expire(1);

    verify(subscriptionDao).update(expectedResult);
  }

  private CreateSubscriptionDto createSubscriptionDto(Integer userId, String name) {
    return CreateSubscriptionDto.builder()
        .userId(userId)
        .name(name)
        .provider(Provider.APPLE.name())
        .expirationDate(Instant.parse("2025-04-09T12:00:00Z"))
        .build();
  }

  static Stream<Arguments> invalidCreateSubscriptions() {
    return Stream.of(
        arguments(
            named("Invalid user id",
                CreateSubscriptionDto.builder()
                    .userId(null)
                    .name("Platinum")
                    .provider(Provider.APPLE.name())
                    .expirationDate(Instant.now().plus(Duration.ofDays(30)))
                    .build()
            )
        ),
        arguments(
            named("Invalid subscription name",
                CreateSubscriptionDto.builder()
                    .userId(1)
                    .name("")
                    .provider(Provider.APPLE.name())
                    .expirationDate(Instant.now().plus(Duration.ofDays(30)))
                    .build()
            )
        ),
        arguments(
            named("Invalid provider name",
                CreateSubscriptionDto.builder()
                    .userId(1)
                    .name("Extra")
                    .provider("non-existing provider")
                    .expirationDate(Instant.now().plus(Duration.ofDays(30)))
                    .build()
            )
        ),
        arguments(
            named("Invalid expiration date",
                CreateSubscriptionDto.builder()
                    .userId(1)
                    .name("Extra")
                    .provider(Provider.APPLE.name())
                    .expirationDate(Instant.now().minus(Duration.ofDays(1)))
                    .build()
            )
        ),
        arguments(
            named("Empty expiration date",
                CreateSubscriptionDto.builder()
                    .userId(1)
                    .name("Extra")
                    .provider(Provider.APPLE.name())
                    .expirationDate(null)
                    .build()
            )
        )
    );
  }
}
