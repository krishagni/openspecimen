
class Utility {
  evaluator = function(expr, variables) {
    function paramsList() {
      var params = '';
      for (let key in variables) {
        if (Object.prototype.hasOwnProperty.call(variables, key)) {
          params += ',' + key;
        }
      }

      return params.length ? params.substring(1) : params;
    }

    function valuesList() {
      var values = [];
      for (let key in variables) {
        if (Object.prototype.hasOwnProperty.call(variables, key)) {
          values.push(variables[key]);
        }
      }

      return values;
    }

    let fn = null;
    eval('fn = function(' + paramsList() + ') { return ' + expr + '; };');
    return fn.apply(null, valuesList()); 
  }

  //
  // TODO: check the utility of this method
  //
  eval(expr, vars) {
    return this.evaluator(expr, vars);
  }
}

export default new Utility();
