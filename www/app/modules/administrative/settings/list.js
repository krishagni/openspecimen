
angular.module('os.administrative.setting.list', ['os.administrative.models'])
  .controller('SettingsListCtrl', function(
    $rootScope, $scope, $state, $stateParams, $sce, $filter, Setting, SettingUtil, Alerts) {

    var fileCtx;

    function init() {
      $scope.isEdit = false;
      fileCtx = $scope.fileCtx = {
        ctrl: {},
        uploadUrl: $sce.trustAsResourceUrl(Setting.getFileUploadUrl())
      };

      var moduleName = $stateParams.moduleName;
      if (!moduleName) {
        $state.go('settings-list', {moduleName: $scope.ctx.dbModuleNames[0]});
        return;
      } 

      $scope.selectedModule = $scope.ctx.filteredModules[moduleName];
    }

    function filterSettings(settings, searchText) {
      searchText = searchText.toLowerCase();
      return $filter('filter')(settings,
        function(setting) {
          return (setting.$$osPropName.toLowerCase().indexOf(searchText) !== -1 ||
            setting.$$osPropDesc.toLowerCase().indexOf(searchText) !== -1);
        }
      );
    }

    function saveSetting() {
      Setting.updateSetting($scope.setting).then(
        function(resp) {
          Alerts.success('settings.success_message');
          angular.extend($scope.existingSetting, resp);
          $scope.isEdit = false;
          SettingUtil.clearSetting(resp.module, resp.name, resp);

          if (resp.module == 'common' && ['date_format', 'de_date_format', 'time_format'].indexOf(resp.name) > -1) {
            $rootScope.loadLocale();
          }
        }
      );
    }
    
    $scope.updateSetting = function(setting) {
      $scope.isEdit = true;
      $scope.existingSetting = setting;
      $scope.setting = angular.copy(setting);
      $scope.setting.value = '';
      fileCtx.uploadUrl = $sce.trustAsResourceUrl(Setting.getFileUploadUrl(setting.module, setting.name));
    }
    
    $scope.cancel = function() {
      $scope.isEdit = false;
    }
    
    $scope.submit = function() {
      var type = $scope.setting.type;
      if (type != 'FILE' && $scope.existingSetting.value == $scope.setting.value) {
        Alerts.error('settings.invalid_value');
        return;
      } 

      if (type == 'FILE' && $scope.fileCtx.ctrl.isFileSelected()) {
        $scope.fileCtx.ctrl.submit().then(
          function(filename) {
            $scope.setting.value = filename;
            saveSetting(); 
          }
        );
      } else {
        saveSetting();
      }
    }

    $scope.searchSetting = function(searchText) {
      var filteredModules     = $scope.ctx.dbModules;
      var filteredModuleNames = $scope.ctx.dbModuleNames;

      if (searchText) {
        var allSettings     = {name: 'all', settings: []};
        filteredModules     = {'all': allSettings};
        filteredModuleNames = ['all'];

        angular.forEach($scope.ctx.dbModules, function(dbModule) {
          var filteredSettings = filterSettings(dbModule.settings, searchText);
          if (filteredSettings.length > 0) {
            allSettings.settings = allSettings.settings.concat(filteredSettings);

            filteredModules[dbModule.name] = {name: dbModule.name, settings: filteredSettings};
            filteredModuleNames.push(dbModule.name);
          }
        });
      }

      $scope.ctx.filteredModules     = filteredModules;
      $scope.ctx.filteredModuleNames = filteredModuleNames;

      var selectedModuleName = $scope.ctx.filteredModuleNames[0];
      $state.go('settings-list', {moduleName: selectedModuleName});
      $scope.selectedModule = $scope.ctx.filteredModules[selectedModuleName];
    }

    init();
  });
