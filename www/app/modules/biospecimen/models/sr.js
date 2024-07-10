angular.module('os.biospecimen.models.sr', ['os.common.models'])
  .factory('SpecimenRequirement', function(osModel, $http, PvManager) {
    var Sr = osModel(
      'specimen-requirements',
      function(sr) {
        sr.copyAttrsIfNotPresent(getDefaultProps())

        if (sr.specimensPool) {
          sr.specimensPool = sr.specimensPool.map(
            function(ps) {
              return new Sr(ps);
            }
          );
        }

        if (sr.defaultCustomFieldValues && typeof sr.defaultCustomFieldValues == 'object') {
          sr.defaultCustomFieldValuesJson = JSON.stringify(sr.defaultCustomFieldValues, null, 2);
        }

        if (sr.children) {
          sr.children = sr.children.map(
            function(child) {
              return new Sr(child);
            }
          );
        }
      }
    );

    function getDefaultProps() {
      var notSpecified = PvManager.notSpecified();
      return {
        anatomicSite: notSpecified,
        laterality: notSpecified,
        pathology: notSpecified,
        collectionContainer: notSpecified,
        collectionProcedure: notSpecified
      }
    }
 
    Sr.listFor = function(cpeId) {
      return Sr.query({eventId: cpeId});
    };

    Sr.getByCpId = function(cpId) {
      return Sr.query({cpId: cpId});
    };

    Sr.prototype.$saveProps = function() {
      if (this.defaultCustomFieldValuesJson) {
        try {
          this.defaultCustomFieldValues = JSON.parse(this.defaultCustomFieldValuesJson);
        } catch (e) {
          alert('Invalid default custom field values JSON: ' + e);
          throw e;
        }
      } else {
        this.defaultCustomFieldValues = null;
      }

      return this;
    }

    Sr.prototype.isAliquot = function() {
      return this.lineage == 'Aliquot';
    };

    Sr.prototype.isDerivative = function() {
      return this.lineage == 'Derived';
    };

    Sr.prototype.copy = function() {
      return $http.post(Sr.url() + this.$id() + '/copy').then(Sr.modelRespTransform);
    };

    Sr.prototype.createAliquots = function(requirement) {
      var url = Sr.url();
      if (requirement.defaultCustomFieldValuesJson) {
        try {
          requirement.defaultCustomFieldValues = JSON.parse(requirement.defaultCustomFieldValuesJson);
        } catch (e) {
          alert('Invalid aliquot default custom field values JSON. Error: ' + e);
          throw e;
        }
      }

      return $http.post(url + this.$id() + '/aliquots', requirement)
               .then(Sr.modelArrayRespTransform);
    };

    Sr.prototype.createDerived = function(requirement) {
      var url = Sr.url();
      if (requirement.defaultCustomFieldValuesJson) {
        try {
          requirement.defaultCustomFieldValues = JSON.parse(requirement.defaultCustomFieldValuesJson);
        } catch (e) {
          alert('Invalid derived specimen default custom field values JSON. Error: ' + e);
          throw e;
        }
      }

      return $http.post(url + this.$id() + '/derived', requirement)
               .then(Sr.modelRespTransform);
    };

    Sr.prototype.availableQty = function() {
      var available = this.initialQty;
      if (available != undefined && available != null) {
        angular.forEach(this.children, function(child) {
          if (child.lineage == 'Aliquot') {
            available = Math.round((available - child.initialQty) * 10000) / 10000;
          }
        });
      }

      return available;
    };

    Sr.prototype.hasSufficientQty = function(aliquotReq) {
      var reqQty = aliquotReq.noOfAliquots * aliquotReq.qtyPerAliquot;
      return reqQty <= this.availableQty();
    };

    Sr.prototype.delete = function() {
      return $http.delete(Sr.url() + this.$id());
    }

    Sr.prototype.getSpecimensCount = function() {
      return $http.get(Sr.url() + this.$id() + "/specimens-count").then(
        function(result) { 
          return result.data; 
        }
      );
    }

    Sr.prototype.addPoolSpecimens = function(poolSpmn) {
      return $http.post(Sr.url() + this.$id() + '/specimen-pool', poolSpmn)
        .then(Sr.modelArrayRespTransform);
    }

    return Sr;
  });
