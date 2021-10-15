
import http from '@/common/services/HttpClient.js';

class Setting {
  cache = {};

  async getAppProps() {
    return http.get('config-settings/app-props');
  }

  async getLocale() {
    return http.get('config-settings/locale');
  }

  async getPasswordRules() {
    return http.get('config-settings/password');
  }

  getKey(module, property) {
    return module + ':' + property;
  }

  getSetting(module, property) {
    let key = this.getKey(module, property);

    let setting = this.cache[key];
    if (!setting || (new Date().getTime() - setting.time) / (60 * 60 * 1000) >= 1) {
      this.cache[key] = setting = {
        promise: this.getByProp(module, property),
        time: new Date().getTime()
      };
    }

    let self = this;
    setting.promise.then(
      function(value) {
        setting.value = value;
      },

      function() {
        //
        // rejected promise, remove it from cache so that it can
        // be loaded again in subsequent invocations...
        //
        delete self.cache[key];
      }
    );

    return setting.promise;
  }

  async getByProp(module, property) {
    return http.get('config-settings', {module: module, property: property});
  }
}

export default new Setting();
