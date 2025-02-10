package com.dmdev.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dmdev.dto.CreateSubscriptionDto;
import com.dmdev.entity.Provider;
import com.dmdev.entity.Status;
import com.dmdev.entity.Subscription;
import com.dmdev.integration.IntegrationTestBase;
import com.dmdev.mapper.CreateSubscriptionMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.Test;

class SubscriptionDaoIT extends IntegrationTestBase {

  private final CreateSubscriptionMapper subscriptionMapper = CreateSubscriptionMapper.getInstance();
  private final SubscriptionDao subscriptionDao = new SubscriptionDao();

  @Test
  void findAll_subscriptionsFound_returnAllSubscriptions() {
    Subscription firstSubscription = subscriptionDao.insert(
        subscriptionMapper.map(createSubscriptionDto(1, "Gold"))
    );
    Subscription secondSubscription = subscriptionDao.insert(
        subscriptionMapper.map(createSubscriptionDto(2, "Platinum"))
    );

    List<Subscription> expectedResult = List.of(firstSubscription, secondSubscription);
    List<Subscription> actualResult = subscriptionDao.findAll();

    assertAll(() -> assertThat(actualResult).hasSize(expectedResult.size()),
        () -> assertThat(actualResult).hasSameElementsAs(expectedResult));
  }

  @Test
  void findAll_noSubscriptions_returnEmptyList() {
    List<Subscription> actualResult = subscriptionDao.findAll();
    assertThat(actualResult).isEmpty();
  }

  @Test
  void findAll_insertDuplicateSubscription_throwsConstraintViolationExc() {
    Subscription subscription = subscriptionMapper.map(createSubscriptionDto(1, "Gold"));

    subscriptionDao.insert(subscription);

    assertThrows(JdbcSQLIntegrityConstraintViolationException.class,
        () -> subscriptionDao.insert(subscription));
  }

  @Test
  void findById_subscriptionExist_returnSubscription() {
    Subscription expectedResult = subscriptionDao.insert(
        subscriptionMapper.map(createSubscriptionDto(1, "Gold"))
    );

    Optional<Subscription> actualResult = subscriptionDao.findById(expectedResult.getId());

    assertThat(actualResult)
        .isPresent()
        .hasValue(expectedResult);
  }

  @Test
  void findById_subscriptionNotExist_returnEmptyOpt() {
    Integer nonExistingId = 999;
    Optional<Subscription> actualResult = subscriptionDao.findById(nonExistingId);
    assertThat(actualResult).isEmpty();
  }

  @Test
  void findById_passedNullKey_returnEmptyOpt() {
    Optional<Subscription> actualResult = subscriptionDao.findById(null);
    assertThat(actualResult).isEmpty();
  }

  @Test
  void delete_subscriptionExist_returnTrue() {
    Subscription savedSubscription = subscriptionDao.insert(
        subscriptionMapper.map(createSubscriptionDto(1, "Gold"))
    );
    Integer subscriptionId = savedSubscription.getId();

    boolean isDeleted = subscriptionDao.delete(savedSubscription.getId());
    Optional<Subscription> foundSubscriptionOpt = subscriptionDao.findById(subscriptionId);

    assertAll(() -> assertThat(isDeleted).isTrue(),
        () -> assertThat(foundSubscriptionOpt).isEmpty());
  }

  @Test
  void delete_subscriptionNotExist_returnFalse() {
    Integer nonExistingId = 222;
    boolean isDeleted = subscriptionDao.delete(nonExistingId);
    assertThat(isDeleted).isFalse();
  }

  @Test
  void update_subscriptionExist_returnUpdatedSubscription() {
    Subscription savedSubscription = subscriptionDao.insert(
        subscriptionMapper.map(createSubscriptionDto(1, "Gold"))
    );

    savedSubscription.setStatus(Status.CANCELED);
    subscriptionDao.update(savedSubscription);
    Subscription actualSubscription = subscriptionDao.findById(savedSubscription.getId())
        .orElseThrow(() -> new AssertionError("Updated subscription not found"));

    assertThat(actualSubscription).isEqualTo(savedSubscription);
  }

  @Test
  void update_subscriptionNotExist_returnEmptyOpt() {
    Subscription subscription = subscriptionMapper.map(createSubscriptionDto(23, "George"));
    Subscription updatedSubscription = subscriptionDao.update(subscription);

    Optional<Subscription> foundSubscriptionOpt = subscriptionDao.findById(
        updatedSubscription.getId());

    assertThat(foundSubscriptionOpt).isEmpty();
  }

  @Test
  void update_subscriptionDuplicateUserIdAndName_throwsConstraintViolation() {
    Subscription firstSubscription = subscriptionMapper.map(createSubscriptionDto(1, "Gold"));
    Subscription secondSubscription = subscriptionMapper.map(createSubscriptionDto(2, "Boris"));

    subscriptionDao.insert(firstSubscription);
    subscriptionDao.insert(secondSubscription);

    secondSubscription.setUserId(firstSubscription.getUserId());
    secondSubscription.setName(firstSubscription.getName());

    assertThrows(JdbcSQLIntegrityConstraintViolationException.class,
        () -> subscriptionDao.update(secondSubscription));
  }

