@HE-32 @wip
Feature: Start use
  As a courier, I want to start using a scooter right away, so that I can leave with the order.

  Background:
    Given a courier who works at store "Aveiro" with a shift from 18:00 to 22:00
    And the courier is logged in
    And store "Aveiro" has an available standard scooter "AV-01" with battery 80 %

  @BR12
  Scenario: The use starts and the platform unlocks the scooter
    Given the time is 19:40
    When the courier starts a use of "AV-01" for a delivery 1 km away
    Then the use starts
    And scooter "AV-01" is in use
    And the platform sends an unlock command to "AV-01"

  @BR3
  Scenario Outline: A use can start only during the courier's shift
    Given the time is <time>
    When the courier starts a use of "AV-01" for a delivery 1 km away
    Then the use <result>

    Examples:
      | time  | result                                     |
      | 17:59 | is refused because it is outside the shift |
      | 18:00 | starts                                     |
      | 22:00 | is refused because it is outside the shift |

  @BR4
  Scenario: A courier with another active booking cannot start a use
    Given the time is 19:40
    And the courier already has an active use of another scooter
    When the courier starts a use of "AV-01" for a delivery 1 km away
    Then the use is refused because the courier already has an active booking
    And scooter "AV-01" is still available
