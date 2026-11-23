@HE-38 @wip
Feature: Cancel reservation
  As a courier, I want to cancel my reservation, so that the scooter goes back to the other couriers.

  @BR5
  Scenario: A cancelled reservation frees the scooter at once
    Given a courier who works at store "Aveiro"
    And the courier is logged in
    And the courier has reserved scooter "AV-01"
    When the courier cancels the reservation
    Then the reservation is cancelled
    And scooter "AV-01" is available
    And another courier of store "Aveiro" sees "AV-01" among the available scooters
