@HE-28
Feature: Log in
  As a user, I want to log in with my account, so that I only see the features of my role.

  Scenario: A user logs in with the right password
    Given an active courier account "rita@example.test" with password "Secret-123"
    When "rita@example.test" logs in with password "Secret-123"
    Then the login succeeds with the role courier

  Scenario: A wrong password is refused
    Given an active courier account "rita@example.test" with password "Secret-123"
    When "rita@example.test" logs in with password "wrong-password"
    Then the login is refused

  Scenario: A request without logging in is refused
    When a client asks for the scooters without logging in
    Then the request is refused as not authenticated

  Scenario Outline: Each role reaches only its own resources
    Given a user logged in with the role <role>
    When the user tries to <action>
    Then the request is <result>

    Examples:
      | role          | action                   | result    |
      | admin         | list the user accounts   | allowed   |
      | courier       | list the user accounts   | forbidden |
      | operator      | list the user accounts   | forbidden |

    # The rules of use have no endpoint until the Client rules customization epic (I4).
    @wip
    Examples:
      | role          | action                   | result    |
      | chain manager | change the rules of use  | allowed   |
      | operator      | change the rules of use  | forbidden |
      | courier       | change the rules of use  | forbidden |
