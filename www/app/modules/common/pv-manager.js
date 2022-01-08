
/**
 * TODO: The PvManager will actually do following
 * 1. make REST API calls to get PVs for the input attribute
 * 2. Cache the PVs so that frequent calls are not needed
 */
angular.module('openspecimen')
  .factory('PvManager', function($http, $q, $translate, ApiUrls, ApiUtil, Site, Util) {
    var url = ApiUrls.getBaseUrl() + 'permissible-values';

    var anatomicSites = [
      'DIGESTIVE ORGANS',
      'SKIN',
      'MALE GENITAL ORGANS',
      'UNKNOWN PRIMARY SITE',
      'PERIPHERAL NERVES AND AUTONOMIC NERVOUS SYSTEM',
      'FEMALE GENITAL ORGANS',                       
      'OTHER AND ILL-DEFINED SITES',
      'HEMATOPOIETIC AND RETICULOENDOTHELIAL SYSTEMS',
      'RETROPERITONEUM AND PERITONEUM',
      'RESPIRATORY SYSTEM AND INTRATHORACIC ORGANS',
      'BONES, JOINTS AND ARTICULAR CARTILAGE',
      'THYROID AND OTHER ENDOCRINE GLANDS',
      'MENINGES',
      'CONNECTIVE, SUBCUTANEOUS AND OTHER SOFT TISSUES',
      'BREAST',
      'LIP, ORAL CAVITY AND PHARYNX',
      'LYMPH NODES',
      'URINARY TRACT',
      'BRAIN',
      'SPINAL CORD, CRANIAL NERVES, AND OTHER PARTS OF CENTRAL NERVOUS SYSTEM',
      'EYE, BRAIN AND OTHER PARTS OF CENTRAL NERVOUS SYSTEM',
      'Not Specified'
    ];
    
    var positionLabelingSchemes = [
      'Numbers',
      'Alphabets Upper Case',
      'Alphabets Lower Case',
      'Roman Upper Case',
      'Roman Lower Case'
    ];

    /** We need to have i18n keys for these as well **/
    var storageTypes = [
      'Manual',
      'Virtual'
    ];

    var visitStatuses = [
      'Complete',
      'Pending',
      'Missed Collection',
      'Not Collected'
    ];

    var specimenStatuses = [
      'Collected',
      'Missed Collection',
      'Not Collected',
      'Pending'
    ];

    var activityStatuses = [
      'Active',
      'Pending',
      'Disabled',
      'Closed'
    ];

    var spmnLabelPrePrintModes = [
      {name: 'ON_REGISTRATION', displayKey: 'cp.spmn_label_pre_print_modes.ON_REGISTRATION'},
      {name: 'ON_VISIT',        displayKey: 'cp.spmn_label_pre_print_modes.ON_VISIT'},
      {name: 'ON_PRIMARY_COLL', displayKey: 'cp.spmn_label_pre_print_modes.ON_PRIMARY_COLL'},
      {name: 'ON_PRIMARY_RECV', displayKey: 'cp.spmn_label_pre_print_modes.ON_PRIMARY_RECV'},
      {name: 'NONE',            displayKey: 'cp.spmn_label_pre_print_modes.NONE'}
    ];

    var spmnLabelAutoPrintModes = [
      {name: 'PRE_PRINT',     displayKey: 'srs.spmn_label_auto_print_modes.PRE_PRINT'},
      {name: 'ON_COLLECTION', displayKey: 'srs.spmn_label_auto_print_modes.ON_COLLECTION'},
      {name: 'ON_RECEIVE',    displayKey: 'srs.spmn_label_auto_print_modes.ON_RECEIVE'},
      {name: 'NONE',          displayKey: 'srs.spmn_label_auto_print_modes.NONE'}
    ];

    var intervalUnits = [
      {name: 'DAYS',   displayKey: 'common.interval_units.DAYS'},
      {name: 'WEEKS',  displayKey: 'common.interval_units.WEEKS'},
      {name: 'MONTHS', displayKey: 'common.interval_units.MONTHS'},
      {name: 'YEARS',  displayKey: 'common.interval_units.YEARS'}
    ]

    var positionAssignments = [
      {name: 'HZ_TOP_DOWN_LEFT_RIGHT',  displayKey: 'container.position_assignments.HZ_TOP_DOWN_LEFT_RIGHT'},
      {name: 'HZ_TOP_DOWN_RIGHT_LEFT',  displayKey: 'container.position_assignments.HZ_TOP_DOWN_RIGHT_LEFT'},
      {name: 'HZ_BOTTOM_UP_LEFT_RIGHT', displayKey: 'container.position_assignments.HZ_BOTTOM_UP_LEFT_RIGHT'},
      {name: 'HZ_BOTTOM_UP_RIGHT_LEFT', displayKey: 'container.position_assignments.HZ_BOTTOM_UP_RIGHT_LEFT'},
      {name: 'VT_TOP_DOWN_LEFT_RIGHT',  displayKey: 'container.position_assignments.VT_TOP_DOWN_LEFT_RIGHT'},
      {name: 'VT_TOP_DOWN_RIGHT_LEFT',  displayKey: 'container.position_assignments.VT_TOP_DOWN_RIGHT_LEFT'},
      {name: 'VT_BOTTOM_UP_LEFT_RIGHT', displayKey: 'container.position_assignments.VT_BOTTOM_UP_LEFT_RIGHT'},
      {name: 'VT_BOTTOM_UP_RIGHT_LEFT', displayKey: 'container.position_assignments.VT_BOTTOM_UP_RIGHT_LEFT'},
    ]

    var pvMap = {
      anatomicSite: anatomicSites,
      'storage-type': storageTypes,
      'visit-status': visitStatuses,
      'specimen-status': specimenStatuses,
      'container-position-labeling-schemes': positionLabelingSchemes,
      'activity-status': activityStatuses,
      'specimen-label-pre-print-modes': spmnLabelPrePrintModes,
      'specimen-label-auto-print-modes': spmnLabelAutoPrintModes,
      'interval-units': intervalUnits,
      'container-position-assignments': positionAssignments
    };

    var pvIdMap = {
      'clinical-status'     : 'clinical_status',
      'gender'              : 'gender',
      'specimen-class'      : 'specimen_type',
      'laterality'          : 'laterality',
      'pathology-status'    : 'pathology_status',
      'collection-procedure': 'collection_procedure',
      'collection-container': 'collection_container',
      'vital-status'        : 'vital_status',
      'received-quality'    : 'receive_quality',
      'ethnicity'           : 'ethnicity',
      'race'                : 'race',
      'anatomic-site'       : 'anatomic_site',
      'site-type'           : 'site_type',
      'clinical-diagnosis'  : 'clinical_diagnosis',
      'specimen-biohazard'  : 'specimen_biohazard',
      'consent_response'    : 'consent_response',
      'missed-visit-reason' : 'missed_visit_reason',
      'cohort'              : 'cohort'
    };

    function valueOf(input) {
      return input.value;
    };

    function parentAndValue(input) {
      return {parent: input.parentValue, value: input.value};
    };

    function transform(pvs, transformfn, incParentVal, result) {
      transformfn = transformfn || (incParentVal ? parentAndValue : valueOf);
      return pvs.map(function(pv) { return transformfn(pv); });
    };

    function loadPvs(attr, srchTerm, transformFn, incOnlyLeaf, allStatuses) {
      var pvId = pvIdMap[attr];
      if (!pvId && pvMap[attr]) {
        return _getPvs(attr);
      } else if (!pvId) {
        pvId = attr;
      }

      var params = {
        attribute: pvId,
        includeOnlyLeafValue: incOnlyLeaf,
        maxResults: 100
      };

      if (angular.isArray(srchTerm)) {
        params.value = srchTerm
      } else {
        params.searchString = srchTerm
      }

      if (allStatuses) {
        params.activityStatus = 'all';
      }

      return $http.get(url, {params: params}).then(
        function(result) {
          return transform(result.data, transformFn, null);
        }
      );
    };

    function loadPvsByParent(parentAttr, parentVal, incParentVal, transformFn, maxPvs) {
      var pvId = pvIdMap[parentAttr];
      if (!pvId) {
        pvId = parentAttr;
      }

      var params = {
        parentAttribute: pvId, 
        parentValue: parentVal,  
        includeParentValue: incParentVal,
        maxResults: maxPvs || 100
      };

      return $http.get(url, {params: params}).then(
        function(result) {
          return transform(result.data, transformFn, incParentVal);
        }
      );
    };

    function  _getPvs(attr) {
      var deferred = $q.defer();
      var result = undefined;
      if (pvMap[attr]) {
        result = pvMap[attr];
        if (hasI18nKey(result) && !isDisplayNameInitialized(result)) {
          initDisplayNames(result);
        }
      } else {
        result = [];
      }
      deferred.resolve(result);
      return deferred.promise;
    };

    function hasI18nKey(pvs) {
      return !!pvs[0].displayKey;
    }

    function isDisplayNameInitialized(pvs) {
      return !!pvs[0].displayName;
    }

    function initDisplayNames(pvs) {
      $translate('pvs.not_specified').then(
        function() {
          angular.forEach(pvs, function(pv) {
            pv.displayName = $translate.instant(pv.displayKey);
          });
        }
      );
    }

    return {
      getPvs: function(attr, srchTerm, transformFn) {
        var pvs = [];
        loadPvs(attr, srchTerm, transformFn).then(
          function(result) {
            Util.unshiftAll(pvs, result);
          }
        );    

        return pvs;
      },

      getLeafPvs: function(attr, srchTerm, transformFn) {
        var pvs = [];
        loadPvs(attr, srchTerm, transformFn, true).then(
          function(result) {
            Util.unshiftAll(pvs, result);
          }
        );

        return pvs;
      },

      loadPvs: loadPvs,

      getPvsByParent: function(parentAttr, parentVal, incParentVal, transformFn) {
        var pvs = [];

        loadPvsByParent(parentAttr, parentVal, incParentVal, transformFn).then(
          function(result) {
            Util.unshiftAll(pvs, result);
          }
        );

        return pvs;
      },

      loadPvsByParent: loadPvsByParent,

      getSites: function(opts) {
        var sites = [];
        Site.list(opts).then(
          function(siteList) {
            Array.prototype.push.apply(sites, siteList);
          }
        );

        return sites;
      },

      notSpecified: function() {
        return $translate.instant('pvs.not_specified');
      }
    };
  });