  @Test
  void update_subscriptionDuplicateUserId_returnUpdatedSubscription() {
    Subscription firstSubscription = subscriptionMapper.map(createSubscriptionDto(1, "Gold"));
    Subscription secondSubscription = subscriptionMapper.map(createSubscriptionDto(2, "Boris"));

    subscriptionDao.insert(firstSubscription);
    subscriptionDao.insert(secondSubscription);

    secondSubscription.setUserId(firstSubscription.getUserId());

    Subscription updatedSecondSubscription = assertDoesNotThrow(
        () -> subscriptionDao.update(secondSubscription));
    Optional<Subscription> foundSecondSubscriptionOpt = subscriptionDao.findById(
        secondSubscription.getId());

    assertThat(foundSecondSubscriptionOpt)
        .isPresent()
        .hasValue(updatedSecondSubscription);
  }

  @Test
  void update_subscriptionDuplicateName_returnUpdatedSubscription() {
    Subscription firstSubscription = subscriptionMapper.map(createSubscriptionDto(1, "Gold"));
    Subscription secondSubscription = subscriptionMapper.map(createSubscriptionDto(2, "Boris"));

    subscriptionDao.insert(firstSubscription);
    subscriptionDao.insert(secondSubscription);

    secondSubscription.setName(firstSubscription.getName());

    Subscription updatedSecondSubscription = assertDoesNotThrow(
        () -> subscriptionDao.update(secondSubscription));
    Optional<Subscription> foundSecondSubscriptionOpt = subscriptionDao.findById(
        secondSubscription.getId());

    assertThat(foundSecondSubscriptionOpt)
        .isPresent()
        .hasValue(updatedSecondSubscription);
  }

  @Test
  void update_subscriptionStatusAndProvider_returnUpdatedSubscription() {
    Subscription savedSubscription = subscriptionDao.insert(
        subscriptionMapper.map(createSubscriptionDto(1, "Gold"))
    );

    savedSubscription.setStatus(Status.EXPIRED);
    savedSubscription.setProvider(Provider.GOOGLE);

    Subscription updatedSubscription = subscriptionDao.update(savedSubscription);
    Optional<Subscription> foundSubscriptionOpt = subscriptionDao.findById(
        updatedSubscription.getId());

    assertThat(foundSubscriptionOpt).isPresent()
        .hasValue(savedSubscription);
  }


  @Test
  void insert_subscriptionValid_returnSavedSubscription() {
    Subscription savedSubscription = subscriptionDao.insert(
        subscriptionMapper.map(createSubscriptionDto(1, "Gold"))
    );

    Optional<Subscription> foundSubscriptionOpt = subscriptionDao.findById(
        savedSubscription.getId());

    assertThat(foundSubscriptionOpt)
        .isPresent()
        .hasValue(savedSubscription);
  }

  @Test
  void insert_subscriptionDuplicate_throwsConstraintViolation() {
    Subscription subscription = subscriptionMapper.map(createSubscriptionDto(1, "Gold"));
    subscriptionDao.insert(subscription);
    assertThrows(JdbcSQLIntegrityConstraintViolationException.class,
        () -> subscriptionDao.insert(subscription));
  }

  @Test
  void findByUserId_userIdExist_returnSubscriptions() {
    Subscription subscription = subscriptionMapper.map(createSubscriptionDto(1, "Gold"));
    Subscription savedSubscription = subscriptionDao.insert(subscription);

    List<Subscription> foundSubscriptionOpt = subscriptionDao.findByUserId(
        subscription.getUserId());

    assertThat(foundSubscriptionOpt)
        .isNotEmpty()
        .containsExactly(savedSubscription);
  }

  @Test
  void findByUserId_userIdNotExist_returnEmptyList() {
    List<Subscription> foundSubscriptionOpt = subscriptionDao.findByUserId(1);

    assertThat(foundSubscriptionOpt).isEmpty();
  }

  @Test
  void findByUserId_existMultipleSubscriptions_returnSubscriptions() {
    Subscription firstSubscription = subscriptionMapper.map(createSubscriptionDto(1, "Gold"));
    Subscription secondSubscription = subscriptionMapper.map(createSubscriptionDto(1, "Platinum"));

    Subscription firstSavedSubscription = subscriptionDao.insert(firstSubscription);
    Subscription secondSavedSubscription = subscriptionDao.insert(secondSubscription);

    List<Subscription> foundSubscriptionOpt = subscriptionDao.findByUserId(1);

    assertThat(foundSubscriptionOpt)
        .isNotEmpty()
        .hasSize(2)
        .hasSameElementsAs(List.of(firstSavedSubscription, secondSavedSubscription));
  }

  private CreateSubscriptionDto createSubscriptionDto(Integer userId, String name) {
    return CreateSubscriptionDto.builder()
        .userId(userId)
        .name(name)
        .provider(Provider.APPLE.name())
        .expirationDate(Instant.parse("2025-04-09T12:00:00Z"))
        .build();
  }
}
