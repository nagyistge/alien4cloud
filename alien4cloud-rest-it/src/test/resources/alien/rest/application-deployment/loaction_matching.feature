Feature: Match locations and set location policies.

  Background: 
    Given I am authenticated with "ADMIN" role
    And I upload the archive "tosca-normative-types-wd06"
    And I upload a plugin
    And I create an orchestrator named "Mount doom orchestrator" and plugin id "alien4cloud-mock-paas-provider:1.0" and bean name "mock-orchestrator-factory"
    And I enable the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I create a location named "Thark location 2" and infrastructure type "OpenStack" to the orchestrator "Mount doom orchestrator"
    And I have an application "ALIEN" with a topology containing a nodeTemplate "Compute" related to "tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT"

  Scenario: Get locations matchings for this topology
    When I ask for the locations matching for this application
    Then I should receive a match result with 2 locations
      | Thark location   |
      | Thark location 2 |
