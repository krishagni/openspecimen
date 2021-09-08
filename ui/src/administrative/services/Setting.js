
import http from '@/common/services/HttpClient.js';

class Setting {

  async getAppProps() {
    return http.get('config-settings/app-props');
  }

  async getLocale() {
    return http.get('config-settings/locale');
  }

  async getPasswordRules() {
    return http.get('config-settings/password');
  }
}

export default new Setting();
