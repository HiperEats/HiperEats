@HE-31 @wip
Feature: See available scooters
  As a courier, I want to see the available scooters at my store, so that I can pick one for my delivery.

  Background:
    Given a courier who works at store "Aveiro"
    And the courier is logged in

  Scenario: Only the scooters of the courier's own store are listed
    Given store "Aveiro" has an available standard scooter "AV-01" with battery 80 %
    And store "Ílhavo" has an available standard scooter "IL-01" with battery 80 %
    When the courier asks for the available scooters for a delivery 1 km away
    Then the list contains "AV-01"
    And the list does not contain "IL-01"

  Scenario Outline: Only available scooters are listed
    Given store "Aveiro" has a standard scooter "AV-02" with battery 80 % that is <state>
    When the courier asks for the available scooters for a delivery 1 km away
    Then the list does not contain "AV-02"

    Examples:
      | state       |
      | inactive    |
      | reserved    |
      | in use      |
      | maintenance |
      | retired     |

  @BR1
  Scenario Outline: Only scooters with enough range for the round trip and its margin are listed
    Given store "Aveiro" has an available long-range scooter "AV-LR" with battery <battery> %
    When the courier asks for the available scooters for a delivery 3 km away
    Then the list <result> "AV-LR"

    # Long range is 50 km at full battery; a 3 km delivery needs 2 x 3 km x 1.2 = 7.2 km.
    Examples:
      | battery | result           |
      | 15      | contains         |
      | 14      | does not contain |

  Scenario: Each scooter shows its type and battery level
    Given store "Aveiro" has an available long-range scooter "AV-LR" with battery 60 %
    When the courier asks for the available scooters for a delivery 1 km away
    Then scooter "AV-LR" is shown as long range with battery 60 %
