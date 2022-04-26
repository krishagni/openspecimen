
import ui from '@/global.js';
import router from '@/router/index.js';

class Router {
  baseUrl = ui.ngServer + '#/';
  // baseUrl = 'http://localhost:9000/#/';

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
    router.push({name: name, params: params || {}, query: query || {}});
  }

  back() {
    router.go(-1);
  }

  reload() {
    router.go(router.currentRoute);
  }

  getUrl(name, params, query) {
    let route = router.resolve({name: name, params: params, query: query});
    return route && route.href;
  }
}

export default new Router();
