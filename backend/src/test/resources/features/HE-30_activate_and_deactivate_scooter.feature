@HE-30
Feature: Activate and deactivate scooter
  As an operator, I want to activate and deactivate a scooter, so that only scooters fit for use can be booked.

  Background:
    Given a user logged in with the role operator

  Scenario: An inactive scooter is activated
    Given a scooter that is inactive
    When the operator activates the scooter
    Then the scooter is available

  Scenario Outline: Only an inactive scooter can be activated
    Given a scooter that is <state>
    When the operator activates the scooter
    Then the change is refused
    And the scooter is still <state>

    Examples:
      | state       |
      | available   |
      | in use      |
      | maintenance |
      | retired     |

  Scenario Outline: An available scooter or one in maintenance is deactivated
    Given a scooter that is <state>
    When the operator deactivates the scooter
    Then the scooter is inactive

    Examples:
      | state       |
      | available   |
      | maintenance |

  Scenario: A scooter in use cannot be deactivated
    Given a scooter that is in use
    When the operator deactivates the scooter
    Then the change is refused
    And the scooter is still in use
