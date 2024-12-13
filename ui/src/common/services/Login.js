
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
}

export default new Login();
