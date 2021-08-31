
import jsep from 'jsep/dist/jsep.js';

class ExpressionUtil {
  cachedExprs = {};

  constructor() {
    jsep.addBinaryOp('or',  1);
    jsep.addBinaryOp('and', 2);
  }

  eval(context, expr) {
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

}

export default new ExpressionUtil();
