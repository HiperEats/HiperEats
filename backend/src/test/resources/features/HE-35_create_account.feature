@HE-35 @wip
Feature: Create account
  As an admin, I want to create an account with a role, so that each person gets only the access their job needs.

  Background:
    Given a user logged in with the role admin

  Scenario: A courier account belongs to one store
    When the admin creates an account for "rita@example.test" with the role courier at store "Aveiro"
    Then the account is created with the role courier
    And the account belongs to store "Aveiro"

  Scenario: A courier account without a store is refused
    When the admin creates an account for "rita@example.test" with the role courier and no store
    Then the account is refused

  Scenario: An email already in use is refused
    Given an account for "rita@example.test" already exists
    When the admin creates an account for "rita@example.test" with the role courier at store "Aveiro"
    Then the account is refused because the email is already in use

  Scenario: Only an admin can create accounts
    Given a user logged in with the role operator
    When the user creates an account for "new@example.test" with the role courier at store "Aveiro"
    Then the request is forbidden
