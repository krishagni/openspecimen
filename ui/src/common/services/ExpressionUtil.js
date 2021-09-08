
import jsep from 'jsep/dist/jsep.js';

class ExpressionUtil {
  cachedExprs = {};

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

      return left + ' ' + op + ' ' + right;
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
    if (!name) {
      return;
    }

    object = object || {};
    let props = name.split('.');
    for (let i = 0; i < props.length - 1; ++i) {
      object = object[props[i]] || {};
    }

    object[props[props.length - 1]] = value;
  }
}

export default new ExpressionUtil();
