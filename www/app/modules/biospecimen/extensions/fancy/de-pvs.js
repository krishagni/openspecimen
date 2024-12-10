var openspecimen = openspecimen || {}
openspecimen.ui = openspecimen.ui || {};
openspecimen.ui.fancy = openspecimen.ui.fancy || {};

openspecimen.ui.fancy.Pvs = edu.common.de.LookupSvc.extend({
  getApiUrl: function() {
    var apiUrls = angular.element(document).injector().get('ApiUrls');
    return apiUrls.getBaseUrl() + 'permissible-values/v/';
  },

  getCacheKey: function(queryTerm, searchFilters, field) {
    var resultKey = '_default';
    if (!queryTerm) {
      if (searchFilters) {
        var keys = Object.keys(searchFilters).sort();
        if (keys.length > 0) {
          resultKey = '';
        }

        for (var i = 0; i < keys.length; ++i) {
          resultKey += keys[i] + "__" + searchFilters[keys[i]] + '__';
        }
      }

      resultKey += '_' + field.attribute + '_' + field.leafValue;
    }

    return resultKey;
  },

  searchRequest: function(searchTerm, searchFilters, field) {
    var filters = {searchString: searchTerm};
    if (field.attribute) {
      filters.attribute = field.attribute;
    }

    filters.includeOnlyLeafValue = field.leafValue;
    return $.extend(filters, searchFilters);
  },

  formatResults: function(pvs) {
    var result = [];
    for (var i = 0; i < pvs.length; ++i) {
      var text = pvs[i].value || pvs[i].text;
      if (pvs[i].conceptCode) {
        text += ' (' + pvs[i].conceptCode + ')';
      }

      result.push({id: pvs[i].id, text: text});
    }

    return result;
  },

  formatResult: function(data) {
    if (!data) {
      return '';
    }

    var text = data.value || data.text;
    if (data.conceptCode) {
      text += ' (' + data.conceptCode + ')';
    }

    return {id: data.id, text: text};
  },

  getDefaultValue: function(luField) {
    var defaultValue = luField.params.field.defaultValue;

    var deferred = $.Deferred();
    if (!defaultValue) {
      deferred.resolve(undefined);
      return deferred.promise();
    }

    luField.search(defaultValue).done(
      function(result) {
        if (result && result.length > 0) {
          result = result[0];
        } else {
          result = undefined;
        }

        deferred.resolve(result);
      }
    );

    return deferred.promise();
  },

  getHeaders: function() {
    var $http = angular.element(document).injector().get('$http');
    return {
      'X-OS-API-TOKEN': $http.defaults.headers.common['X-OS-API-TOKEN'],
      'X-OS-SURVEY-TOKEN': $http.defaults.headers.common['X-OS-SURVEY-TOKEN']
    };
  },

  getQueryParams: function(id, field) {
    return {
      key: 'attribute=' + field.attribute + '_value=' + id,
      query: {attribute: field.attribute}
    }
  }
});

openspecimen.ui.fancy.PvField = edu.common.de.LookupField.extend({
  svc: new openspecimen.ui.fancy.Pvs()
});

edu.common.de.FieldManager.getInstance()
  .register({
    name: "pvField", 
    displayName: "PV Dropdown",
    fieldCtor: openspecimen.ui.fancy.PvField
  }); 
