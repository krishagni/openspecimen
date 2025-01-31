
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

  get(url, params, options, errorHandler) {
    return this.promise('get', () => axios.get(this.getUrl(url), this.config(params, options)), errorHandler);
  }

  async post(url, data, params, options, errorHandler) {
    return this.promise('post', () => axios.post(this.getUrl(url), data, this.config(params, options)), errorHandler);
  }

  async put(url, data, params, options, errorHandler) {
    return this.promise('put', () => axios.put(this.getUrl(url), data, this.config(params, options)), errorHandler);
  }

  async patch(url, data, params, options, errorHandler) {
    return this.promise('patch', () => axios.patch(this.getUrl(url), data, this.config(params, options)), errorHandler);
  }

  async delete(url, data, params, options, errorHandler) {
    const config = this.config(params, options);
    config.data = data;
    return this.promise('delete', () => axios.delete(this.getUrl(url), config), errorHandler);
  }

  getUrl(url, {query = ''} = {}) {
    if (url.indexOf('http://') != 0 && url.indexOf('https://') != 0) {
      url = this.getServerAppUrl() + 'rest/ng/' + url;
    }

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

  config(params, options = {}) {
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

    return {headers: this.headers, params: params, ...options};
  }

  notifyStart(method) {
    this.listeners.forEach(listener => listener.callStarted({method}));
  }

  notifyComplete(method, response) {
    this.listeners.forEach(listener => listener.callCompleted({method, response}));
  }

  notifyFail(method, response) {
    this.listeners.forEach(listener => listener.callFailed({method, response}));
  }

  handleError(resp) {
    if (typeof resp == 'string') {
      alertSvc.error(resp);
    } else if (resp && typeof resp == 'object') {
      if (resp.status == 401) {
        localStorage.removeItem('osAuthToken');
        routerSvc.ngGoto('', {logout: true});
        return;
      }

      const errors = resp.data;
      if (errors instanceof Array) {
        const msg = errors.map(err => err.message + ' (' + err.code + ')').join(',');
        alertSvc.error(msg);
      } else if (errors) {
        alertSvc.error(errors);
      } else {
        alertSvc.error(resp.status + ': ' + resp.statusText);
      }
    } else {
      alert(resp);
    }
  }

  promise(method, apiCall, errorHandler) {
    this.notifyStart(method);
    return new Promise((resolve, reject) => {
      apiCall()
        .then(resp => {
          this.notifyComplete(method, resp);
          resolve(resp.data);
        })
        .catch(e => {
          this.notifyFail(method, e.response || e.message);
          reject(e);
          if (typeof errorHandler == 'function') {
            errorHandler(resolve, e.response || e.message);
            return;
          }

          this.handleError(e.response || e.message);
        });
    });
  }
}

export default new HttpClient();
