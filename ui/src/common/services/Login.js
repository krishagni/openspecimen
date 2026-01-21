
import http from '@/common/services/HttpClient.js';
import ui   from '@/global.js';

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
        if (resp.token) {
          localStorage.osAuthToken = http.headers['X-OS-API-TOKEN'] = resp.token;
        } else {
          localStorage.removeItem('osAuthToken');
          delete http.headers['X-OS-API-TOKEN'];
        }

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

  forgotPassword(detail) {
    return http.post('users/forgot-password', detail);
  }

  resetPassword(detail) {
    return http.post('users/reset-password', detail);
  }

  generateOtpSecretCodeToken(detail) {
    return http.post('user-otp-details/reset-token', detail);
  }

  resetOtpSecretCode(token, errorHandler) {
    return http.get('user-otp-details/reset-secret', {token}, null, errorHandler);
  }

  signUp(detail) {
    return http.post('users/sign-up', detail);
  }

  getPasswordSettings() {
    return http.get('config-settings/password');
  }

  gotoIdp(domain) {
    if (domain.type == 'saml') {
      window.location.replace(http.getServerAppUrl() + 'saml2/authenticate/' + domain.name + '?_nonce=' + Date.now());
    } else if (domain.type == 'oauth') {
      window.location.replace(http.getUrl('oauth/login/' + domain.id));
    }
  }

  getIdpLogoutUrl() {
    return http.get('sessions/saml2-logout?_nonce=' + Date.now()).then(resp => resp.url);
  }
}

export default new Login();
