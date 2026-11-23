@HE-36 @wip
Feature: Deactivate account
  As an admin, I want to deactivate an account, so that people who leave lose access.

  Background:
    Given a user logged in with the role admin
    And an active courier account "rita@example.test" with password "Secret-123"

  Scenario: A deactivated user cannot log in
    When the admin deactivates the account "rita@example.test"
    Then "rita@example.test" cannot log in with password "Secret-123"

  Scenario: The past bookings of a deactivated user stay in the history
    Given "rita@example.test" has 3 past uses
    When the admin deactivates the account "rita@example.test"
    Then the usage history of store "Aveiro" still has the 3 uses of "rita@example.test"
