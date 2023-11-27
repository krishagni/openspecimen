
import ui from '@/global.js';
import router from '@/router/index.js';

class Router {
  baseUrl = ui.ngServer + '#/';
  // baseUrl = 'http://localhost:9000/#/';

  uiUrl = 'ui-app/';
  // uiUrl = 'http://localhost:8081/';

  ngGoto(state, params, newTab) {
    const url = this.ngUrl(state, params);
    if (newTab) {
      window.open(url, '_blank');
    } else {
      window.location.href = url;
    }
  }

  ngUrl(state, params) {
    let url   = this.baseUrl + state;
    let query = '';
    Object.keys(params || {}).forEach(
      (key) => {
        let value = params[key];
        if (value) {
          if (query) {
            query += '&';
          }

          query += key + '=' + value;
        }
      }
    );

    if (query) {
      url += '?' + query;
    }

    return url
  }

  goto(name, params, query) {
    return router.push({name: name, params: params || {}, query: query || {}});
  }

  back() {
    router.go(-1);
  }

  reload() {
    router.go(router.currentRoute);
  }

  getCurrentRoute() {
    const route = router.currentRoute;
    return (route && route.value) || {};
  }

  getUrl(name, params, query) {
    let route = router.resolve({name: name, params: params, query: query});
    return route && route.href;
  }

  getFullUrl(name, params, query) {
    const url = this.getUrl(name, params, query);
    return this.uiUrl + url;
  }

  getLastRoute() {
    return this.lastRoute;
  }

  setLastRoute(route) {
    this.lastRoute = route;
  }

  eraseLastRoute() {
    this.lastRoute = null;
  }
}

export default new Router();
