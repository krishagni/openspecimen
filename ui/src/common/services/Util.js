
class Util {
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
}

export default new Util();
