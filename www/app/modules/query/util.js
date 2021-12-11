
angular.module('os.query.util', ['os.query.models', 'os.query.save'])
  .factory('QueryUtil',function($translate, $document, $modal, $state, $stateParams, Alerts, SavedQuery) {
    var ops = SavedQuery.ops;

    var propIdFields = {
      'Participant.ppid' : [{expr: 'Participant.id', caption: '$cprId'}],
      'Specimen.label'   : [
                             {expr: 'Specimen.id', caption: '$specimenId'},
                             {expr: 'CollectionProtocol.id', caption: '$cpId'}
                           ],
      'Specimen.parentSpecimen.parentLabel': [
                             {expr: 'Specimen.parentSpecimen.parentId', caption: '$parentSpecimenId'},
                             {expr: 'CollectionProtocol.id', caption: '$cpId'}
                           ]
    }

    var init = false;

    function initOpsDesc() {
      if (init) {
        return;
      }

      $translate('queries.list').then(
        function() {
          angular.forEach(ops, function(op) {
            op.desc = $translate.instant('queries.ops.' + op.name);
          });
          init = true;
        }
      );
    }

    var searchOp = function(fn) {
      var result = undefined;
      for (var k in ops) {
        if (fn(ops[k])) {
          result = ops[k];
          break;
        }
      }

      return result;
    };

    var getOpBySymbol = function(symbol) {
      return searchOp(function(op) { return op.symbol == symbol });
    };
      
    var getOpByModel = function(model) {
      return searchOp(function(op) { return op.model == model });
    };


    function getAllowedOps(field) {
      var result = null;
      if (field.type == "STRING") {
        result = getStringOps();
      } else {
        result = getNumericOps();
      }

      return result;
    };

    function getStringOps() {
      return [ 
        ops.eq, ops.ne, 
        ops.exists, ops.not_exists, ops.any,
        ops.starts_with, ops.ends_with, 
        ops.contains, ops.qin, ops.not_in
      ];
    };

    function getNumericOps() {
      return [
        ops.eq, ops.ne, 
        ops.lt, ops.le, 
        ops.gt, ops.ge, 
        ops.exists, ops.not_exists, ops.any,
        ops.qin, ops.not_in, 
        ops.between
      ];
    };

    function getValueType(field, op) {
      if (!field || !op) {
        return "text";
      } else if (op && (op.name == "qin" || op.name == "not_in")) {
        if (field.lookupProps) {
          return "lookupMultiple";
        } else {
          return field.pvs ? "multiSelect" : "tagsSelect";
        }
      } else if (field.lookupProps && !(op.name == 'contains' || op.name == 'starts_with' || op.name == 'ends_with')) {
        return "lookupSingle";
      } else if (op && op.name == "between") {
        return field.type == "DATE" ? "betweenDate" : "betweenNumeric";
      } else if (field.pvs && !(op.name == 'contains' || op.name == 'starts_with' || op.name == 'ends_with')) {
        return "select";
      } else if (field.type == "DATE") {
        return "datePicker";
      } else {
        return "text";
      }
    };

    function isUnaryOp(op) {
      return op && (op.name == 'exists' || op.name == 'not_exists' || op.name == 'any');
    }

    function onOpSelect(filter) {
      if (!filter.op) {
        return;
      }

      if (filter.op.name == "between") {
        filter.value = [undefined, undefined];
      } else if (filter.op.name == 'qin' || filter.op.name == 'not_in') {
        filter.value = [];
      } else {
        filter.value = undefined;
      }

      filter.valueType = getValueType(filter.field, filter.op);
      filter.unaryOp = isUnaryOp(filter.op);
      filter.hasSq = false;
      filter.subQuery = undefined;
    }

    function hidePopovers() {
      var popups = $document.find('div.popover');
      angular.forEach(popups, function(popup) {
        angular.element(popup).scope().$hide();
      });
    }

    function getOp(name) {
      return ops[name];
    }

    function isValidQueryExpr(exprNodes) {
      var parenCnt = 0, next = 'filter', last = 'filter';

      for (var i = 0; i < exprNodes.length; ++i) {
        var exprNode = exprNodes[i];

        if (exprNode.type == 'paren' && exprNode.value == '(') {
          ++parenCnt;  
          continue;
        } else if (exprNode.type == 'paren' && exprNode.value == ')' && last != 'op') {
          --parenCnt;
          if (parenCnt < 0) {
            return false;
          }
          continue;
        } else if (exprNode.type == 'op' && exprNode.value == 'nthchild' && next == 'filter') { 
          if (i + 1 < exprNodes.length) {
            var nextToken = exprNodes[i + 1];
            if (nextToken.type == 'paren' && nextToken.value == '(') {
              ++parenCnt;
              ++i;
              last = 'op';
              continue;
            }
          }

          return false; 
        } else if (exprNode.type == 'op' && exprNode.value == 'not' && next == 'filter') {
          last = 'op';
          continue;
        } else if (exprNode.type == 'op' && next != 'op') {
          return false;
        } else if (exprNode.type == 'filter' && next != 'filter') {
          return false;
        } else if (exprNode.type == 'op' && next == 'op' && exprNode.value != 'not' && exprNode.value != 'nthchild') {
          next = 'filter';
          last = 'op';
          continue;
        } else if (exprNode.type == 'filter' && next == 'filter') {
          next = 'op';
          last = 'filter';
          continue;
        } else {
          return false;
        }
      }

      return parenCnt == 0 && last == 'filter';
    };

    function escapeQuotes(value) {
      return value.replace(/"/g, '\\\"');
    }

    function stringLiteral(value) {
      return "\"" + escapeQuotes(value) + "\"";
    }

    function getFilterExpr(filter) {
      if (filter.expr) {
        return filter.expr;
      }

      if (!filter.form || !filter.field) {
        return '';
      }
          
      var expr = filter.form.name + "." + filter.field.name + " ";
      expr += filter.op.symbol + " ";

      if (filter.op.name == 'exists' || filter.op.name == 'not_exists' || filter.op.name == 'any') {
        return expr;
      }

      if (filter.hasSq) {
        var sqWhere = getWhereExpr(filter.subQuery.context.filtersMap, filter.subQuery.context.exprNodes);
        expr += '(select ' + filter.form.name + '.' + filter.field.name + ' where ' + sqWhere + ')';
        return expr;
      }

      var filterValue = filter.value;
      if (filter.field.type == "STRING" || filter.field.type == "DATE") {
        if (filter.op.name == 'qin' || filter.op.name == 'not_in' || filter.op.name == 'between') {
          filterValue = filterValue.map(stringLiteral);
        } else {
          filterValue = stringLiteral(filterValue);
        }
      }

      if (filter.op.name == 'qin' || filter.op.name == 'not_in' || filter.op.name == 'between') {
        filterValue = "(" + filterValue.join() + ")";
      }

      return expr + filterValue;
    }

    function getWhereExpr(filtersMap, exprNodes) {
      var query = "";
      angular.forEach(exprNodes, function(exprNode) {
        if (exprNode.type == 'paren') {
          query += exprNode.value;
        } else if (exprNode.type == 'op') {
          query += ops[exprNode.value].symbol + " ";
        } else if (exprNode.type == 'filter') {
          query += " " + getFilterExpr(filtersMap[exprNode.value]) + " ";
        }
      });
      return query;
    };

    function getSelectList(selectedFields, filtersMap, addPropIds) {
      var addedIds = {}, result = "";

      angular.forEach(selectedFields, function(field) {
        var selFieldName = null;

        if (typeof field == "string") {
          selFieldName = field;
          field = getFieldExpr(filtersMap, {name: field}, true);
        } else if (typeof field != "string") {
          if (field.aggFns && field.aggFns.length > 0) {
            var fieldExpr = getFieldExpr(filtersMap, field);
            var fnExprs = "";
            for (var j = 0; j < field.aggFns.length; ++j) {
              if (fnExprs.length > 0) {
                fnExprs += ", ";
              }

              var aggFn = field.aggFns[j];
              if (aggFn.name == 'count') {
                fnExprs += 'count(distinct ';
              } else if (aggFn.name == 'c_count') {
                fnExprs += 'c_count(distinct ';
              } else {
                fnExprs += aggFn.name + '(';
              }

              fnExprs += fieldExpr + ") as \"" + aggFn.desc + " \"";
            }

            field = fnExprs;
          } else {
            selFieldName = field.name;
            field = getFieldExpr(filtersMap, field, true);
          }
        }

        if (addPropIds) {
          for (var prop in propIdFields) {
            if (!propIdFields.hasOwnProperty(prop)) {
              continue;
            }

            propIdFields[prop].forEach(
              function(idField) {
                if (selFieldName != prop || addedIds[idField.expr]) {
                  return;
                }

                result += idField.expr + " as \"" + idField.caption + "\"" + ", ";
                addedIds[idField.expr] = true;
              }
            );
          }
        }

        result += field + ", ";
      });

      if (result) {
        result = result.substring(0, result.length - 2);
      }

      return result;
    };

    function getFieldExpr(filtersMap, field, includeDesc) {
      var fieldName = field.name;
      var temporalMarker = '$temporal.';
      if (fieldName.indexOf(temporalMarker) != 0) {
        var alias = '';
        if (includeDesc && !!field.displayLabel) {
          alias = ' as "' + field.displayLabel + '"';
        }

        return fieldName + alias;
      }

      var filterId = fieldName.substring(temporalMarker.length);
      var filter = filtersMap[filterId];
      var expr = getTemporalExprObj(filter.expr).lhs;
      if (includeDesc) {
        if (!!field.displayLabel) {
          expr += ' as "' + field.displayLabel + '"';
        } else {
          expr += ' as "' + filter.desc + '"';
        }
      }

      return expr;
    };

    function getTemporalExprObj(temporalExpr) {
      var re = /<=|>=|<|>|=|!=|\sbetween\s|\)any|\sany|\)exists|\sexists/g
      var matches = undefined;
      if ((matches = re.exec(temporalExpr))) {
        return {
          lhs: temporalExpr.substring(0, matches.index),
          op : matches[0].trim(),
          rhs: temporalExpr.substring(matches.index + matches[0].length)
        }
      }

      return {};
    };

    function getRptExpr(selectedFields, reporting) {
      if (!reporting || reporting.type == 'none') {
        return '';
      }

      if (reporting.type == 'specimenqty') {
        return 'specimenqty("' +
          reporting.params.restrictBy + '", "' +
          (reporting.params.minQty || -1) + '", "' +
          (reporting.params.maxQty || - 1) + '"' +
        ')';
      }

      var rptFields = getReportFields(selectedFields, true);
      if (reporting.type == 'columnsummary') {
        return getColumnSummaryRptExpr(rptFields, reporting);
      }

      if (reporting.type != 'crosstab') {
        return reporting.type;
      }

      var rowIdx = getFieldIndices(rptFields, reporting.params.groupRowsBy);
      var colIdx = getFieldIndices(rptFields, [reporting.params.groupColBy]);
      colIdx = colIdx.length > 0 ? colIdx[0] : undefined;
      var summaryIdx = getFieldIndices(rptFields, reporting.params.summaryFields);
      var rollupExclIdx = getFieldIndices(rptFields, reporting.params.rollupExclFields);

      var includeSubTotals = "";
      if (reporting.params.includeSubTotals) {
        includeSubTotals = ", true";
      } 

      for (var i = 0; i < summaryIdx.length; ++i) {
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

    function getColumnSummaryRptExpr(rptFields, rpt) {
      var expr = 'columnsummary(';
      var addComma = false;
      if (rpt.params.sum && rpt.params.sum.length > 0) {
        expr += "\"sum\",";
        expr += "\"" + rpt.params.sum.length + "\",";
        var sumIdx = getFieldIndices(rptFields, rpt.params.sum);
        sumIdx = sumIdx.map(function(idx) { return "\"" + idx + "\""; });
        expr += sumIdx.join(",");
        addComma = true;
      }

      if (rpt.params.avg && rpt.params.avg.length > 0) {
        if (addComma) {
          expr += ", ";
        }
        expr += "\"avg\",";
        expr += "\"" + rpt.params.avg.length + "\",";
        var avgIdx = getFieldIndices(rptFields, rpt.params.avg);
        avgIdx = avgIdx.map(function(idx) { return "\"" + idx + "\""; });
        expr += avgIdx.join(",");
      }

      expr += ')';
      return expr;
    }

    function getReportFields(selectedFields, fresh) {
      var reportFields = [];

      angular.forEach(selectedFields, function(field) {
        var isAgg = false;
        angular.forEach(field.aggFns, function(aggFn) {
          if (fresh || aggFn.opted) {
            reportFields.push({
              id: field.name + '$' + aggFn.name,
              name: field.name,
              value: aggFn.desc,
              aggFn: aggFn.name
            });
            isAgg = true;
          }
        });

        if (!isAgg) {
          reportFields.push({
            id: field.name, 
            name: field.name, 
            value: field.form + ": " + field.label
          });
        }
      });

      return reportFields;
    };

    var getFieldIndices = function(fields, reportFields) {
      var idx = [];
      if (!reportFields) {
        return idx;
      }

      for (var i = 0; i < reportFields.length; ++i) {
        var rptField = reportFields[i];
        for (var j = 0; j < fields.length; ++j) {
          var selField = fields[j];
          selField = (typeof selField == "string") ? selField : selField.id;

          if (selField == rptField.id) {
            idx.push(j + 1);
            break;
          }
        }
      }

      return idx;
    };

    function getCountAql(filtersMap, exprNodes) {
      return "" +
        "select " +
        "  count(distinct Participant.id) as \"cprCnt\", " +
        "  count(distinct SpecimenCollectionGroup.id) as \"visitCnt\", " +
        "  count(distinct Specimen.id) as \"specimenCnt\" " +
        "where " +
           getWhereExpr(filtersMap, exprNodes);
    }

    function getDataAql(selectedFields, filtersMap, exprNodes, havingClause, reporting, addLimit, addPropIds) {
      addPropIds = !!addPropIds && (!reporting || reporting.type != 'crosstab');

      var selectList = getSelectList(selectedFields, filtersMap, addPropIds);
      var where = getWhereExpr(filtersMap, exprNodes);
      var rptExpr = getRptExpr(selectedFields, reporting);
      return "select " + selectList + 
             " where " + where +
             getHavingClause(havingClause) +
             (addLimit ? " limit 0, 10000 " : " ")  + rptExpr;
    }

    function getHavingClause(havingClause) {
      if (!havingClause) {
        return "";
      }

      havingClause = havingClause.replace(/count\s*\(/g, "count(distinct ");
      havingClause = havingClause.replace(/c_count\s*\(/g, "c_count(distinct ")
      return " having " + havingClause;
    }

    function getDefSelectedFields() {
      return [
        "Participant.id", "Participant.firstName", "Participant.lastName",
        "Participant.regDate", "Participant.ppid", "Participant.activityStatus"
      ];
    }

    function getForm(selectedCp, formName) {
      var form = undefined;
      var forms = selectedCp.forms;
      for (var i = 0; i < forms.length; ++i) {
        if (formName == forms[i].name) {
          form = forms[i];
          break;
        }
      }

      return form;
    }

    function getDateFns() {
      return [
        {label: 'current_date',    value: 'current_date()'},
        {label: 'months_between',  value: 'months_between('},
        {label: 'years_between',   value: 'years_between('},
        {label: 'minutes_between', value: 'minutes_between('},
        {label: 'round',           value: 'round('},
        {label: 'date_range',      value: 'date_range('}
      ];
    };

    function getFormsAndFnAdvise(selectedCp) {
      var forms = selectedCp.forms.map(
        function(form) {
          return {label: form.caption, value: form.name};
        }
      );
      return getDateFns().concat(forms);
    };

    function getFieldsAdvise(form) {
      var result = [];
      var fields = [].concat(form.staticFields).concat(form.extnFields);
      angular.forEach(fields, function(field) {
        if (field.type == 'DATE' || field.type == 'INTEGER' || field.type == 'FLOAT') {
          var label = field.caption;
          if (field.extensionForm) {
            label = field.extensionForm + ": " + label;
          }

          result.push({label: label, value: field.name});
        }
      });

      return result;
    };

    function getOpIdx(term) {
      var re = /[\+\-\(,<=>!]/g;
      var index = -1, numMatches = 0;
      var match;
      while ((match = re.exec(term)) != null) {
        index = match.index;
        re.lastIndex = ++numMatches;
      }
      return index;
    }

    function getUiFilter(queryGlobal, selectedCp, filterDef) {
      if (filterDef.expr) {
        return filterDef;
      }

      var fieldName = filterDef.field;
      var dotIdx = fieldName.indexOf(".");
      var formName = fieldName.substr(0, dotIdx);

      var op = getOpByModel(filterDef.op);
      var value = undefined;
      if (op.name == 'exists' || op.name == 'not_exists' || op.name == 'any') {
        value = undefined;
      } else if (op.name != 'qin' && op.name != 'not_in' && op.name != 'between') {
        value = filterDef.values[0];
      } else {
        value = filterDef.values;
      }

      var uiFilter =  {
        id: filterDef.id,
        op: getOpByModel(filterDef.op),
        value: value,
        form: getForm(selectedCp, formName),
        fieldName: fieldName.substr(dotIdx + 1),
        parameterized: filterDef.parameterized,
        hideOptions: filterDef.hideOptions,
        desc: filterDef.desc
      };

      if (filterDef.subQueryId) {
        uiFilter.sqCtxQ = SavedQuery.getById(filterDef.subQueryId).then(
          function(query) {
            uiFilter.hasSq    = true;
            uiFilter.subQuery = query;
            return queryGlobal.setupFilters(selectedCp, query).then(
              function(context) {
                return (query.context = context);
              }
            );
          }
        );
      }

      return uiFilter;
    };

    function getUiExprNode(expr) {
      var result = undefined;
      if (expr.nodeType == 'FILTER') {
        result = {type: 'filter', value: expr.value};
      } else if (expr.nodeType == 'OPERATOR') {
        result = {type: 'op', value: getOpByModel(expr.value).name};
      } else if (expr.nodeType == 'PARENTHESIS') {
        result = {type: 'paren', value: expr.value == 'LEFT' ? '(' : ')'};
      }

      return result;
    }

    function getUiExprNodes(nodes) {
      return nodes.map(getUiExprNode);
    }

    function disableCpSelection(queryCtx) {
      var filters = queryCtx.filters;
      for (var i = 0; i < filters.length; ++i) {
        var filter = filters[i];

        if (filter.expr && (filter.expr.indexOf('.extensions.') != -1 || filter.expr.indexOf('.customFields.') != -1)) {
          queryCtx.disableCpSelection = true;
          return;
        }

        if (!filter.expr && (filter.field.name.indexOf('extensions.') == 0 || filter.field.name.indexOf('customFields.') == 0)) {
          queryCtx.disableCpSelection = true;
          return;
        }

        if (filter.hasSq) {
          queryCtx.disableCpSelection = true;
          return;
        }
      }

      var selectedFields = queryCtx.selectedFields || [];
      for (var i = 0; i < selectedFields.length; ++i) {
        var fieldName = undefined;
        if (typeof selectedFields[i] == "string") {
          fieldName = selectedFields[i];
        } else if (typeof selectedFields[i] == "object") {
          fieldName = selectedFields[i].name;
        } 

        if (fieldName.split(".")[1] == 'extensions' || fieldName.split(".")[1] == "customFields") {
          queryCtx.disableCpSelection = true;
          return;
        }
      }

      queryCtx.disableCpSelection = false;
    }
    
    function getStringifiedValue(value) {
      return value.map(
        function(el) {
          if (el.indexOf('"') != -1) {
            return "'" + el + "'";
          } else if (el.indexOf("'") != -1) {
            return '"' + el + '"';
          } else {
            return el;
          }
        }
      ).join(", ");
    }

    function sortDate(d1, d2) {
      if (!!d1 && !!d2) {
        return new Date(d1) - new Date(d2);
      } else if (!!d1) {
        return 1;
      } else if (!!d2) {
        return -1;
      } else {
        return 0;
      }
    }

    function sortNumber(n1, n2) {
      n1 = (n1 == undefined) ? null : n1;
      n2 = (n2 == undefined) ? null : n2;

      if (n1 == n2) {
        return 0;
      } else if (n1 == null) {
        return -1;
      } else if (n2 == null) {
        return 1;
      }

      n1 = +n1;
      var badN1 = isNaN(n1);

      n2 = +n2;
      var badN2 = isNaN(n2);

      if (badN1 && badN2) {
        return 0;
      } else if (badN1) {
        return 1;
      } else if (badN2) {
        return -1;
      }

      return n1 - n2;
    }

    function sortAlpha(s1, s2) {
      s1 = (s1 == undefined) ? null : s1;
      s2 = (s2 == undefined) ? null : s2;

      if (s1 == s2) {
        return 0;
      } else if (s1 == null) {
        return -1;
      } else if (s2 == null) {
        return 1;
      } else {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        return (s1 === s2) ? 0 : (s1 < s2 ? -1 : 1);
      }
    }

    function sortBool(b1, b2) {
      if ((b1 && b2) || (!b1 && !b2)) {
        return 0;
      }

      return !b1 ? -1 : 1;
    }

    function saveQuery(queryContext) {
      $modal.open({
        templateUrl: 'modules/query/save.html',
        controller: 'QuerySaveCtrl',
        resolve: {
          queryToSave: function() {
            return SavedQuery.fromQueryCtx(queryContext);
          },

          dependentQueries: function() {
            return queryContext.dependentQueries || [];
          }
        }
      }).result.then(
        function(savedQuery) {
          angular.extend(queryContext, {id: savedQuery.id, title: savedQuery.title});

          Alerts.success('queries.query_saved', {title: savedQuery.title});
          var params = {queryId: savedQuery.id, cpId: savedQuery.cpId || -1, editMode: $stateParams.editMode};
          $state.go($state.current.name, params, {reload: true});
        }
      );
    }

    return {
      initOpsDesc:         initOpsDesc,

      getAllowedOps:       getAllowedOps,
 
      getValueType:        getValueType,

      isUnaryOp:           isUnaryOp,
 
      onOpSelect:          onOpSelect,

      hidePopovers:        hidePopovers,

      getOp:               getOp,

      isValidQueryExpr:    isValidQueryExpr,

      getCountAql:         getCountAql,

      getDataAql:          getDataAql,

      getCriteriaAql:      getWhereExpr,

      getDefSelectedFields: getDefSelectedFields,

      getForm:             getForm,

      getFormsAndFnAdvise: getFormsAndFnAdvise,

      getFieldsAdvise:     getFieldsAdvise,

      getOpIdx:            getOpIdx,

      getOpByModel:        getOpByModel,

      getOpBySymbol:       getOpBySymbol,

      getUiFilter:         getUiFilter,

      getUiExprNode:       getUiExprNode,

      getUiExprNodes:      getUiExprNodes,

      getTemporalExprObj:  getTemporalExprObj,

      disableCpSelection:  disableCpSelection,

      getStringifiedValue: getStringifiedValue,

      sortDate:            sortDate,

      sortNumber:          sortNumber,

      sortAlpha:           sortAlpha,

      sortBool:            sortBool,

      saveQuery:           saveQuery
    };
  });
