
import httpSvc from '@/common/services/HttpClient.js';

class Settings {
  async getPasswordRules() {
    return httpSvc.get('config-settings/password');
  }
}

export default new Settings();
