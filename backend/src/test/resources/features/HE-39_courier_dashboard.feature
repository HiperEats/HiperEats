@HE-39 @wip
Feature: Courier dashboard
  As a courier, I want to see my active and past uses, so that I can check my trips and any overtime.

  Background:
    Given a courier who works at store "Aveiro"
    And the courier is logged in

  Scenario: The courier sees only their own uses, the active one first
    Given the courier has these uses:
      | scooter | started          | ended            |
      | AV-01   | 2026-11-20 12:00 | 2026-11-20 12:40 |
      | AV-02   | 2026-11-21 19:10 | 2026-11-21 19:35 |
      | AV-03   | 2026-11-22 19:40 |                  |
    And another courier has a use of scooter "AV-04"
    When the courier opens the dashboard
    Then the dashboard lists the uses of "AV-03", "AV-02" and "AV-01" in this order
    And the dashboard does not list the use of "AV-04"

  Scenario: Each use shows its duration, distance and overtime charged to the chain
    Given the courier has a past use of 40 min and 12.5 km that ended 16 minutes after the end of their shift
    When the courier opens the dashboard
    Then the use shows a duration of 40 min, a distance of 12.5 km and an overtime charge of € 2 to the chain
