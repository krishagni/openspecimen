
angular.module('os.common.import.addctrl', ['os.common.import.importjob'])
  .controller('ImportObjectCtrl', function(
    $rootScope, $scope, $sce, $state, importDetail,
    Form, ImportJob, Alerts, Util, SettingUtil) {

    var maxTxnSize = undefined;

    function init() {
      $scope.importDetail = importDetail;
      $scope.importJob = new ImportJob({
        objectType: importDetail.objectType,
        importType: importDetail.importType || 'CREATE',
        csvType: importDetail.csvType || 'SINGLE_ROW_PER_OBJ',
        dateFormat: $rootScope.global.shortDateFmt,
        timeFormat: $rootScope.global.timeFmt,
        inputFileId: undefined,
        fieldSeparator: importDetail.objectType && importDetail.objectType.fieldSeparator,
        objectParams: importDetail.objectParams || {}
      });

      $scope.importJobsFileUrl = $sce.trustAsResourceUrl(ImportJob.url() + 'input-file');
      $scope.inputFileTmplUrl  = getInputTmplUrl($scope.importJob);
      $scope.fileImporter = {};
      $scope.submitting = false;

      if (importDetail.types && importDetail.types.length > 0) {
        $scope.onTypeSelect(importDetail.types[0]);
      }
    }

    function getInputTmplUrl(importJob) {
      var url = ImportJob.url() + 'input-file-template?' + getQueryParams(importJob);
      return $sce.trustAsResourceUrl(url);
    }

    function getQueryParams(importJob) {
      var qs = 'schema=' + importJob.objectType;
      angular.forEach(importJob.objectParams,
        function(value, key) {
          qs += '&' + key + '=' + value;
        }
      );

      return qs;
    }

    function handleTxnSizeExceeded(resp, fileId) {
      if (maxTxnSize == undefined) {
        SettingUtil.getSetting('common', 'import_max_records_per_txn').then(
          function(setting) {
            maxTxnSize = setting.value;
            warnTxnSizeExceeded(resp, fileId);
          }
        );
      } else {
        warnTxnSizeExceeded(resp, fileId);
      }
    }

    function warnTxnSizeExceeded(resp, fileId) {
      Util.showConfirm({
        ok: function () {
          submitJob(fileId, false);
        },

        title: "common.warning",
        isWarning: true,
        confirmMsg: "bulk_imports.txn_size_exceeded",
        input: { maxTxnSize: maxTxnSize, inputRecsCount: resp.totalRecords }
      });
    }

    function submitJob(fileId, atomic) {
      $scope.submitting = true;
      $scope.importJob.inputFileId = fileId;
      $scope.importJob.atomic = atomic;
      $scope.importJob.$saveOrUpdate().then(
        function(resp) {
          if (resp.status == 'TXN_SIZE_EXCEEDED') {
            $scope.submitting = false;
            handleTxnSizeExceeded(resp, fileId);
          } else {
            Alerts.success('bulk_imports.job_submitted', resp);
            $state.go(importDetail.onSuccess.state, importDetail.onSuccess.params);
          }
        },

        function(err) {
          $scope.submitting = false;
        }
      );
    }

    $scope.import = function() {
      $scope.submitting = true;
      $scope.fileImporter.submit().then(
        function(resp) {
          submitJob(resp.fileId, true);
        },

        function() {
          $scope.submitting = false;
        }
      );
    };

    $scope.onTypeSelect = function(objectType) {
      $scope.importDetail.objectType = objectType.type;
      $scope.importDetail.showImportType = objectType.showImportType;
      $scope.importDetail.showUpsert = objectType.showUpsert;
      $scope.importDetail.importType = objectType.importType;

      var importJob            = $scope.importJob;
      importJob.objectType     = objectType.type;
      importJob.importType     = objectType.importType || 'CREATE',
      importJob.csvType        = objectType.csvType || 'SINGLE_ROW_PER_OBJ',
      importJob.fieldSeparator = objectType.fieldSeparator;
      importJob.objectParams   = objectType.params;

      $scope.inputFileTmplUrl  = getInputTmplUrl(importJob);
    };

    $scope.onEntitySelect = function() {
      $scope.importJob.objectParams.entityId = importDetail.objectParams.entityId;
      $scope.inputFileTmplUrl = getInputTmplUrl($scope.importJob);
    }

    $scope.searchEntities = function(searchTerm) {
      importDetail.entitiesFn(searchTerm).then(
        function(entities) {
          importDetail.entities = entities;
        }
      );
    }

    init();
  });
