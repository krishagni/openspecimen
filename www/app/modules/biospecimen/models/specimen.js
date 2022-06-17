angular.module('os.biospecimen.models.specimen', ['os.common.models', 'os.biospecimen.models.form'])
  .factory('Specimen', function(osModel, $http, $parse, SpecimenRequirement, Form, Util, User) {

    function matches(name, regex) {
      return name && name.match(regex) != null;
    }

    function isSpecimenCentric(specimen) {
      return matches(specimen.ppid, /^(\$\$cp_reg_\d+\$\$)$/g) ||
               matches(specimen.visitName, /^(\$\$cp_visit_\d+\$\$)$/g);
    }

    var Specimen = osModel(
      'specimens',
      function(specimen) {
        if (specimen.specimensPool) {
          specimen.specimensPool = specimen.specimensPool.map(
            function(poolSpmn) {
              return new Specimen(poolSpmn);
            }
          );
        }

        if (specimen.children) {
          specimen.children = specimen.children.map(
            function(child) {
              return new Specimen(child);
            }
          );
        }

        if (specimen.storageLocation) {
          specimen.storageLocation = angular.extend({}, specimen.storageLocation);
        }
        specimen.$$specimenCentricCp = isSpecimenCentric(specimen);
      }
    );

    Specimen.listFor = function(cprId, visitDetail) {
      return Specimen.query(angular.extend({cprId: cprId}, visitDetail || {}));
    };

    Specimen.listByLabels = function(labels) {
      return Specimen.query({label: labels});
    };

    Specimen.search = function(criteria) {
      return $http.post(Specimen.url() + 'search', criteria).then(Specimen.modelArrayRespTransform);
    };
    
    Specimen.flatten = function(specimens, parent, depth, pooledSpecimen, opts) {
      var result = [];
      if (!specimens) {
        return result;
      }

      depth = depth || 0;
      angular.forEach(specimens, function(specimen) {
        var depthIncrStep = 1;
        var hasChildren = (!!specimen.children && specimen.children.length > 0);
        if (opts && opts.hideDerivatives && specimen.lineage == 'Derived' && hasChildren &&
          specimen.children.every(function(a) { return a.lineage == 'Aliquot'; })) {
          depthIncrStep = 0;
          specimen.$$invisibleN = true;
        } else {
          result.push(specimen);
        }

        specimen.depth = depth || 0;
        specimen.parent = specimen.parent || parent;
        if (specimen.parent && (specimen.parent.status == 'Missed Collection' || specimen.parent.status == 'Not Collected')) {
          specimen.status = specimen.parent.status;
        }

        specimen.hasChildren = hasChildren;
        if (hasChildren) {
          result = result.concat(Specimen.flatten(specimen.children, specimen, depth + depthIncrStep, undefined, opts));
        }

        if (specimen instanceof SpecimenRequirement) {
          return;
        }

        specimen.hasOnlyPendingChildren = hasOnlyPendingChildren(specimen.children);
        if (!specimen.$$childrenHaveImg && hasChildren) {
          specimen.$$childrenHaveImg = hasImage(specimen.children);
        }

        specimen.pooledSpecimen = specimen.specimensPool && specimen.specimensPool.length > 0;
      });

      return result;
    };

    Specimen.toSpecimenAttrs = function(sr) {
      return new Specimen(toSpecimenAttrs(angular.copy(sr)));
    };

    Specimen.save = function(specimens) {
      return $http.post(Specimen.url() + 'collect', specimens).then(Specimen.modelArrayRespTransform);
    };

    //Update specimens in bulk with same detail
    Specimen.bulkEdit = function(detail) {
      return $http.put(Specimen.url() + "bulk-update", detail).then(Specimen.modelArrayRespTransform);
    }

    //Update specimens in bulk by providing detail for each
    Specimen.bulkUpdate = function(specimens) {
      return $http.put(Specimen.url(), specimens).then(Specimen.modelArrayRespTransform);
    };

    Specimen.isUniqueLabel = function(cpShortTitle, label) {
      return $http.head(Specimen.url(), {params: {cpShortTitle: cpShortTitle, label: label}}).then(
        function(result) {
          return false;
        },

        function(result) {
          return true;
        }
      );
    };

    Specimen.getAnticipatedSpecimen = function(srId) {
      return SpecimenRequirement.getById(srId).then(
        function(sr) {
          return new Specimen(toSpecimenAttrs(sr));
        }
      );
    };

    Specimen.getRouteIds = function(specimenId) {
      return $http.get(Specimen.url() + specimenId + '/cpr-visit-ids').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    Specimen.bulkDelete = function(specimenIds, reason) {
      return $http.delete(Specimen.url(), {params: {id: specimenIds, reason: reason}}).then(
        function(result) {
          return result.data;
        }
      );
    }

    Specimen.bulkStatusUpdate = function(statusSpecs) {
      return $http.put(Specimen.url() + '/status', statusSpecs).then(
        function(result) {
          return result.data;
        }
      );
    }

    Specimen.getByIds = function(ids, includeExtensions, minimalInfo) {
      return Specimen.search({
        ids: ids,
        includeExtensions: includeExtensions || false,
        minimalInfo: minimalInfo || false,
        maxResults: ids.length
      });
    }

    Specimen.prototype.getType = function() {
      return 'specimen';
    }

    Specimen.prototype.getDisplayName = function() {
      return this.label;
    };

    Specimen.prototype.hasSufficientQty = function() {
      var qty = this.initialQty;
      angular.forEach(this.children, function(child) {
        if (child.lineage == 'Aliquot') {
          qty -= child.initialQty;
        }
      });

      return qty >= 0;
    }

    Specimen.prototype.rootSpecimen = function() {
      var curr = this;
      while (curr.parent) {
        curr = curr.parent;
      }

      return curr;
    };

    Specimen.prototype.getPrimarySpecimenId = function() {
      return $http.get(Specimen.url() + this.$id() + '/primary-specimen-id').then(
        function(resp) {
          return resp.data.id;
        }
      );
    }

    Specimen.prototype.getExtensionCtxt = function(params) {
      return Specimen.getExtensionCtxt(params);
    }

    Specimen.prototype.getForms = function(params) {
      return Form.listFor(Specimen.url(), this.$id(), params);
    };

    Specimen.prototype.getRecords = function() {
      var url = Specimen.url() + this.$id() + '/extension-records';
      return Form.listRecords(url);
    };

    Specimen.prototype.getEvents = function(callback) {
      var events = [];

      if (!!this.$id()) {
        $http.get(Specimen.url() + '/' + this.$id() + '/events').then(
          function(result) {
            Util.unshiftAll(events, getEventsList(result.data));
            if (typeof callback == 'function') {
              callback(events);
            }
          }
        );
      }

      return events;
    };

    Specimen.prototype.$saveProps = function() {
      var props = ['children', 'depth', 'hasChildren', 'isOpened', 'specimensPool'];
      var that = this;
      props.forEach(function(prop) { delete that[prop]; });
      return this;
    };

    Specimen.prototype.close = function(reason) {
      var statusSpec = {status: 'Closed', reason: reason};
      return updateSpecimenStatus(this, statusSpec);
    }

    Specimen.prototype.reopen = function() {
      var statusSpec = {status: 'Active'};
      return updateSpecimenStatus(this, statusSpec);
    };

    Specimen.prototype.showVirtual = function() {
      return this.storageType == 'Virtual' && (!this.status || this.status == 'Pending');
    }

    Specimen.prototype.getMatchingRule = function(allocRules) {
      var result = -1;
      for (var i = 0; i < allocRules.length; ++i) {
        if ($parse(allocRules[i].criteria)({specimen: this})) {
          result = i;
          break;
        }
      }

      return {index: result, rule: result != -1 ? allocRules[result] : undefined};
    }

    Specimen.prototype.setCollectionEvent = function(formData) {
      if (this.lineage != 'New') {
        return;
      }

      var eventData = {
        id: formData.id,
        user: {id: formData.user},
        time: formData.time,
        comments: formData.comments,
        procedure: formData.procedure,
        container: formData.container
      }

      if (!this.collectionEvent) {
        this.collectionEvent = {};
      }

      if (this.collectionEvent.user && this.collectionEvent.user.id == eventData.user.id) {
        eventData.user = this.collectionEvent.user;
      } else {
        var that = this;
        User.getById(eventData.user.id).then(
          function(user) {
            that.collectionEvent.user = user;
          }
        )
      }

      angular.extend(this.collectionEvent, eventData);
    }

    Specimen.prototype.setReceivedEvent = function(formData) {
      if (this.lineage != 'New') {
        return;
      }

      var eventData = {
        id: formData.id,
        user: {id: formData.user},
        time: formData.time,
        comments: formData.comments,
        receivedQuality: formData.quality
      }

      if (!this.receivedEvent) {
        this.receivedEvent = {};
      }

      if (this.receivedEvent.user && this.receivedEvent.user.id == eventData.user.id) {
        eventData.user = this.receivedEvent.user;
      } else if (eventData.user && eventData.user.id > 0) {
        var that = this;
        User.getById(eventData.user.id).then(
          function(user) {
            that.receivedEvent.user = user;
          }
        )
      }

      angular.extend(this.receivedEvent, eventData);
    }

    function toSpecimenAttrs(sr) {
      sr.reqId = sr.id;
      sr.reqLabel = sr.name;
      sr.reqCode = sr.code;
      sr.poolSpecimen = !!sr.pooledSpecimenReqId;

      if (sr.lineage == 'New' && !sr.poolSpecimen) {
        sr.collectionEvent = {user: sr.collector, procedure: sr.collectionProcedure, container: sr.collectionContainer};
        sr.receivedEvent   = {user: sr.receiver, receivedQuality: 'Acceptable'};
      }

      var attrs = [
        'id', 'name', 'pooledSpecimenReqId',
        'collector', 'collectionProcedure', 'collectionContainer',
        'receiver', 'labelPrintCopies'
      ];

      attrs.forEach(function(attr) {
        delete sr[attr];
      });

      if (sr.children) {
        sr.children = sr.children.map(toSpecimenAttrs);
      }

      if (sr.specimensPool) {
        sr.specimensPool = sr.specimensPool.map(toSpecimenAttrs);
      }

      return sr;
    };

    function getEventsList(eventsRecs) { /* eventsRecs: [{event info, records: [event records]}] */
      var eventsList = [];
      angular.forEach(eventsRecs, function(eventRecs) {
        angular.forEach(eventRecs.records, function(record) {
          var user = null, time = null;
          for (var i = 0; i < record.fieldValues.length; ++i) {
            if (record.fieldValues[i].name == 'user') {
              user = record.fieldValues[i].value;
            } else if (record.fieldValues[i].name == 'time') {
              time = record.fieldValues[i].value;
            }
          }

          var formName = eventRecs.name;
          eventsList.push({
            id: record.recordId,
            formId: eventRecs.id,
            formCtxtId: record.fcId,
            sysForm: record.sysForm,
            name: eventRecs.caption,
            updatedBy: record.user,
            updateTime: record.updateTime,
            user: user,
            time: time,
            isEditable: !record.sysForm || formName == 'SpecimenCollectionEvent' || formName == 'SpecimenReceivedEvent'
          });
        });
      });

      eventsList.sort(
        function(e1, e2) { 
          var t1 = e1.time || 0;
          var t2 = e2.time || 0;
          var diff = t2 - t1;
          return diff != 0 ? diff : (e2.id - e1.id);
        }
      );
      return eventsList;
    }

    function updateSpecimenStatus(specimen, statusSpec) {
      return $http.put(Specimen.url() + '/' + specimen.$id() + '/status', statusSpec).then(
        function(result) {
          specimen.activityStatus = result.data.activityStatus;
          specimen.availabilityStatus = result.data.availabilityStatus;
          return specimen;
        }
      );
    }

    function hasOnlyPendingChildren(specimens) {
      if (!specimens || specimens.length == 0) {
        return true;
      }

      return specimens.every(
        function(spmn) {
          return !spmn.status || spmn.status == 'Pending';
        }
      );
    }

    function hasImage(specimens) {
      return (specimens || []).some(
        function(spmn) {
          return !!spmn.imageId || !!spmn.$$childrenHaveImg;
        }
      );
    }

    return Specimen;
  });
