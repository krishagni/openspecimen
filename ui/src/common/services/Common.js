
import http from './HttpClient.js';

class Common {
  async getUiState() {
    return http.get('users/current-user-ui-state');
  }

  async saveUiState(uiState) {
    return http.put('users/current-user-ui-state', uiState);
  }
}

export default new Common();
