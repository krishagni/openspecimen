
class Util {
  httpsRe = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|])/gim;

  wwwRe   = /(^|[^/])(www\.[\S]+(\b|$))/gim;

  mailRe  = /(([a-zA-Z0-9-_.])+@[a-zA-Z_]+?(\.[a-zA-Z]{2,6})+)/gim;

  queryString(params) {
    return Object.keys(params || {}).sort().reduce(
      (result, param) => {
        if (result) {
          result += '&';
        }

        if (params[param]) {
          result += param + '=' + params[param];
        }

        return result;
      },
      ''
    );
  }

  uriEncode(params) {
    if (!params || Object.keys(params).length <= 0) {
      return undefined;
    }

    let curatedParams = {};
    for (const [key, value] of Object.entries(params)) {
      if (value || value == 0 || value == false || value == 'false') {
        curatedParams[key] = value;
      }
    }

    let result = undefined;
    if (Object.keys(curatedParams).length > 0) {
      result = btoa(encodeURIComponent(JSON.stringify(curatedParams)));
    }

    return result;
  }

  linkify(text) {
    if (!text) {
      return text;
    }

    return text.replace(this.httpsRe, '<a href="$1" target="_blank">$1</a>')
      .replace(this.wwwRe, '$1<a href="http://$2" target="_blank">$2</a>')
      .replace(this.mailRe, '<a href="mailto:$1">$1</a>');
  }
}

export default new Util();
