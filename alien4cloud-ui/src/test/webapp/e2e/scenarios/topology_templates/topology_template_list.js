/* global describe, it, element, by, expect */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var topologyTemplates = require('../../topology_templates/topology_templates');

var topologyTemplateName = 'MyTopologyTemplate';

describe('Topology templates list:', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('architect');
  });

  it('Architect should be able to add a new topology template', function() {
    topologyTemplates.create(topologyTemplateName, 'description');
    topologyTemplates.checkTopologyTemplate(topologyTemplateName);

    topologyTemplates.go();
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('Architect should not be able to add a new topology template with an existing name', function() {
    topologyTemplates.create(topologyTemplateName, 'description');
    toaster.dismissIfPresent();
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('Architect should be able to cancel creation of a new topology template', function() {
    topologyTemplates.create('name', 'description', true);
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('Architect should be able to delete a topology template', function() {
    topologyTemplates.go();
    common.deleteWithConfirm('delete-template_' + topologyTemplateName, true);
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(0);
  });

  // TODO: check the pagination
  it('Admin should be able to see topology template list and check pagination', function() {
    authentication.logout();
    authentication.login('admin');
    topologyTemplates.go();
    expect(element(by.id('btn-add-template')).isPresent()).toBe(true);
  });

  it('Component manager should not be able to see topology template list', function() {
    authentication.logout();
    authentication.login('componentManager');
    expect(element(by.id('menu.topologytemplates')).isPresent()).toBe(false);
  });

  it('afterAll', function() { authentication.logout(); });
});
