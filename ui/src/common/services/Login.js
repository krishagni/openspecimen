
import http from '@/common/services/HttpClient.js';
import ui from '@/global.js';

class Login {
  signInState = {};

  showSignIn() {
    return this.signInState && this.signInState.showSignIn;
  }

  targetView() {
    return this.signInState && this.signInState.navTo;
  }

  setShowSignIn(state) {
    this.signInState = state;
  }

  getAuthDomains() {
    return http.get('auth-domains');
  }

  login(loginDetail) {
    return http.post('sessions', loginDetail).then(
      resp => {
        localStorage.osAuthToken = http.headers['X-OS-API-TOKEN'] = resp.token;
        return resp;
      }
    );
  }

  logout() {
    return http.delete('sessions').then(
      () => {
        delete localStorage.osAuthToken;
        delete http.headers['X-OS-API-TOKEN'];
        delete ui.currentUser;
        return true;
      }
    );
  }

  gotoIdp() {
    window.location.replace(http.getServerAppUrl() + 'saml/login?_nonce=' + Date.now());
  }
}

export default new Login();
