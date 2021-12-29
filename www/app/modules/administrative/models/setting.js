angular.module('os.administrative.models.setting', ['os.common.models'])
  .factory('Setting', function(osModel, $http) {
    function init(props) {
      if (props.type == 'FILE' && !!props.value) {
        if (props.value.indexOf('classpath:') != 0) {
          props.value = props.value.substring(props.value.indexOf('_') + 1);
        }
      }
    }

    var Setting = new osModel('config-settings', init);

    Setting.getByProp = function(moduleName, propertyName) {
      var params = {module: moduleName, property: propertyName};
      return $http.get(Setting.url(), {params: params}).then(
        function(resp) {
          return new Setting(resp.data[0]);
        }
      );
    }

    Setting.getLocale = function() {
      return $http.get(Setting.url() + 'locale').then(
        function(resp) {
          return resp.data;
        }
      );
    }
    
    Setting.getWelcomeVideoSetting = function () {
      return $http.get(Setting.url() + 'welcome-video').then(
        function (resp) {
          return resp.data;
        }
      );
    };

    Setting.getAppProps = function() {
      return $http.get(Setting.url() + 'app-props').then(
        function(resp) {
          return resp.data;
        }
      );
    };

    Setting.getDeploymentSiteAssets = function() {
      return $http.get(Setting.url() + 'deployment-site-assets').then(
        function(resp) {
          return resp.data;
        }
      );
    };

    Setting.updateSetting = function(setting) {
      return $http.put(Setting.url(), setting).then(
        function(resp) {
          return new Setting(resp.data);
        }
      );
    }

    Setting.getFileUploadUrl = function() {
      return Setting.url() + "files";
    }

    Setting.getPasswordSettings = function () {
      return $http.get(Setting.url() + 'password').then(
        function (resp) {
          return resp.data;
        }
      );
    };

    Setting.prototype.getFileDownloadUrl = function() {
      return Setting.url() + "files?module=" + this.module + "&property=" + this.name;
    }

    return Setting;
  });
