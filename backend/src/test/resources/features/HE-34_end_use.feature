@HE-34 @wip
Feature: End use
  As a courier, I want to end my use at the store, so that the scooter is available again.

  Background:
    Given a courier who works at store "Aveiro"
    And the courier is logged in
    And the courier has an active use of scooter "AV-01" from store "Aveiro"

  Scenario: The use ends with the scooter locked at its store
    Given scooter "AV-01" reports that it is locked 10 m from store "Aveiro" with battery 60 %
    When the courier ends the use
    Then the use ends
    And scooter "AV-01" is available

  @BR6
  Scenario: A use cannot end at another store
    Given scooter "AV-01" reports that it is locked at store "Ílhavo"
    When the courier ends the use
    Then the end of the use is refused
    And scooter "AV-01" is still in use

  @BR12
  Scenario Outline: The scooter must be locked within the parking area of its store
    Given scooter "AV-01" reports that it is locked <distance> m from store "Aveiro" with battery 60 %
    When the courier ends the use
    Then the use <result>

    Examples:
      | distance | result      |
      | 50       | ends        |
      | 51       | is refused  |

  @BR12
  Scenario: A use cannot end while the scooter is unlocked
    Given scooter "AV-01" reports that it is unlocked 10 m from store "Aveiro"
    When the courier ends the use
    Then the end of the use is refused
    And scooter "AV-01" is still in use

  @BR7
  Scenario Outline: Overtime after the grace period is charged to the chain
    Given the use has <overtime> minutes of overtime
    And scooter "AV-01" reports that it is locked 10 m from store "Aveiro" with battery 60 %
    When the courier ends the use
    Then the use ends
    And the chain is charged € <charge> for overtime
    And the courier is charged nothing

    # Grace 15 min, then €2 per started 15 min.
    Examples:
      | overtime | charge |
      | 15       | 0      |
      | 16       | 2      |
      | 30       | 2      |
      | 31       | 4      |

  @BR8
  Scenario Outline: A scooter returned at or below the alert level goes to maintenance
    Given the low-battery alert level is 15 %
    And scooter "AV-01" reports that it is locked 10 m from store "Aveiro" with battery <battery> %
    When the courier ends the use
    Then the use ends
    And scooter "AV-01" is <state>

    Examples:
      | battery | state                                   |
      | 16      | available                               |
      | 15      | in maintenance with an open ticket      |
