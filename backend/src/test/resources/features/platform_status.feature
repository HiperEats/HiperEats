@HE-46
Feature: Platform status
  Checks that the acceptance tests reach the running backend.
  It keeps the Cucumber setup exercised while the story scenarios are still @wip.

  Scenario: The backend reports that it is up
    When a client asks for the platform status
    Then the backend reports that it is up
