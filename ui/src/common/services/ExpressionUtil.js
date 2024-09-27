
import jsep from 'jsep';

class ExpressionUtil {
  cachedExprs = {};

  /** Picked from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects **/
  builtInObjs = {
    /** Value Properties **/
    Infinity, NaN, undefined,

    /** Function Properties **/
    isFinite, isNaN, parseFloat, parseInt, decodeURI, decodeURIComponent, encodeURI, encodeURIComponent,
    escape, unescape, console,

    /** Fundamental Objects **/
    Object, Function, Boolean, Symbol,

    /** Error objects **/
    Error, EvalError, RangeError, ReferenceError, SyntaxError, TypeError, URIError,

    /** Numbers and Dates **/
    Number, Math, Date,

    /** Text Processing **/
    String, RegExp,

    /** Indexed Collections **/
    Array,

    /** Keyed Collections **/
    Map, Set, WeakMap, WeakSet,

    /** Structured data **/
    ArrayBuffer, JSON,

    /** Control abstraction objects **/
    Promise,

    /** Internationalization **/
    Intl
  };

  constructor() {
    jsep.addBinaryOp('or',  1);
    jsep.addBinaryOp('and', 2);
  }

  eval(context, expr) {
    if (context == undefined || context == null) {
      return context;
    } else if (context.fd == null || context.fd == undefined) {
      context.fd = this.fd;
    } else if (typeof context.fd != 'function') {
      alert('Dev error - FD is not a function');
      return;
    }

    return new Function('return ' + this.parse(expr)).call(context);
  }

  evalJavaScript(src, context) {
    context = context || {};

    function has() {
      return true;
    }

    function get(target, key) {
      return key == Symbol.unscopables ? undefined : target[key];
    }

    const jsSrc  = 'with (sandbox) {' + src + '}'
    const jsCode = new Function('sandbox', jsSrc);
    return (function(sandbox) {
      const sandboxProxy = new Proxy(sandbox, {has, get})
      return jsCode(sandboxProxy)
    })({...this.builtInObjs, ...context, osSvc: {}, osUi: {...window.osUi}});
  }

  parse(expr) {
    let parsed = this.cachedExprs[expr];
    if (!parsed) {
      parsed = this.cachedExprs[expr] = this.decorate(jsep(expr));
    }

    return parsed;
  }

  decorate(exprTree) {
    if (exprTree.type == 'BinaryExpression') {
      let left  = this.decorate(exprTree.left);
      let right = this.decorate(exprTree.right);
      let op    = exprTree.operator;
      if (op == 'and') {
        op = '&&';
      } else if (op == 'or') {
        op = '||';
      }

      return '(' + left + ') ' + op + ' (' + right + ')';
    } else if (exprTree.type == 'UnaryExpression') {
      let arg = this.decorate(exprTree.argument);
      if (exprTree.prefix) {
        return exprTree.operator + arg;
      } else {
        return arg + exprTree.operator;
      }
    } else if (exprTree.type == 'MemberExpression') {
      let objectExpr = exprTree.property.name;
      let object = exprTree.object;
      while (object) {
        if (object.type == 'MemberExpression') {
          objectExpr += '.' + object.property.name;
        } else if (object.type == 'Identifier') {
          objectExpr += '.' + object.name;
        }

        object = object.object;
      }

      return 'this.fd(\'' + objectExpr.split('.').reverse().join('.') + '\')';
    } else if (exprTree.type == 'Identifier') {
      return 'this.fd(\'' + exprTree.name + '\')';
    } else if (exprTree.type == 'Literal') {
      return exprTree.raw;
    } else if (exprTree.type == 'CallExpression') {
      let object = this.decorate(exprTree.callee.object);
      if (object instanceof Array) {
        object = '[' + object + ']';
      }

      let method = exprTree.callee.property.name;
      let args = [];
      for (let i = 0; i < (exprTree.arguments || []).length; ++i) {
        if (i > 0) {
          args += ',';
        }

        let arg = this.decorate(exprTree.arguments[i]);
        if (arg instanceof Array) {
          arg = '[' + arg + ']';
        }

        args += arg;
      }

      return '(' + object + ')?.' + method + '(' + args + ')';
    } else if (exprTree.type == 'ConditionalExpression') {
      const test = this.decorate(exprTree.test);
      const consequent = this.decorate(exprTree.consequent);
      const alternate = this.decorate(exprTree.alternate);
      return '(' + test + ' ? ' + consequent + ' : ' + alternate + ')';
    } else if (exprTree.type == 'ArrayExpression') {
      return exprTree.elements.map(element => this.decorate(element));
    } else {
      console.log(exprTree);
      console.log('Could not identify the expression type: ' + exprTree.type + ', ' + JSON.stringify(exprTree));
      return JSON.stringify(exprTree);
    } 
  }

  fd(name) {
    let object = this;
    if (!name) {
      return object;
    }

    let props = name.split('.');
    for (let i = 0; i < props.length; ++i) {
      if (!object) {
        return undefined;
      }
      object = object[props[i]];
    }

    return object;
  }

  getValue(object, name) {
    if (!name) {
      return object;
    }

    let props = name.split('.');
    for (let i = 0; i < props.length; ++i) {
      if (!object) {
        return undefined;
      }
      object = object[props[i]];
    }

    return object;
  }

  setValue(object, name, value) {
    if (!object || !name) {
      return;
    }

    let props = name.split('.');
    for (let i = 0; i < props.length - 1; ++i) {
      object = object[props[i]] = object[props[i]] || {};
    }

    object[props[props.length - 1]] = value;
  }
}

export default new ExpressionUtil();
