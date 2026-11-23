@HE-33 @wip
Feature: Reserve scooter
  As a courier, I want to reserve a scooter, so that it is waiting for me when the order is ready.

  Background:
    Given a courier who works at store "Aveiro" with a shift from 18:00 to 22:00
    And the courier is logged in
    And store "Aveiro" has an available standard scooter "AV-01" with battery 80 %
    And the reservation validity is 15 minutes

  Scenario: Other couriers no longer see a reserved scooter
    Given the time is 19:00
    When the courier reserves "AV-01"
    Then scooter "AV-01" is reserved
    And another courier of store "Aveiro" does not see "AV-01" among the available scooters

  @BR5
  Scenario: The use starts at the last moment of the reservation
    Given the courier reserved "AV-01" at 19:00:00
    When the courier starts the use of "AV-01" at 19:15:00
    Then the use starts

  @BR5
  Scenario: The reservation expires when the use does not start in time
    Given the courier reserved "AV-01" at 19:00:00
    When the time is 19:15:01
    Then the reservation has expired
    And scooter "AV-01" is available
