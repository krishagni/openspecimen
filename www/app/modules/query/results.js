
angular.module('os.query.results', ['os.query.models'])
  .filter('osFacetRange', function() {
    return function(input, facet) {
      var in0 = input[0] != undefined && input[0] != null && input[0] != '';
      var in1 = input[1] != undefined && input[1] != null && input[1] != '';

      if (in0 && in1) {
        return facet.relOp == 'eq' ? '= ' + input[0] : input[0] + " - " + input[1];
      } else if (in0) {
        return (facet.relOp == 'gt' ? '> ' : '>= ') + input[0];
      } else if (in1) {
        return (facet.relOp == 'lt' ? '< ' : '<= ') + input[1];
      } else {
        return facet.relOp == 'between' ? 'Any' : 'Err';
      }
    }
  })
  .filter('osQueryDate', function($filter) {
    return function(input) {
      if (typeof input != 'string') {
        return input;
      }

      if (input.indexOf('T') != -1) {
        return $filter('date')(input, ui.os.global.dateTimeFmt);
      } else {
        return $filter('date')(input, ui.os.global.dateFmt);
      }
    };
  })
  .controller('QueryResultsCtrl', function(
    $scope, $state, $stateParams, $modal, $document, $timeout, $interpolate, currentUser,
    queryCtx, cps, QueryCtxHolder, QueryUtil, QueryExecutor, SpecimenList, SpecimensHolder, Util, Alerts) {

    var STR_FACETED_OPS = ['eq', 'qin', 'exists', 'any'];

    var RANGE_FACETED_OPS = ['lt', 'le', 'gt', 'ge', 'eq', 'between', 'exists', 'any'];

    var currResults = {};

    var criteria = undefined;

    function isNumber(val) {
      return !isNaN(val) && angular.isNumber(val);
    }

    function identity(val) {
      return val;
    }

    function isNotBlankStr(val) {
      return angular.isString(val) && val.trim().length > 0;
    }

    var RANGE_FNS = {
      'INTEGER': {
        parse   : parseInt,
        isValid : isNumber
      },

      'FLOAT': {
        parse   : parseFloat,
        isValid : isNumber
      },

      'DATE': {
        parse   : identity,
        isValid : isNotBlankStr
      }
    };

    function init() {
      criteria = QueryUtil.getCriteriaAql(queryCtx.filtersMap, queryCtx.exprNodes);
      $scope.queryCtx = queryCtx;
      $scope.cps = cps;
      $scope.selectedRows = [];

      $scope.resultsCtx = {
        waitingForRecords: true,
        error: false,
        moreData: false,
        columnDefs: [],
        rows: [],
        numRows: 0,
        labelIndices: [],
        gridOpts: getGridOpts(),

        counters: {waiting: true, error: false},
      }

      $scope.$on('ngGridEventDigestGridParent',
        function() {
          /* Patented idea.
             This is to trigger a digest cycle when user changes the selection by
             using up and down arrow keys. */
          $timeout(function() { }, 0);
        }
      );
      executeQuery($stateParams.editMode);
    }

    function getGridOpts() {
      return {
        columnDefs        : 'resultsCtx.columnDefs',
        data              : 'resultsCtx.rows',
        enableColumnResize: true,
        showFooter        : false,
        totalServerItems  : 'resultsCtx.numRows',
        plugins           : [/*gridFilterPlugin*/],
        headerRowHeight   : 39,
        selectedItems     : $scope.selectedRows,
        enablePaging      : false,
        rowHeight         : 36,
        rowTemplate       :'<div ng-style="{\'cursor\': row.cursor, \'z-index\': col.zIndex() }" ' +
                              'ng-repeat="col in renderedColumns" ng-class="col.colIndex()" ' +
                              'class="ngCell {{col.cellClass}}" ng-cell> ' +
                           '</div>'
      };
    }

    function executeQuery(editMode) {
      //if (!editMode && isParameterized()) {
      //  showParameterizedFilters();
      //} else {
        loadRecords(true, true);
      //}
    }

    function isParameterized() {
      var filters = $scope.queryCtx.filters;
      for (var i = 0; i < filters.length; ++i) {
        if (filters[i].parameterized) {
          return true;
        }
      }

      return false;
    }

    function showParameterizedFilters() {
      var modal = $modal.open({
        templateUrl: 'modules/query/parameters.html',
        controller: 'ParameterizedFilterCtrl',
        resolve: {
          queryCtx: function() {
            return $scope.queryCtx;
          }
        },
        size: 'lg'
      });

      modal.result.then(
        function(result) {
          if (result) {
            $scope.queryCtx = result;
          }

          loadRecords();
        }
      );
    };

    function loadRecords(initFacets, initCounts) {
      var qc = $scope.queryCtx;
      $scope.showAddToSpecimenList = showAddToSpecimenList();
      $scope.resultsCtx.waitingForRecords = true;
      $scope.resultsCtx.error = $scope.resultsCtx.moreData = false;

      currResults = {};

      var aql = getAql(true, true);
      var outputIsoFmt = (qc.reporting.type != 'crosstab');
      var opts = {outputColumnExprs: qc.outputColumnExprs, caseSensitive: qc.caseSensitive};
      QueryExecutor.getRecords(qc.id, qc.selectedCp, aql, qc.wideRowMode || 'DEEP', outputIsoFmt, opts).then(
        function(result) {
          currResults = result;
          $scope.resultsCtx.waitingForRecords = false;
          if (qc.reporting.type == 'crosstab') {
            preparePivotTable(result);
          } else {
            var showColSummary = qc.reporting.type == 'columnsummary';
            prepareDataGrid(showColSummary, result);
            $scope.resultsCtx.gridOpts.headerRowHeight = showColSummary ? 66 : 39;
          }
        },

        function(error) {
          $scope.resultsCtx.waitingForRecords = false;
          $scope.resultsCtx.error = true;
          $scope.resultsCtx.errorData = error.data;
        }
      );

      if (initFacets) {
        loadFacets();
      }

      if (initCounts) {
        loadCounters();
      }
    }

    function loadFacets() {
      var facets = getFacets($scope.queryCtx.filters);
      $scope.resultsCtx.facets = facets;
      $scope.resultsCtx.hasFacets = (facets.length > 0);
    }


    function getFacets(filters) {
      var facets = [];

      angular.forEach(filters,
        function(filter, index) {
          if (!filter.parameterized && !filter.subQuery) {
            return;
          }

          if (filter.subQuery) {
            //
            // if the sub-query has parameterised filters then all the facet
            // values are loaded instead of loading only those that satisfy
            // the query criteria
            //
            var sqFacets = getFacets(filter.subQuery.context.filters);
            if (sqFacets.length > 0) {
              criteria = undefined;
            }

            angular.forEach(sqFacets,
              function(sqFacet) {
                sqFacet.queryCtx = sqFacet.queryCtx || filter.subQuery.context;
                facets.push(sqFacet);
              }
            );
            return;
          }

          var type = undefined;
          if (!!filter.expr) {
            var tObj = filter.tObj = QueryUtil.getTemporalExprObj(filter.expr);
            filter.op = QueryUtil.getOpBySymbol(tObj.op);
            var rhs = tObj.rhs && tObj.rhs.trim();
            if (rhs) {
              if (tObj.op == 'between') {
                filter.value = JSON.parse('[' + rhs.substring(1, rhs.length - 1) + ']');
              } else {
                filter.value = tObj.rhs;
              }
            }

            type = 'INTEGER';
          } else {
            type = filter.field.type;
          }

          if (!filter.op) {
            return;
          }

          switch (type) {
            case 'STRING':
              if (STR_FACETED_OPS.indexOf(filter.op.name) == -1) {
                return;
              }
              break;

            case 'INTEGER':
            case 'FLOAT':
            case 'DATE':
              if (RANGE_FACETED_OPS.indexOf(filter.op.name) == -1) {
                return;
              }
              break;

            default:
              return;
          }

          facets.push(getFacet(filter, index));
        }
      );

      return facets;
    }

    function getFacet(filter, index) {
      var values = undefined;
      var type = !!filter.expr ? 'INTEGER' : filter.field.type;
      var isRangeType = !!RANGE_FNS[type];

      if (isRangeType) {
        var value = undefined;
        if (filter.value) {
          switch (filter.op.name) {
            case 'eq': value = [filter.value, filter.value]; break;
            case 'lt': value = [undefined, filter.value]; break;
            case 'le': value = [undefined, filter.value]; break;
            case 'gt': value = [filter.value, undefined]; break;
            case 'ge': value = [filter.value, undefined]; break;
            case 'between': value = filter.value; break;
          }
        }

        if (value) {
          values = [{value: value, selected: true}];
        }
      } else if (filter.field.type == 'STRING') {
        if (typeof filter.value == "string" && filter.value.length > 0) {
          values = [{value: filter.value, selected: false}];
        } else if (filter.value instanceof Array) {
          values = filter.value.map(
            function(val) {
              return {value: val, selected: false};
            }
          );
        }
      }

      return {
        id: filter.id,
        caption: !!filter.expr || !!filter.desc ? filter.desc : filter.field.caption,
        dataType: type,
        isRange: isRangeType,
        relOp: isRangeType && filter.op && filter.op.name,
        expr: !!filter.expr ? filter.expr : (filter.form.name + "." + filter.field.name),
        type: type,
        values: values,
        valuesQ: undefined,
        selectedValues: [],
        subset: !filter.hideOptions && !!values,
        isOpen: false,
        hideOptions: filter.hideOptions
      };
    }

    function loadFacetValues(facet, searchTerm) {
      if (facet.dataType != 'STRING' || facet.subset || facet.hideOptions) {
        return;
      }

      var q = undefined;
      if (!!searchTerm) {
        q = QueryExecutor.getFacetValues($scope.queryCtx.selectedCp, [facet.expr], searchTerm, criteria);
      }

      if (!q) {
        if (facet.valuesQ) {
          q = facet.valuesQ;
        } else {
          q = facet.valuesQ = QueryExecutor.getFacetValues($scope.queryCtx.selectedCp, [facet.expr], undefined, criteria);
        }
      }

      q.then(
        function(result) {
          facet.values = result[0].values.map(
            function(value) {
              return {value: value, selected: false}
            }
          );
          facet.dbRows = result[0].dbRows;
          facet.searchValues = !!searchTerm;

          var selectedValues = facet.selectedValues;
          if (!selectedValues || selectedValues.length == 0) {
            return;
          }

          angular.forEach(facet.values, function(val) {
            val.selected = (selectedValues.indexOf(val.value) != -1);
          });
        }
      );
    }

    function loadCounters() {
      var qc = $scope.queryCtx;
      var aql = QueryUtil.getCountAql(qc.filtersMap, qc.exprNodes);

      var counters = $scope.resultsCtx.counters;
      counters.waiting = true;
      counters.error = false;
      QueryExecutor.getCount(qc.id, qc.selectedCp, aql, qc.caseSensitive).then(
        function(result) {
          counters.waiting = false;
          angular.extend(counters, result);
        },

        function(result) {
          counters.waiting = false;
          counters.error = true;
        }
      );
    }


    function getAql(addLimit, addPropIds) {
      var qc = $scope.queryCtx;
      return QueryUtil.getDataAql(
        qc.selectedFields, 
        qc.filtersMap, 
        qc.exprNodes, 
        qc.havingClause,
        qc.reporting,
        addLimit,
        addPropIds);
    }

    function removeSeparator(label)  {
      var idx = label.lastIndexOf("# ");
      if (idx != -1) {
        label = label.substr(idx + 2);
      }

      return label;
    }

    function getColumnWidth(text) {
      var span = angular.element('<span/>')
        .addClass('ngHeaderText')
        .css('visibility', 'hidden')
        .text(text);

      angular.element($document[0].body).append(span);
      var width = span[0].offsetWidth + 2 + 8 * 2; // 8 + 8 is padding, 2 is buffer/uncertainity
      span.remove();
      return width;
    }

    function preparePivotTable(result) {
      $scope.resultsCtx.rows = result.rows;
      $scope.resultsCtx.columnLabels = result.columnLabels;
      $scope.resultsCtx.moreData = (result.dbRowsCount >= 10000);

      var numGrpCols = $scope.queryCtx.reporting.params.groupRowsBy.length;
      for (var i = 0; i < numGrpCols; ++i) {
        result.columnLabels[i] = removeSeparator(result.columnLabels[i]);
      }

      var numValueCols = $scope.queryCtx.reporting.params.summaryFields.length;
      var numRollupCols = numValueCols;
      var rollupExclFields = $scope.queryCtx.reporting.params.rollupExclFields;
      if (rollupExclFields && rollupExclFields.length > 0) {
        numRollupCols = numRollupCols - rollupExclFields.length;
      }

      $scope.resultsCtx.pivotTableOpts = {
        height: '100%',
        width: '100%',
        colHeaders: $scope.resultsCtx.columnLabels,
        numGrpCols: numGrpCols,
        numValueCols: numValueCols,
        numRollupCols: numRollupCols,
        data: $scope.resultsCtx.rows
      };
    };

    function columnInstance(label) {
      var hyphenIdx = label.lastIndexOf(' - ');
      if (hyphenIdx == -1) {
        return {label: label, instance: 0};
      }

      var idx = parseInt(label.substring(hyphenIdx + 2));
      return {label: label.substring(0, hyphenIdx), instance: isNaN(idx) ? 0 : idx};
    }

    function prepareDataGrid(showColSummary, result) {
      var idx = -1,
          summaryRow = [];

      if (showColSummary && !!result.rows && result.rows.length > 0) {
        summaryRow = result.rows.splice(result.rows.length - 1, 1)[0];
      }

      var colDefs = [];
      angular.forEach(result.columnLabels,
        function(columnLabel) {
          ++idx;

          if (columnLabel.charAt(0) == '$') {
            return;
          }

          var columnLabel = removeSeparator(columnLabel);
          var width = getColumnWidth(columnLabel);

          var isDateColumn = (result.columnTypes[idx] == 'DATE');
          var cellTemplate = null;
          if (result.columnUrls[idx]) {
            var link, linkTxt;
            if (result.columnUrls[idx] == 'true') {
              link = '{{row.entity[col.field]}}';
              linkTxt = 'Click here';
            } else {
              link = '{{cellUrl(row, col,' + idx + ')}}'
              linkTxt = '{{row.entity[col.field]}}';
            }

            cellTemplate = '<div class="ngCellText" ng-class="col.colIndex()" title="' + linkTxt + '">' +
                           '  <a href="' + link + '" target="_blank">' +
                                linkTxt +
                           '  </a>' +
                           '</div>';
          } else if (isDateColumn) {
            cellTemplate = '<div class="ngCellText" ng-class="col.colIndex()" title="{{row.entity[col.field]}}">' +
                           '  <span>{{row.entity[col.field] | osQueryDate}}</span>' +
                           '</div>';
          } else {
            cellTemplate = '<div class="ngCellText" ng-class="col.colIndex()" title="{{row.entity[col.field]}}">' +
                           '  <span>{{row.entity[col.field]}}</span>' +
                           '</div>';
          }

          colDefs.push({
            field:        "col" + idx,
            instance:     columnInstance(columnLabel).instance,
            displayName:  columnLabel,
            minWidth:     width < 100 ? 100 : width,
            headerCellTemplate: 'modules/query/column-filter.html',
            cellTemplate: !!cellTemplate ? cellTemplate : undefined,
            showSummary:  showColSummary,
            summary:      summaryRow[idx],
            sortFn:       getSortFn(result.columnTypes[idx]),
            cellFilter:   isDateColumn ? "osQueryDate" : undefined /* unused */
          });
        }
      );

      $scope.resultsCtx.columnDefs = colDefs;
      $scope.resultsCtx.labelIndices = result.columnIndices;
      $scope.resultsCtx.rows = getFormattedRows(result.columnLabels, result.rows);
      $scope.resultsCtx.numRows = result.rows.length;
      $scope.resultsCtx.moreData = (result.dbRowsCount >= 10000);
      $scope.selectedRows.length = 0;

      /** Hack to make grid resize **/
      window.setTimeout(function(){
        $(window).resize();
        $(window).resize();
      }, 500);
    }

    function getSortFn(type) {
      if (type == 'INTEGER' || type == 'FLOAT' || type == 'DATE_INTERVAL') {
        return QueryUtil.sortNumber;
      } else if (type == 'STRING') {
        return QueryUtil.sortAlpha;
      } else if (type == 'BOOLEAN') {
        return QueryUtil.sortBool;
      } else if (type == 'DATE') {
        return QueryUtil.sortDate;
      } else {
        return function(a, b) { return (a === b) ? 0 : (a < b ? -1 : 1); };
      }
    }

    function getFormattedRows(labels, rows) {
      var formattedRows = [];
      for (var i = 0; i < rows.length; ++i) {
        var formattedRow = {hidden:{}};
        for (var j = 0; j < rows[i].length && j < labels.length; ++j) {
          if (labels[j].charAt(0) == '$') {
            var colInstance = columnInstance(labels[j]);
            if (!formattedRow['hidden'][colInstance.instance]) {
              formattedRow['hidden'][colInstance.instance] = {};
            }
            formattedRow['hidden'][colInstance.instance][colInstance.label] = rows[i][j];
          } else {
            formattedRow["col" + j] = rows[i][j];
          }
        }
        formattedRows.push(formattedRow);
      }
      return formattedRows;
    }

    function showAddToSpecimenList() {
      if ($scope.queryCtx.reporting.type == 'crosstab') {
        return false;
      }

      var result = false, fields = $scope.queryCtx.selectedFields;
      for (var i = 0; i < fields.length; ++i) {
        var field = fields[i];

        if (typeof field == 'string') {
          result = (field == 'Specimen.label');
        } else if (typeof field == 'object' && (!field.aggFns || field.aggFns.lengths == 0)) {
          result = (field.name == 'Specimen.label');
        }

        if (result) {
          break;
        }
      }

      return result;
    }

    function getSelectedSpecimens() {
      return $scope.selectedRows.map(function(row) {
        return {id: row.hidden[0].$specimenId, cpId: row.hidden[0].$cpId};
      });
    };

    var gridFilterPlugin = {
      init: function(scope, grid) {
        gridFilterPlugin.scope = scope;
        gridFilterPlugin.grid = grid;
        $scope.$watch(
          function() {
            var searchQuery = "";
            angular.forEach(
              gridFilterPlugin.scope.columns, 
              function(col) {
                if (col.visible && col.filterText) {
                  var filterText = '';
                  if (col.filterText.indexOf('*') == 0 ) {
                    filterText = col.filterText.replace('*', '');
                  } else {
                    filterText = col.filterText;
                  }
                  filterText += ";";
                  searchQuery += col.displayName + ": " + filterText;
                }
              }
            );
            return searchQuery;
          },

          function(searchQuery) {
            gridFilterPlugin.scope.$parent.filterText = searchQuery;
            gridFilterPlugin.grid.searchProvider.evalFilter();
          }
        );
      },
      scope: undefined,
      grid: undefined
    };

    $scope.cellUrl = function(row, col, colIdx) {
      var hidden = row.entity.hidden[columnInstance(col.displayName).instance];
      var locals = angular.extend({$value: row.getProperty(col.field)}, hidden);
      return $interpolate(currResults.columnUrls[colIdx])(locals);
    }

    $scope.editFilters = function() {
      $state.go('query-addedit', {queryId: $scope.queryCtx.id});
    }

    $scope.showFacetSearch = function() {
      $scope.resultsCtx.showFacetSearch = true;
    }

    $scope.hideFacetSearch = function() {
      $scope.resultsCtx.showFacetSearch = false;
      $scope.resultsCtx.facetSearchTerm = undefined;
    }


    $scope.defineView = function() {
      var prevCaseSensitive = $scope.queryCtx.caseSensitive;
      var mi = $modal.open({
        templateUrl: 'modules/query/define-view.html',
        controller: 'DefineViewCtrl',
        resolve: {
          queryCtx: function() {
            return $scope.queryCtx;
          }
        },
        size: 'lg'
      });

      mi.result.then(
        function(queryCtx) {
          $scope.queryCtx = queryCtx;
          QueryUtil.disableCpSelection(queryCtx);
          loadRecords(false, (prevCaseSensitive != queryCtx.caseSensitive));
        }
      );
    }

    $scope.rerun = function() {
      executeQuery(false);
    }

    $scope.saveCtx = function(navTo) {
      queryCtx.fromState = navTo;
      QueryCtxHolder.setCtx(queryCtx);
    }

    $scope.downloadResults = function() {
      var qc = $scope.queryCtx;

      var alert = Alerts.info('queries.export_initiated', {}, false);  
      var aql = getAql(false);
      var opts = {outputColumnExprs: qc.outputColumnExprs, caseSensitive: qc.caseSensitive};
      QueryExecutor.exportQueryResultsData(qc.id, qc.selectedCp, aql, qc.wideRowMode || 'DEEP', opts).then(
        function(result) {
          Alerts.remove(alert);
          if (result.completed) {
            Alerts.info('queries.downloading_data_file');
            QueryExecutor.downloadDataFile(result.dataFile);
          } else if (result.dataFile) {
            Alerts.info('queries.data_file_will_be_emailed');
          }
        },

        function() {
          Alerts.remove(alert);
        }
      );
    };

    $scope.selectAllRows = function() {
      $scope.resultsCtx.gridOpts.selectAll(true);
      $scope.resultsCtx.selectAll = true;
    };

    $scope.unSelectAllRows = function() {
      $scope.resultsCtx.gridOpts.selectAll(false);
      $scope.resultsCtx.selectAll = false;
    };

    $scope.getSelectedSpecimens = getSelectedSpecimens;

    $scope.addSelectedSpecimensToSpecimenList = function(list) {
      var selectedSpecimens = getSelectedSpecimens();
      if (!selectedSpecimens || selectedSpecimens.length == 0) {
        Alerts.error('specimens.no_specimens_for_specimen_list');
        return;
      }

      if (!list) {
        $scope.createNewSpecimenList();
      } else {
        list.addSpecimens(selectedSpecimens).then(
          function(specimens) {
            var type = list.getListType(currentUser);
            Alerts.success('specimen_list.specimens_added_to_' + type, list);
          }
        );
      }
    }

    $scope.createNewSpecimenList = function() {
      queryCtx.fromState = 'specimen-list-addedit'
      QueryCtxHolder.setCtx(queryCtx);
      SpecimensHolder.setSpecimens(getSelectedSpecimens());
      $state.go('specimen-list-addedit', {listId: ''});
    };

    $scope.toggleFacetValues = function(facet) {
      $timeout(
        function() {
          if (!facet.isOpen) {
            return;
          }

          loadFacetValues(facet);
        }
      );
    }

    $scope.toggleFacetValueSelection = function(facet, toggledValue) {
      var ctx = facet.queryCtx || $scope.queryCtx;
      angular.forEach(ctx.filters, function(filter) {
        if (filter.id != facet.id) {
          return;
        }

        var rangeFns = RANGE_FNS[facet.dataType];
        if (!!rangeFns) {
          var minMax = [undefined, undefined];
          if (facet.values[0].selected) {
            minMax = facet.values[0].value;
          }

          var validMin = rangeFns.isValid(minMax[0]);
          var validMax = rangeFns.isValid(minMax[1]);

          if (validMin && validMax) {
            filter.value = minMax;
            filter.op = QueryUtil.getOp('between');
          } else if (validMin && !validMax) {
            filter.value = minMax[0];
            filter.op = QueryUtil.getOp(facet.relOp == 'gt' ? 'gt' : 'ge');
          } else if (!validMin && validMax) {
            filter.value = minMax[1];
            filter.op = QueryUtil.getOp(facet.relOp == 'lt' ? 'lt' : 'le');
          } else {
            filter.value = undefined;
            filter.op = QueryUtil.getOp(facet.relOp || 'any');
          }

          if (!filter.tObj) {
            return;
          }

          // temporal expression
          var tObj = filter.tObj;
          filter.expr = tObj.lhs + ' ' + filter.op.symbol + ' ';
          if (filter.value instanceof Array) {
            filter.expr += "(" + filter.value.join() + ")";
          } else if (angular.isDefined(filter.value)) {
            filter.expr += filter.value;
          }

          facet.selectedValues = filter.value;
        } else {
          filter.op = QueryUtil.getOp('qin');

          if (!toggledValue) {
            filter.value = [];
          } else {
            filter.value = facet.selectedValues;

            var valueIdx = filter.value.indexOf(toggledValue.value);
            if (toggledValue.selected && valueIdx == -1) {
              filter.value.push(toggledValue.value);
            } else if (!toggledValue.selected && valueIdx != -1) {
              filter.value.splice(valueIdx, 1);
            }
          }

          facet.selectedValues = filter.value;
          if (facet.selectedValues.length == 0) {
            if (facet.subset) {
              filter.op = QueryUtil.getOp('qin');
              filter.value = facet.values.map(function(val) { return val.value; });
            } else {
              filter.op = QueryUtil.getOp('any');
              filter.value = undefined;
            }
          }
        }
      });

      if (ctx.searchQ) {
        $timeout.cancel(ctx.searchQ);
      }

      ctx.searchQ = $timeout(
        function() {
          loadRecords(false, true);
        },
        ui.os.global.filterWaitInterval
      );
    }

    $scope.clearFacetValueSelection = function($event, facet) {
      if ($event) {
        $event.stopPropagation();
      }

      if (facet.hideOptions) {
        facet.values = [];
      } else {
        angular.forEach(facet.values, function(value) {
          value.selected = false;
        });
      }

      $scope.toggleFacetValueSelection(facet);
      facet.isOpen = false;
    }

    $scope.searchFacetValue = function(facet) {
      var valuesCount = (facet.dbRows != null && facet.dbRows != undefined) ? facet.dbRows : facet.values.length;
      if (valuesCount < 500 && !facet.searchValues) {
        return;
      }

      if (facet.searchQ) {
        $timeout.cancel(facet.searchQ);
      }

      facet.searchQ = $timeout(
        function() {
          loadFacetValues(facet, facet.searchFor);
        },
        ui.os.global.filterWaitInterval
      );
    }


    $scope.addRangeCond = function(facet) {
      var fns = RANGE_FNS[facet.dataType];

      var min = fns.parse(facet.min);
      if (!fns.isValid(min)) {
        min = undefined;
      }

      var max = fns.parse(facet.max);
      if (!fns.isValid(max)) {
        max = undefined;
      }

      facet.min = facet.max = '';
      if (min == undefined && max == undefined) {
        return;
      }

      facet.values = [{value: [min, max], selected: true}];
      $scope.toggleFacetValueSelection(facet);
    }

    $scope.addCond = function(facet) {
      var values = Util.splitStr(facet.searchFor, /,|\t|\n/, false);
      if (values.length == 0) {
        return;
      }

      facet.selectedValues = values;
      facet.values = values.map(function(value) { return {value: value, selected: true} });
      $scope.toggleFacetValueSelection(facet, {});
      facet.searchFor = undefined;
    }

    $scope.saveQuery = function() {
      QueryUtil.saveQuery($scope.queryCtx);
    }

    init();
  });
