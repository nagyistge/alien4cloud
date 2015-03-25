'use strict';

angular.module('alienUiApp').controller('AuditController', ['$scope', 'auditService', 'ngTableParams', '$filter', '$timeout', '$state',
  function($scope, auditService, ngTableParams, $filter, $timeout, $state) {

    // display configuration
    var timestampFormat = 'medium';

    // displayed column
    $scope.columns = [{
      title: 'Date',
      field: 'timestamp',
      visible: true
    }, {
      title: 'Username',
      field: 'userName',
      visible: true
    }, {
      title: 'First Name',
      field: 'userFirstName',
      visible: false
    }, {
      title: 'Last Name',
      field: 'userLastName',
      visible: false
    }, {
      title: 'Email',
      field: 'userEmail',
      visible: false
    }, {
      title: 'Category',
      field: 'category',
      visible: true
    }, {
      title: 'Action',
      field: 'action',
      visible: true
    }, {
      title: 'Method',
      field: 'method',
      visible: true
    }, {
      title: 'Response status',
      field: 'responseStatus',
      visible: true
    }, {
      title: 'Action description',
      field: 'actionDescription',
      visible: false
    }, {
      title: 'Path',
      field: 'path',
      visible: false
    }, {
      title: 'Request Body',
      field: 'requestBody',
      visible: false
    }, {
      title: 'Source IP',
      field: 'sourceIp',
      visible: false
    }];

    var searchRequestObject = {
      'query': '',
      'filters': undefined,
      'from': 0,
      'size': 1000
    };

    $scope.auditTableParam = new ngTableParams({
      page: 1, // show first page
      count: 10 // count per page
    }, {
      total: 0, // length of data
      getData: function($defer, params) {
        auditService.search([], angular.toJson(searchRequestObject), function(successResult) {
          // get facets
          var data = successResult.data.data;
          $scope.facets = successResult.data.facets;

          // prepare to dysplay
          prepareTraces(data);
          params.total(data.length);
          $defer.resolve(data.slice((params.page() - 1) * params.count(), params.page() * params.count()));

        });
      }
    });

    //////////////////////////////////
    // UI utils methods
    //////////////////////////////////

    // format traces befor display
    function prepareTraces(traces) {
      // prepare each "timestamp" field
      traces.forEach(function(trace) {
        trace.timestamp = $filter('date')(trace.timestamp, timestampFormat);
      });
    }

    // use to display the correct text in UI
    $scope.getFormatedFacetValue = function(term, value) {
      // Add other boolean term facet in the condition
      if (term === 'abstract') {
        if (value === 'F' || value[0] === false) {
          return $filter('translate')('FALSE');
        } else {
          return $filter('translate')('TRUE');
        }
      } else {
        return value;
      }
    };

    $scope.goToAuditConfiguration = function() {
      $state.go('admin.audit.conf');
    };

    //////////////////////////////////
    // Search methods
    //////////////////////////////////
    var doSearch = function() {
      // prepare filters
      var allFacetFilters = [];
      allFacetFilters.push.apply(allFacetFilters, $scope.facetFilters);
      updateSearch($scope.searchedKeyword, allFacetFilters);
    };

    $scope.doSearch = doSearch;

    // update search result table
    function updateSearch(keyword, filters) {

      /*
       Search api expect a json object matching the following pattern:
       {
       'query': 'mysearched',
       'from' : int,
       'to' : int,
       'filters': {'termId1' : ['facetId1'],'termId2' : ['facetId2','facetedId3'] }
       }
       */

      // Convert filter [] filters -> Object
      var objectFilters = {};
      filters.forEach(function(filter) {

        filter = filter || {};
        if (!(filter.term in objectFilters)) {
          // First time the key is present set to the value in filter
          objectFilters[filter.term] = filter.facet;
        } else {
          // Merge otherwise
          objectFilters[filter.term].push.apply(objectFilters[filter.term], filter.facet);
        }
      });

      searchRequestObject.query = keyword;
      searchRequestObject.filters = objectFilters;

      // reload traces table
      $scope.auditTableParam.reload();
    }

    /* Add a facet Filters*/
    $scope.addFilter = function(termId, facetId) {
      $scope.facetFilters = $scope.facetFilters || [];
      // Test if the filter exists : [term:facet]
      var termIndex = -1;
      for (var i = 0, len = $scope.facets.length; i < len; i++) {
        if ($scope.facetFilters[i].term === termId && $scope.facetFilters[i].facet === facetId) {
          termIndex = i;
        }
      }

      if (termIndex < 0) {
        var facetSearchObject = {};
        facetSearchObject.term = termId;
        facetSearchObject.facet = [];
        facetSearchObject.facet.push(facetId);
        $scope.facetFilters.push(facetSearchObject);
      }

      // Search update with new filters list
      $scope.doSearch();
    };

    /*Remove a facet filter*/
    $scope.removeFilter = function(filterToRemove) {
      // Remove the selected filter
      var index = $scope.facetFilters.indexOf(filterToRemove);
      if (index >= 0) {
        $scope.facetFilters.splice(index, 1);
      }

      // Search update with new filters list
      $scope.doSearch();
    };

    /*Reset all filters*/
    $scope.reset = function() {
      // Reset all filters
      $scope.facetFilters.splice(0, $scope.facetFilters.length);
      $scope.doSearch();
    };

  }
]);