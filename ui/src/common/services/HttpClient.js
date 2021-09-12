
import axios from 'axios';
import alertSvc from './Alerts.js';
import routerSvc from './Router.js';

class HttpClient {
  protocol = '';

  host = '';

  port = '';

  path = '';

  headers = {};

  constructor() {
  }

  get(url, params) {
    return this.promise(() => axios.get(this.getUrl(url), this.config(params)));
  }

  async post(url, data, params) {
    return this.promise(() => axios.post(this.getUrl(url), data, this.config(params)));
  }

  async put(url, data, params) {
    return this.promise(() => axios.put(this.getUrl(url), data, this.config(params)));
  }

  async delete(url, data, params) {
    return this.promise(() => axios.delete(this.getUrl(url), this.config(params), data));
  }

  getUrl(url) {
    return this.getServerAppUrl() + 'rest/ng/' + url;
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

  promise(apiCall) {
    return new Promise((resolve) => {
      apiCall()
        .then(resp => resolve(resp.data))
        .catch(e => {
          if (e.response.status == 401) {
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
