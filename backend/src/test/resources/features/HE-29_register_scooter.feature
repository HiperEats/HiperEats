@HE-29 @wip
Feature: Register scooter
  As an operator, I want to register a scooter at a store, so that it joins that store's fleet.

  Background:
    Given a user logged in with the role operator

  Scenario: A new scooter starts inactive
    Given a store with capacity 10 and 0 scooters
    When the operator registers a standard scooter at that store
    Then the scooter is registered at that store
    And the scooter is inactive

  @BR13
  Scenario: The scooter that fills the store is accepted
    Given a store with capacity 10 and 9 scooters
    When the operator registers a standard scooter at that store
    Then the scooter is registered at that store
    And the store has 10 scooters

  @BR13
  Scenario: A scooter beyond the store capacity is refused
    Given a store with capacity 10 and 10 scooters
    When the operator registers a standard scooter at that store
    Then the registration is refused because the store is at capacity
    And the store has 10 scooters
