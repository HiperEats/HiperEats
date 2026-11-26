# Added by HE-46 (Cucumber setup), a task, not a story: a @HE-n tag here would link this
# scenario to it in Xray as if it covered a story (HE-41).
@platform
Feature: Platform status
  Checks that the acceptance tests reach the running backend.
  It keeps the Cucumber setup exercised while the story scenarios are still @wip.

  Scenario: The backend reports that it is up
    When a client asks for the platform status
    Then the backend reports that it is up
