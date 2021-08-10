
import httpSvc from '@/common/services/HttpClient.js';

class Settings {
  getPasswordRules() {
    return httpSvc.get('config-settings/password');
  }
}

export default new Settings();
