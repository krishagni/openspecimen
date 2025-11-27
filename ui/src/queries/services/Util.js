import i18n from '@/common/services/I18n.js';

import formsCache    from './FormsCache.js';
import queriesCache  from './QueriesCache.js';

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

  async getCountAql(query, facets) {
    if (!query.filters || !query.queryExpression) {
      return '';
    }

    const filtersMap = this._getFiltersMap(query);
    const whereClause = await this._getWhereClause('', query, filtersMap, facets);
    return '' +
      'select ' +
      ' count(distinct Participant.id) as "cprCnt", ' +
      ' count(distinct SpecimenCollectionGroup.id) as "visitCnt", ' +
      ' count(distinct Specimen.id) as "specimenCnt" ' +
      'where ' + 
        whereClause;
  }

  async getDataAql(query, facets, addPropIds, addLimit) {
    if (!query.filters || !query.queryExpression) {
      return '';
    }

    const filtersMap = this._getFiltersMap(query);
    addPropIds = addPropIds && (!query.reporting || query.reporting.type != 'crosstab');

    const selectClause = this._getSelectClause(query, filtersMap, addPropIds);
    const whereClause  = await this._getWhereClause('', query, filtersMap, facets);
    const havingClause = this._getHavingClause(query);
    const reportClause = this._getRptExpr(query);

    let aql = 'select ' + selectClause + ' where ' + whereClause + ' ' + havingClause;
    if (addLimit) {
      aql += ' limit 0, 1000';
    }

    return aql + ' ' + reportClause;
  }

  async getWhereAql(query) {
    const filtersMap = this._getFiltersMap(query);
    return this._getWhereClause('', query, filtersMap);
  }

  parseTemporalExpression(expr) {
    return this._getTemporalExprObj(expr);
  }

  getFilterDesc(filter) {
    filter = filter || {};
    if (filter.expr) {
      return filter.desc || i18n.msg('queries.unknown');
    } else if (filter.fieldObj) {
      const {formCaption, caption} = filter.fieldObj;
      const op = i18n.msg('queries.op.' + filter.op);

      let desc = formCaption.join(' >> ') + ' >> ' + caption + ' ' + op;
      if (filter.values) {
        desc += ' ' + filter.values.join(', ');
      } else if (filter.subQuery) {
        desc += ' ' + filter.subQuery.title;
      } else if (filter.subQueryId) {
        desc += ' ' + filter.subQueryId;
      }

      return desc;
    } else {
      return filter.id || i18n.msg('queries.unknown');
    }
  }

  isValidExpr(exprNodes) {
    let parenCnt = 0, next = 'FILTER', last = 'FILTER';
    for (let i = 0; i < exprNodes.length; ++i) {
      const {nodeType, value} = exprNodes[i];
      if (nodeType == 'PARENTHESIS' && value == 'LEFT') {
        ++parenCnt;
        continue;
      } else if (nodeType == 'PARENTHESIS' && value == 'RIGHT' && last != 'OPERATOR') {
        --parenCnt;
        if (parenCnt < 0) {
          return false;
        }
        continue;
      } else if (nodeType == 'OPERATOR' && value == 'NTHCHILD' && next == 'FILTER') { 
        if (i + 1 < exprNodes.length) {
          const nextToken = exprNodes[i + 1];
          if (nextToken.nodeType == 'PARENTHESIS' && nextToken.value == 'LEFT') {
            ++parenCnt;
            ++i;
            last = 'OPERATOR';
            continue;
          }
        }

        return false;
      } else if (nodeType == 'OPERATOR' && value == 'NOT' && next == 'FILTER') {
        last = 'OPERATOR';
        continue;
      } else if (nodeType == 'OPERATOR' && next != 'OPERATOR') {
        return false;
      } else if (nodeType == 'FILTER' && next != 'FILTER') {
        return false;
      } else if (nodeType == 'OPERATOR' && next == 'OPERATOR' && value != 'NOT' && value != 'NTHCHILD') {
        next = 'FILTER';
        last = 'OPERATOR';
        continue;
      } else if (nodeType == 'FILTER' && next == 'FILTER') {
        next = 'OPERATOR';
        last = 'FILTER';
        continue;
      } else {
        return false;
      }
    }

    return parenCnt == 0 && last == 'FILTER';
  }

  _getSelectClause(query, filtersMap, addPropIds) {
    const addedIds = {};

    let result = '';
    const propIdsList = [];
    for (let field of query.selectList || []) {
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

              propIdsList.push(idField.expr + ' as "' + idField.caption + '"');
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

    if (propIdsList.length > 0) {
      result += ', ' + propIdsList.join(', ');
    }

    return result;
  }

  _getFiltersMap(query) {
    return (query.filters || []).reduce(
      (map, filter) => {
        map[filter.id] = filter;
        return map;
      },
      {}
    );
  }

  async _getWhereClause(prefix, query, filtersMap, facets) {
    let whereClause = '';
    for (const {nodeType, value} of query.queryExpression || []) {
      if (nodeType == 'OPERATOR') {
        whereClause += value.toLowerCase();
      } else if (nodeType == 'PARENTHESIS') {
        whereClause += (value == 'LEFT' ? '(' : ')');
      } else if (nodeType == 'FILTER') {
        whereClause += await this._getFilterExpr(prefix, query.cpId, query.cpGroupId, filtersMap, facets, value);
      }

      whereClause += ' ';
    }

    return whereClause;
  }

  _getHavingClause(query) {
    if (!query.havingClause) {
      return '';
    }

    let havingClause = query.havingClause.replace(/count\s*\(/g, 'count(distinct ');
    havingClause = havingClause.replace(/c_count\s*\(/g, 'c_count(distinct ');
    return 'having ' + havingClause;
  }

  _getRptExpr(query) {
    const {type, params} = query.reporting || {type: 'none', params: {}};
    if (!type || type == 'none') {
      return '';
    }

    if (type == 'specimenqty') {
      return 'specimenqty("' +
          params.restrictBy + '", "' +
          (params.minQty || -1) + '", "' +
          (params.maxQty || - 1) + '"' +
        ')';
    }

    const rptFields = this._getReportFields(query.selectList, true);
    if (type == 'columnsummary') {
      return this._getColumnSummaryRptExpr(rptFields, query.reporting);
    }

    if (type != 'crosstab') {
      return type;
    }

    let rowIdx = this._getFieldIndices(rptFields, params.groupRowsBy);
    let colIdx = this._getFieldIndices(rptFields, [params.groupColBy]);
    colIdx = colIdx.length > 0 ? colIdx[0] : undefined;

    let summaryIdx    = this._getFieldIndices(rptFields, params.summaryFields);
    let rollupExclIdx = this._getFieldIndices(rptFields, params.rollupExclFields);

    let includeSubTotals = '';
    if (params.includeSubTotals) {
      includeSubTotals = ', true';
    }

    for (let i = 0; i < summaryIdx.length; ++i) {
      if (rollupExclIdx.indexOf(summaryIdx[i]) != -1) {
        summaryIdx[i] = -1 * summaryIdx[i];
      }
    }

    return 'crosstab(' +
      '(' + rowIdx.join(',') + '), ' +
      colIdx + ', ' +
      '(' + summaryIdx.join(',') + ') ' +
      includeSubTotals +
    ')';
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

  async _getFilterExpr(prefix, cpId, cpGroupId, filtersMap, facets, filterId) {
    const filter = filtersMap[filterId];
    if (filter == null) {
      alert('Invalid filter: ' + filterId);
      return '';
    }

    if (filter.expr) {
      const tObj = this._getTemporalExprObj(filter.expr);
      if (this._isUndef(tObj.lhs) && this._isUndef(tObj.op)) {
        return filter.expr;
      } else if (this._isUndef(tObj.lhs) || this._isUndef(tObj.op)) {
        alert('Invalid temporal expression: ' + filter.expr);
        return '1 = 0';  
      }
      
      const facetExpr = this._getFacetExpr(prefix, filter, tObj.lhs, facets);
      if (facetExpr) {
        return facetExpr;
      } else if (this._isUndef(tObj.rhs)) {
        if (!filter.parameterized) {
          alert('Invalid temporal expression: ' + filter.expr + '. No RHS and not parameterized.');
          return '1 = 0';
        } else {
          return tObj.lhs + ' any ';
        }
      } else {
        return filter.expr;
      }
    }

    const field = await formsCache.getField(cpId, cpGroupId, filter.field);
    if (!field) {
      return '';
    }

    const facetExpr = this._getFacetExpr(prefix, filter, filter.field, facets);
    if (facetExpr) {
      return facetExpr;
    }

    let expr = filter.field;
    if (filter.op == 'EXISTS' || filter.op == 'NOT_EXISTS' || filter.op == 'ANY') {
      expr += ' ' + this.symbols[filter.op];
      return expr;
    }

    if (filter.hasSq || filter.subQueryId > 0) {
      const subQuery = await queriesCache.getQuery(filter.subQueryId);
      const sqFiltersMap = this._getFiltersMap(subQuery);
      const sqWhere = await this._getWhereClause(prefix + filter.id + '.', subQuery, sqFiltersMap, facets);

      expr += ' ' + this.symbols[filter.op] + ' ';
      expr += '(select ' + filter.field + ' where ' + sqWhere + ')';
      return expr;
    }

    if (this._isUndef(filter.values) && (field.type == 'DATE' || field.type == 'INTEGER' || field.type == 'FLOAT')) {
      return expr + ' any';
    }
        
    expr += ' ' + this.symbols[filter.op];

    let filterValue = filter.values.length > 0 ? filter.values[0] : '';
    if (field.type == 'STRING' || field.type == 'DATE') {
      if (filter.op == 'IN' || filter.op == 'NOT_IN' || filter.op == 'BETWEEN') {
        filterValue = filter.values.map(item => this._stringLiteral(item));
      } else {
        filterValue = this._stringLiteral(filter.values[0]);
      }
    } else if (filter.op == 'IN' || filter.op == 'NOT_IN' || filter.op == 'BETWEEN') {
      filterValue = filter.values.length > 0 ? filter.values : '';
    }
        
    if (filter.op == 'IN' || filter.op == 'NOT_IN' || filter.op == 'BETWEEN') {
      filterValue = '(' + filterValue.join() + ')';
    }   
          
    return expr + ' ' + filterValue;
  }

  _getFacetExpr(prefix, filter, lhs, facets) {
    const facetId = prefix + filter.id;
    const facet = (facets || []).find(f => f.id == facetId);
    if (!facet || !facet.values || facet.values.length == 0) {
      return null;
    }

    let expr = null;
    if (facet.type == 'INTEGER' || facet.type == 'FLOAT' || facet.type == 'DATE') {
      const {minValue, maxValue} = facet.values;
      const literal = facet.type == 'DATE' ? (l) => this._stringLiteral(l) : (l) => l;

      if ((minValue || minValue === 0) && (maxValue || maxValue === 0)) {
        expr = '(' + lhs + ' >= ' + literal(minValue) + ' and ' + lhs + ' <= ' + literal(maxValue) + ')';
      } else if (minValue || minValue === 0) {
        const relOp = facet.op == 'GT' ? ' > ' : ' >= ';
        expr = '(' + lhs + relOp + literal(minValue) + ')';
      } else if (maxValue || maxValue === 0) {
        const relOp = facet.op == 'LT' ? ' < ' : ' <= ';
        expr = '(' + lhs + relOp + literal(maxValue) + ')';
      }
    } else if (facet.type == 'BOOLEAN') {
      expr = lhs + ' in (' + facet.values.map(v => v == 'Yes' ? 1 : (v == 'No' ? 0 : v)).join(', ') + ')';
    } else {
      expr = lhs + ' in ("' + facet.values.join('", "') + '")';
    }

    return expr;
  }

  _getReportFields(selectedFields, fresh) {
    const reportFields = [];
    for (let field of selectedFields) {
      let isAgg = false;
      for (let aggFn of field.aggFns || []) {
        if (fresh || aggFn.opted) {
          reportFields.push({
            id: field.name + '$' + aggFn.name,
            name: field.name,
            value: aggFn.desc,
            aggFn: aggFn.name
          });
          isAgg = true;
        }
      }

      if (!isAgg) {
        reportFields.push({
          id: field.name,
          name: field.name,
          value: field.form + ": " + field.label
        });
      }
    }

    return reportFields;
  }

  _getColumnSummaryRptExpr(rptFields, {params}) {
    let expr = 'columnsummary(';
    let addComma = false;
    if (params.sum && params.sum.length > 0) {
      expr += '"sum", "' + params.sum.length + '",';
      expr += this._getFieldIndices(rptFields, params.sum).map(idx => '"' + idx + '"').join(',');
      addComma = true;
    }

    if (params.avg && params.avg.length > 0) {
      if (addComma) {
        expr += ", ";
      }

      expr += '"avg","' + params.avg.length + '",';
      expr += this._getFieldIndices(rptFields, params.avg).map(idx => '"' + idx + '"').join(',');
    }

    expr += ')';
    return expr;
  }

  _getFieldIndices(fields, reportFields) {
    reportFields = reportFields || [];

    const indices = [];
    for (const rptField of reportFields) {
      for (let idx = 0; idx < fields.length; ++idx) {
        const selField = typeof fields[idx] == 'string' ? fields[idx] : fields[idx].id;
        if (selField == rptField.id) {
          indices.push(idx + 1);
          break;
        }
      }
    }

    return indices;
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
