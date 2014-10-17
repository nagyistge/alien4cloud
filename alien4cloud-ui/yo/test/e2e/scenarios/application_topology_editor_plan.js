'use strict';

var common = require('../common/common');
var navigation = require('../common/navigation');
var topologyEditorCommon = require('../topology/topology_editor_common');
var componentData = require('../topology/component_data');

describe('NodeTemplate relationships edition', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  // After each spec in the tests suite(s)
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to view the topology start plan/workflow', function() {
    console.log('################# should be able to view the topology start plan/workflow');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.dependsOnCompute2);

    navigation.go('applications', 'plan');
  });
});
