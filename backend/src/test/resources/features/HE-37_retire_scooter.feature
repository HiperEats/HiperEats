@HE-37
Feature: Retire scooter
  As an operator, I want to retire a scooter, so that a scooter beyond repair is never offered again.

  Background:
    Given a user logged in with the role operator

  Scenario: An inactive scooter is retired
    Given a scooter that is inactive
    When the operator retires the scooter
    Then the scooter is retired

  Scenario Outline: Only an inactive scooter can be retired
    Given a scooter that is <state>
    When the operator retires the scooter
    Then the change is refused
    And the scooter is still <state>

    Examples:
      | state       |
      | available   |
      | reserved    |
      | in use      |
      | maintenance |
      | retired     |

  # Needs the step "the operator activates the scooter" of HE-30; remove @wip when HE-30 is in.
  @wip
  Scenario: A retired scooter never changes state again
    Given a scooter that is retired
    When the operator activates the scooter
    Then the change is refused
    And the scooter is still retired
