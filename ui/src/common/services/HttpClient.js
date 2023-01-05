
import axios from 'axios';
import alertSvc from './Alerts.js';
import routerSvc from './Router.js';

class HttpClient {
  protocol = '';

  host = '';

  port = '';

  path = '';

  headers = {};

  listeners = [];

  constructor() {
  }

  addListener(listener) {
    if (this.listeners.indexOf(listener) == -1) {
      this.listeners.push(listener);
    }
  }

  removeListener(listener) {
    const idx = this.listeners.indexOf(listener);
    if (idx >= 0) {
      this.listeners.splice(idx, 1);
    }
  }

  get(url, params) {
    return this.promise('get', () => axios.get(this.getUrl(url), this.config(params)));
  }

  async post(url, data, params) {
    return this.promise('post', () => axios.post(this.getUrl(url), data, this.config(params)));
  }

  async put(url, data, params) {
    return this.promise('put', () => axios.put(this.getUrl(url), data, this.config(params)));
  }

  async delete(url, data, params) {
    const config = this.config(params);
    config.data = data;
    return this.promise('delete', () => axios.delete(this.getUrl(url), config));
  }

  getUrl(url, {query = ''} = {}) {
    url = this.getServerAppUrl() + 'rest/ng/' + url;
    if (query && typeof query == 'object') {
      let qp = '';
      Object.keys(query).forEach(
        (name) => {
          if (qp) {
            qp += '&';
          }

          qp += name + '=' + encodeURIComponent(query[name])
        }
      );

      if (qp) {
        url += '?' + qp;
      }
    }

    return url;
  }

  getServerUrl() {
    let result = '';
    if (this.host) {
      result = this.protocol ? (this.protocol + '://') : 'https://';
      result += this.host;
      if (this.port) {
        result += ':' + this.port;
      }
    }

    return result;
  }

  getServerAppUrl() {
    let result = this.getServerUrl();
    if (this.path) {
      result += this.path;
    }

    if (result && !result.endsWith('/')) {
      result += '/';
    }

    return result;
  }

  downloadFile(url) {
    let clickEvent;
    if (typeof Event == 'function') {
      clickEvent = new MouseEvent('click', { view: window, bubbles: true, cancelable: false });
    } else {
      clickEvent = document.createEvent('Event');
      clickEvent.initEvent('click', true, false);
    }

    let link = document.createElement('a');
    link.href = url;
    link.target = '_blank';
    link.dispatchEvent(clickEvent);
  }

  config(params) {
    if (params) {
      params = Object.keys(params).reduce(
        (urlSearchParams, name) => {
          let value = params[name];
          if (value === undefined || value === null) {
            return urlSearchParams;
          }

          if (value instanceof Array) {
            value.forEach(element => urlSearchParams.append(name, element));
          } else {
            urlSearchParams.append(name, value);
          }

          return urlSearchParams;
        },
        new URLSearchParams()
      );
    }

    return {headers: this.headers, params: params};
  }

  notifyStart(method) {
    this.listeners.forEach(listener => listener.callStarted({method}));
  }

  notifyComplete(method) {
    this.listeners.forEach(listener => listener.callCompleted({method}));
  }

  notifyFail(method) {
    this.listeners.forEach(listener => listener.callFailed({method}));
  }

  promise(method, apiCall) {
    this.notifyStart(method);
    return new Promise((resolve) => {
      apiCall()
        .then(resp => {
          this.notifyComplete(method);
          resolve(resp.data);
        })
        .catch(e => {
          this.notifyFail(method);

          if (!e.response) {
            alertSvc.error(e.message);
            return;
          }

          if (e.response.status == 401) {
            localStorage.removeItem('osAuthToken');
            routerSvc.ngGoto('', {logout: true});
            return;
          }

          let errors = e.response.data;
          if (errors instanceof Array) {
            let msg = errors.map(err => err.message + ' (' + err.code + ')').join(',');
            alertSvc.error(msg);
          } else if (errors) {
            alertSvc.error(errors);
          } else {
            alertSvc.error(e.response.status + ': ' + e.response.statusText);
          }
        });
    });
  }
}

export default new HttpClient();
