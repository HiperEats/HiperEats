@HE-36
Feature: Deactivate account
  As an admin, I want to deactivate an account, so that people who leave lose access.

  Background:
    Given a user logged in with the role admin
    And an active courier account "rita@example.test" with password "Secret-123"

  Scenario: A deactivated user cannot log in
    When the admin deactivates the account "rita@example.test"
    Then "rita@example.test" cannot log in with password "Secret-123"

  Scenario: A user who is already logged in loses access at once
    Given "rita@example.test" is logged in with password "Secret-123"
    When the admin deactivates the account "rita@example.test"
    Then the access token of "rita@example.test" is no longer accepted

  Scenario: Only an admin can deactivate accounts
    Given a user logged in with the role operator
    When the user tries to deactivate the account "rita@example.test"
    Then the request is forbidden
    And "rita@example.test" can still log in with password "Secret-123"

  # Uses come with the booking stories (HE-32, HE-34).
  @wip
  Scenario: The past bookings of a deactivated user stay in the history
    Given "rita@example.test" has 3 past uses
    When the admin deactivates the account "rita@example.test"
    Then the usage history of store "Aveiro" still has the 3 uses of "rita@example.test"
