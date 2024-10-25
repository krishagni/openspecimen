import formsCache from './FormsCache.js';

class Util {
  symbols = {
    'ANY':         'any',
    'BETWEEN':     'between',
    'CONTAINS':    'contains',
    'ENDS_WITH':   'ends with',
    'EQ':          '=',
    'EXISTS':      'exists',
    'GE':          '>=',
    'GT':          '>',
    'IN':          'in',
    'LE':          '<=',
    'LT':          '<',
    'NE':          '!=',
    'NOT_EXISTS':  'not exists',
    'NOT_IN':      'not in',
    'STARTS_WITH': 'starts with'
  };

  propIdFields = {
    'Participant.ppid' : [{expr: 'Participant.id', caption: '$cprId'}],
    'Specimen.label'   : [
                           {expr: 'Specimen.id', caption: '$specimenId'},
                           {expr: 'CollectionProtocol.id', caption: '$cpId'}
                         ],
    'Specimen.parentSpecimen.parentLabel': [
                           {expr: 'Specimen.parentSpecimen.parentId', caption: '$parentSpecimenId'},
                           {expr: 'CollectionProtocol.id', caption: '$cpId'}
                         ]
  };

  async getCountAql(query) {
    if (!query.filters || !query.queryExpression) {
      return '';
    }

    const filtersMap = query.filters.reduce(
      (map, filter) => {
        map[filter.id] = filter;
        return map;
      },
      {}
    );

    let whereClause = await this._getWhereClause(query.cpId, filtersMap, query.queryExpression);
    return '' +
      'select ' +
      ' count(distinct Participant.id) as "cprCnt", ' +
      ' count(distinct SpecimenCollectionGroup.id) as "visitCnt", ' +
      ' count(distinct Specimen.id) as "specimenCnt" ' +
      'where ' + 
        whereClause;
  }

  async getDataAql(query, addPropIds, addLimit) {
    if (!query.filters || !query.queryExpression) {
      return '';
    }

    const filtersMap = query.filters.reduce(
      (map, filter) => {
        map[filter.id] = filter;
        return map;
      },
      {}
    );

    const selectClause = this._getSelectClause(filtersMap, query.selectList, addPropIds);
    const whereClause  = await this._getWhereClause(query.cpId, filtersMap, query.queryExpression);

    let aql = 'select ' + selectClause + ' where ' + whereClause;
    if (addLimit) {
      aql += ' limit 0, 1000';
    }

    return aql;
  }

  _getSelectClause(filtersMap, selectedFields, addPropIds) {
    const addedIds = {};

    let result = '';
    for (let field of selectedFields) {
      let fieldName = null;

      if (typeof field == 'string') {
          fieldName = field;
          field = this._getFieldExpr(filtersMap, {name: field}, true);
      } else if (typeof field != 'string') {
        if (field.aggFns && field.aggFns.length > 0) {
          const fieldExpr = this._getFieldExpr(filtersMap, field);
          let fnExprs = '';
          for (let aggFn of field.aggFns) {
            if (fnExprs.length > 0) {
              fnExprs += ', ';
            }

            if (aggFn.name == 'count') {
              fnExprs += 'count(distinct ';
            } else if (aggFn.name == 'c_count') {
              fnExprs += 'c_count(distinct ';
            } else {
              fnExprs += aggFn.name + '(';
            }

            fnExprs += fieldExpr + ') as "' + aggFn.desc + ' "';
          }

          field = fnExprs;
        } else {
          fieldName = field.name;
          field = this._getFieldExpr(filtersMap, field, true);
        }
      }

      if (addPropIds) {
        for (let prop of Object.keys(this.propIdFields)) {
          this.propIdFields[prop].forEach(
            idField => {
              if (fieldName != prop || addedIds[idField.expr]) {
                return;
              }

              result += idField.expr + ' as "' + idField.caption + '", ';
              addedIds[idField.expr] = true;
            }
          );
        }
      }

      result += field + ', ';
    }

    if (result) {
      result = result.substring(0, result.length - 2);
    }

    return result;
  }

  async _getWhereClause(cpId, filtersMap, expressionNodes) {
    let whereClause = '';
    for (const {nodeType, value} of expressionNodes) {
      if (nodeType == 'OPERATOR') {
        whereClause += value.toLowerCase();
      } else if (nodeType == 'PARENTHESIS') {
        whereClause += (value == 'LEFT' ? '(' : ')');
      } else if (nodeType == 'FILTER') {
        whereClause += await this._getFilterExpr(cpId, filtersMap, value);
      }

      whereClause += ' ';
    }

    return whereClause;
  }

