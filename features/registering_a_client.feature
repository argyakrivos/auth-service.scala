@clients @registration @client_registration
Feature: Registering a client
  As a user
  I want to be able to register my client
  So that I can use client-dependent services

  Background:
    Given I have registered an account

  Scenario: Registering a client with a name
    Given I have provided my access token
    And I have provided a client name
    When I submit the client registration request
    Then the response contains client information, including a client secret
    And the client name should match the provided name
    And it is not cacheable

  Scenario: Registering a client with a name containing international characters
    Given I have provided my access token
    And I have provided a client name of "Iñtërnâtiônàlizætiøn 中国扬声器可以阅读本"
    When I submit the client registration request
    Then the response contains client information, including a client secret
    And the client name should match the provided name
    And it is not cacheable

  Scenario: Registering a client without a name
    Given I have provided my access token
    And I have not provided a client name
    When I submit the client registration request
    Then the response contains client information, including a client secret
    And a client name should have been created for me
    And it is not cacheable

  Scenario: Trying to register a client without user authorisation
    Given I have not provided my access token
    When I submit a client registration request
    Then the request fails because I am unauthorised

  Scenario: Trying to register a client with invalid user authorisation
    Given I have provided an incorrect access token
    When I submit a client registration request
    Then the request fails because I am unauthorised

  Scenario: Trying to register a client with an empty name
    Not providing a name is OK because it's optional, but providing an empty name means that an
    invalid name has been provided, which should return an error.
    Given I have provided my access token
    And I have provided a client name of ""
    When I submit the client registration request
    Then the request fails because it is invalid

  Scenario: Trying to register more than the allowed number of clients
    Given I have provided my access token
    And I have registered 12 clients
    When I submit a client registration request
    Then the request fails because it is invalid
    And the reason is that the client limit has been reached