  _getFieldExpr(filtersMap, field, includeDesc) {
    const fieldName = field.name;
    const temporalMarker = '$temporal.';
    if (fieldName.indexOf(temporalMarker) != 0) {
      let alias = includeDesc && field.displayLabel ? ' as "' + field.displayLabel + '"' : '';
      return fieldName + alias;
    }

    const filterId = fieldName.substring(temporalMarker.length);
    const filter   = filtersMap[filterId] || {};
    let expr       = this._getTemporalExprObj(filter.expr).lhs;
    if (includeDesc) {
      if (field.displayLabel) {
        expr += ' as "' + field.displayLabel + '"';
      } else {
        expr += ' as "' + filter.desc + '"';
      }
    }

    return expr;
  }

  async _getFilterExpr(cpId, filtersMap, filterId) {
    const filter = filtersMap[filterId];
    if (filter == null) {
      alert('Invalid filter: ' + filterId);
      return '';
    }

    if (filter.expr) {
      const tObj = this._getTemporalExprObj(filter.expr);
      if (this._isUndef(tObj.lhs) && this._isUndef(tObj.op)) {
        return filter.expr;
      }
            
      if (this._isUndef(tObj.lhs) || this._isUndef(tObj.op)) {
        alert('Invalid temporal expression: ' + filter.expr);
        return '1 = 0';  
      }
      
      if (this._isUndef(tObj.rhs)) {
        if (!filter.parameterized) {
          alert('Invalid temporal expression: ' + filter.expr + '. No RHS and not parameterized.');
          return '1 = 0';
        } else {
          return tObj.lhs + ' any ';
        }
      }
      
      return filter.expr;
    }

    const field = await formsCache.getField(cpId, null, filter.field);
    if (!field) {
      return '';
    }

    let expr = filter.field;
    if (filter.op == 'EXISTS' || filter.op == 'NOT_EXISTS' || filter.op == 'ANY') {
      expr += ' ' + this.symbols[filter.op];
      return expr;
    }

    if (filter.hasSq) {
      alert('TODO');
      return '';
    }

    if (this._isUndef(filter.values) && (field.type == 'DATE' || field.type == 'INTEGER' || field.type == 'FLOAT')) {
      return expr + ' any';
    }
        
    expr += ' ' + this.symbols[filter.op];
      
    let filterValue = '';
    if (field.type == 'STRING' || field.type == 'DATE') {
      if (filter.op == 'IN' || filter.op == 'NOT_IN' || filter.op == 'BETWEEN') {
        filterValue = filter.values.map(item => this._stringLiteral(item));
      } else {
        filterValue = this._stringLiteral(filter.values[0]);
      }
    }   
        
    if (filter.op == 'IN' || filter.op == 'NOT_IN' || filter.op == 'BETWEEN') {
      filterValue = '(' + filterValue.join() + ')';
    }   
          
    return expr + ' ' + filterValue;
  }

  _isUndef(value) {
    if (value == null || value == undefined || (typeof value == 'string' && value.trim() == '')) {
      return true;
    }
        
    if (value instanceof Array) {
      return value.every(item => item == null || item == undefined || item.toString().trim() == '');
    }   
        
    return false;
  }

  _stringLiteral(value) {
    return '"' + this._escapeQuotes(value) + '"';
  }

  _escapeQuotes(value) {
    return value.replace(/"/g, '\\"');
  }

  _getTemporalExprObj(temporalExpr) {
    const qre = /"(.*?)"|'(.*?)'/;
    const placeholders = {};

    let qmatches = undefined;
    let i = 0;
    while ((qmatches = qre.exec(temporalExpr))) {
      const match = qmatches[0];
      placeholders['$RX' + i] = match;
        
      temporalExpr = temporalExpr.substring(0, qmatches.index) +
        '$RX' + i +
        temporalExpr.substring(qmatches.index + match.length);
      ++i;
    }

    const re = /<=|>=|<|>|=|!=|\sbetween\s|\)any|\sany|\)exists|\sexists/g
    let matches = undefined;
    if ((matches = re.exec(temporalExpr))) {
      let lhs = temporalExpr.substring(0, matches.index);
      if (lhs) {
        Object.keys(placeholders).forEach(placeholder => lhs = lhs.replaceAll(placeholder, placeholders[placeholder]));
      }

      let rhs = temporalExpr.substring(matches.index + matches[0].length);
      if (rhs) {
        Object.keys(placeholders).forEach(placeholder => rhs = rhs.replaceAll(placeholder, placeholders[placeholder]));
      }

      return { lhs: lhs, op : matches[0].trim(), rhs: rhs }
    }

    return {};
  }
}

export default new Util();
