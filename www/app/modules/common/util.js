
angular.module('openspecimen')
  .factory('Util', function(
    $rootScope, $filter, $state, $stateParams, $timeout, $document, $q,
    $parse, $modal, $translate, $http, osRightDrawerSvc, ApiUrls, Alerts) {

    var isoDateRe = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*))(?:Z|(\+|-)([\d|:]*))?$/;

    var fileTypes = [
      'bmp', 'csv', 'css', 'doc', 'docx', 'gif', 'html', 'jar', 'java', 'jpeg', 'jpg',
      'js', 'json', 'pdf', 'png', 'tif', 'tiff', 'txt', 'xls', 'xlsx', 'xml', 'zip'
    ];

    var httpsRe = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;

    var wwwRe   = /(^|[^\/])(www\.[\S]+(\b|$))/gim;

    var mailRe  = /(([a-zA-Z0-9\-\_\.])+@[a-zA-Z\_]+?(\.[a-zA-Z]+)+)/gim;

    function clear(input) {
      input.splice(0, input.length);
    };

    function unshiftAll(arr, elements) {
      Array.prototype.splice.apply(arr, [0, 0].concat(elements));
    };

    function assign(arr, elements) {
      clear(arr);
      unshiftAll(arr, elements);
    };

    function filterOpts(opts, filters) {
      if (!filters) {
        filters = $stateParams.filters;
      }

      if (!!filters) {
        try {
          angular.extend(opts, angular.fromJson(decodeURIComponent(atob(filters))));
          osRightDrawerSvc.open();
        } catch (e) {
          console.log("Invalid filter");
        }
      }

      return opts;
    }

    function filter($scope, varName, callback, excludeParams) {
      $scope.$watch(varName, function(newVal, oldVal) {
        if (newVal == oldVal) {
          return;
        }

        if ($scope._filterQ) {
          $timeout.cancel($scope._filterQ);
        }

        $scope._filterQ = $timeout(
          function() {
            var filters = angular.copy(newVal);

            excludeParams = excludeParams || ['includeStats', 'maxResults', 'max'];
            angular.forEach(excludeParams, function(param) { delete filters[param]; });

            angular.forEach(filters,
              function(value, key) {
                if (!value) { delete filters[key]; }
              }
            );

            var fb = undefined;
            if (Object.keys(filters).length > 0) {
              fb = btoa(encodeURIComponent(JSON.stringify(filters)));
            }

            $stateParams.filters = fb;
            $state.go($state.current.name, $stateParams, {notify: false});
            callback(newVal);
          },
          $rootScope.global.filterWaitInterval
        );
      }, true);
    }

    function getEscapeMap(str) {
      var map = {}, insideSgl = false, insideDbl = false;
      var lastIdx = -1;

      for (var i = 0; i < str.length; ++i) {
        if (str[i] == "'" && !insideDbl) {
          if (insideSgl) {
            map[lastIdx] = i;
          } else {
            lastIdx = i;
          }

          insideSgl = !insideSgl;
        } else if (str[i] == '"' && !insideSgl) {
          if (insideDbl) {
            map[lastIdx] = i;
          } else {
            lastIdx = i;
          }

          insideDbl = !insideDbl;
        }
      }

      return map;
    }

    function getToken(token) {
      token = token.trim();
      if (token.length != 0) {
        if ((token[0] == "'" && token[token.length - 1] == "'") ||
            (token[0] == '"' && token[token.length - 1] == '"')) {
          token = token.substring(1, token.length - 1);
        }
      }

      return token;
    }

    function splitStr(str, re, returnEmpty) {
      var result = [], token = '', escUntil = undefined;
      var map = getEscapeMap(str);

      for (var i = 0; i < str.length; ++i) {
        if (escUntil == undefined) {
          escUntil = map[i];
        }

        if (i <= escUntil) {
          token += str[i];
          if (i == escUntil) {
            escUntil = undefined;
          }
        } else {
          if (re.exec(str[i]) == null) {
            token += str[i];
          } else {
            token = getToken(token);
            if (token.length > 0 || !!returnEmpty) {
              result.push(token);
            }
            token = '';
          }
        }
      }

      token = getToken(token);
      if (token.length > 0) {
        result.push(token);
      }

      return result;
    }

    function getDupObjects(objs, props) {
      var dupObjs = {};
      var scannedObjs = {};
      angular.forEach(props, function(prop) {
        dupObjs[prop] = [];
        scannedObjs[prop] = [];
      });

      angular.forEach(objs, function(obj) {
        angular.forEach(props, function(prop) {
          if (!obj[prop]) {
            return;
          }

          if (scannedObjs[prop].indexOf(obj[prop]) >= 0) {
            if (dupObjs[prop].indexOf(obj[prop]) == -1) {
              dupObjs[prop].push(obj[prop]);
            }
          }

          scannedObjs[prop].push(obj[prop]);
        })
      });
 
      return dupObjs;
    }

    function hidePopovers() {
      var popovers = $document.find('div.popover');
      angular.forEach(popovers, function(popover) {
        angular.element(popover).scope().$hide();
      });
    }

    function getNumberInScientificNotation(input, minRange, fractionDigits) {
      if (input == null || input == undefined) {
        return input;
      }

      minRange = minRange || 1000000;
      fractionDigits = fractionDigits || undefined;
      
      input = +input;
      if (angular.isNumber(input) && !isNaN(input) && input >= minRange) {
        input = fractionDigits != undefined ? input.toExponential(fractionDigits) : input.toExponential();
      }

      return input;
    }

    function parseDate(value) {
      if (typeof value === 'string') {
        var matches = isoDateRe.exec(value);
        if (matches) {
          return new Date(value);
        }
      }

      return value;
    }

    function downloadReport(entity, msgClass, filename, params) {
      var alert = Alerts.info(msgClass + '.report_gen_initiated', {}, false);
      entity.generateReport(params).then(
        function(result) {
          Alerts.remove(alert);
          if (result.completed) {
            Alerts.info(msgClass + '.downloading_report');

            filename = (filename || entity.name);

            var extn = filename.substr(filename.lastIndexOf('.') + 1).toLowerCase();
            if (fileTypes.indexOf(extn) == -1) {
              // no known extension, by default, append .csv
              filename += '.csv';
            }

            downloadFile(ApiUrls.getBaseUrl() + 'query/export' +
              '?fileId=' + result.dataFile +
              '&filename=' + encodeURIComponent(filename)
            );
          } else if (result.dataFile) {
            Alerts.info(msgClass + '.report_will_be_emailed');
          }
        },

        function() {
          Alerts.remove(alert);
        }
      );
    }

    function downloadFile(fileUrl) {
      var clickEvent;
      if (typeof Event == 'function') {
        clickEvent = new MouseEvent('click', {view: window, bubbles: true, cancelable: false});
      } else {
        clickEvent = document.createEvent('Event');
        clickEvent.initEvent('click', true, false);
      }

      var link = angular.element('<a target="_blank"/>').attr({href: fileUrl});
      link[0].dispatchEvent(clickEvent);
    }

    function booleanPromise(condition) {
      var deferred = $q.defer();
      if (condition) {
        deferred.resolve(true);
      } else {
        deferred.reject(false);
      }
      
      return deferred.promise;
    }

    function appendAll(array, elements) {
      Array.prototype.push.apply(array, elements);
    }

    function merge(src, dst, deep) {
      var h = dst.$$hashKey;

      if (!angular.isObject(src) && !angular.isFunction(src)) {
        return dst;
      }

      var keys = Object.keys(src);
      for (var i = 0; i < keys.length; i++) {
        var key = keys[i];
        var value = src[key];

        if (deep && angular.isObject(value)) {
          if (angular.isDate(value)) {
            dst[key] = new Date(value.valueOf());
          } else {
            if (!angular.isObject(dst[key])) {
              dst[key] = angular.isArray(value) ? [] : {};
            }

            merge(value, dst[key], true);
          }
        } else {
          dst[key] = value;
        }
      }

      if (h) {
        dst.$$hashKey = h;
      } else {
        delete dst.$$hashKey;
      }

      return dst;
    }

    function copyAttrs(src, attrs, array) {
      angular.forEach(array,
        function(dst) {
          if (dst == src) {
            return;
          }

          angular.forEach(attrs,
            function(attr) {
              dst[attr] = src[attr];
            }
          );
        }
      );
    }

    function addIfAbsent(dstArray, srcArray, keyProp) {
      if (!keyProp) {
        return;
      }

      var key = $parse(keyProp);
      var map = {};
      angular.forEach(dstArray,
        function(obj) {
          map[key(obj)] = obj;
        }
      );

      var added = [];
      angular.forEach(srcArray,
        function(obj) {
          if (!map[key(obj)]) {
            dstArray.push(obj);
            map[key(obj)] = obj;
            added.push(obj);
          }
        }
      );

      return added;
    }

    function showConfirm(opts) {
      var mi = $modal.open({
        templateUrl: opts.templateUrl || 'modules/common/show-confirm.html',
        controller: function($scope, $modalInstance) {
          angular.extend($scope, opts);

          $scope.ok = function() {
            $modalInstance.close(true);
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });

      mi.result.then(
        function() {
          if (opts.ok) {
            opts.ok();
          }
        },
        function() {
          if (opts.cancel) {
            opts.cancel();
          }
        }
      );

      return mi.result;
    }

    function showErrorMsg(resp) {
      $modal.open({
        templateUrl: 'modules/common/show-error-messages.html',
        controller: function($scope, $modalInstance) {
          var ctx = $scope.errCtx = {};
          if (resp.status == 0) {
            ctx.fixedMsg = 'common.server_connect_error';
          } else if (resp.status == 401) {
            ctx.fixedMsg = 'common.auth_error';
          } else if (resp.status / 100 == 5) {
            if (resp.data instanceof Array) {
              ctx.apiMsg = resp.data.map(function(err) { return err.message + " (" + err.code + ")"; });
            } else {
              ctx.fixedMsg = 'common.server_error';
            }
          } else if (resp.status / 100 == 4) {
            if (resp.data instanceof Array) {
              ctx.apiMsg = resp.data.map(function(err) { return err.message + " (" + err.code + ")"; });
            } else {
              ctx.fixedMsg = 'common.ui_error';
            }
          } else {
            ctx.fixedMsg = resp.msg;
            ctx.input = resp.args;
          }

          $scope.done = function() {
            $modalInstance.close(true);
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }

          $scope.copyToClipboard = function() {
            var text;
            if (ctx.fixedMsg) {
              text = $translate.instant(ctx.fixedMsg, ctx.input);
            } else if (ctx.apiMsg) {
              text = ctx.apiMsg.join('\n');
            }

            if (copyToClipboard(text || 'Nothing to copy')) {
              Alerts.success('common.error_msg_copied');
            }
          }
        }
      });
    }

    function validateItems(items, itemLabels, labelProp) {
      var labelGetter = $parse(labelProp);

      // for checking presence of element, using map is faster than list
      var labelsMap = {};
      angular.forEach(items,
        function(item) {
          labelsMap[labelGetter(item)] = true;
        }
      );

      var found = [], notFound = [], extra = [];
      angular.forEach(itemLabels,
        function(label) {
          if (labelsMap[label]) {
            found.push(label);
            delete labelsMap[label];
          } else {
            notFound.push(label);
          }
        }
      );

      angular.forEach(labelsMap,
        function(value, label) {
          extra.push(label);
        }
      );

      return {found: found, notFound: notFound, extra: extra};
    }

    function showItemsValidationResult(msgKeys, data) {
      return $modal.open({
        templateUrl: 'modules/common/item-validation-result.html',
        controller: function($scope, $modalInstance) {
          $scope.ctx = {msgKeys: msgKeys, data: data};

          $scope.ok = function() {
            $modalInstance.close(true);
          }

          $scope.generateReport = function() {
            generateValidationReport(msgKeys, data);
          }
        }
      });
    }

    function generateValidationReport(msgKeys, data) {
      var msg = $translate.instant;
      $translate(msgKeys.label).then(
        function() {
          var report = '';
          report = msg(msgKeys.itemLabel) + ',' + msg(msgKeys.error) + '\n';

          var notFound = msg(msgKeys.notFoundError);
          angular.forEach(data.notFound,
            function(label) {
              report += label + ',' + notFound + '\n';
            }
          );

          var extra = msg(msgKeys.extraError);
          angular.forEach(data.extra,
            function(label) {
              report += label + ',' + extra + '\n';
            }
          );

          var success = copyToClipboard(report);
          if (success) {
            Alerts.info(msgKeys.reportCopied);
          }
        }
      );
    }

    function copyToClipboard(text) {
      var textarea = angular.element('<textarea/>').val(text).css('opacity', 0);
      angular.element(document.body).append(textarea);
      textarea.select();

      var success = false;
      try {
        document.execCommand('copy');
        success = true;
      } catch (e) {
        console.log("Error copying text to clipboard");
      }

      textarea.remove();
      return success;
    }

    function toUtc(dt, ignSecs) {
      var seconds = !!ignSecs ? 0 : dt.getSeconds();
      var ms      = !!ignSecs ? 0 : dt.getMilliseconds();

      return Date.UTC(
        dt.getFullYear(), dt.getMonth(), dt.getDate(),
        dt.getHours(), dt.getMinutes(), seconds, ms
      );
    }

    function dateDiffInMs(i1, i2, ignSecs) {
      return toUtc(new Date(i2), ignSecs) - toUtc(new Date(i1), ignSecs);
    }

    function dateDiffInYears(i1, i2) {
      var d1 = new Date(i1);
      var d2 = new Date(i2);
      var diff = d2.getFullYear() - d1.getFullYear();

      var m = d2.getMonth() - d1.getMonth();
      if (m < 0 || (m === 0 && d2.getDate() < d1.getDate())) {
        --diff;
      }

      return diff;
    }


    /**
     * List of in-built functions available in evaluation of expressions
     */
    var fns = {
      set: function(object, expr, value) {
        $parse(expr).assign(object, value);
        return object;
      },

      get: function(object, expr) {
        return $parse(expr)(object);
      },

      ifnull: function(cond, truth, falsy) {
        return (cond == null || cond == undefined) ? truth : falsy;
      },

      ifNull: function(cond, truth, falsy) {
        return (cond == null || cond == undefined) ? truth : falsy;
      },

      ifNotNull: function(cond, truth, falsy) {
        return (cond !== null && cond !== undefined) ? truth : falsy;
      },

      split: function(inputStr, regex, limit) {
        return (inputStr || '' ).split(regex, limit);
      },

      join: function(inputStrs, separator) {
        return (inputStrs || []).join(separator);
      },

      concatList: function(list, expr, separator) {
        var parsedExpr = $parse(expr);

        return (list || []).map(
          function(e) {
            return parsedExpr(e);
          }
        ).filter(
          function(e) {
            return !!e;
          }
        ).join(separator);
      },

      concat: function() {
        var result = '';
        for (var i = 0; i < arguments.length; ++i) {
          if (!arguments[i]) {
            continue;
          }

          if (i != 0) {
            result += ' ';
          }

          result += arguments[i];
        }

        return result;
      },

      minValue: function(coll, expr) {
        var parsedExpr = $parse(expr);

        var minValue = null, minIdx = -1;
        angular.forEach(coll,
          function(e, idx) {
            var value = parsedExpr(e);

            if (idx == 0) {
              minValue = value;
              minIdx = idx;
              return;
            }

            if (value != null && value != undefined && value < minValue) {
              minValue = value;
              minIdx = idx;
            }
          }
        );

        return minValue;
      },

      toDateStr: function(input, fmt) {
        if (input === null || input === undefined) {
          return undefined;
        }

        return $filter('date')(input, fmt || ui.os.global.dateFmt);
      },

      toDateTimeStr: function(input, fmt) {
        if (input === null || input === undefined) {
          return undefined;
        }

        return $filter('date')(input, fmt || ui.os.global.dateTimeFmt);
      },

      ageInYears: function(input, today) {
        return dateDiffInYears(input, (today && new Date(today)) || new Date());
      },

      now: function() {
        return new Date().getTime();
      },

      currentTime: function() {
        return new Date().getTime();
      },

      dateDiffInYears: function(d1, d2) {
        return dateDiffInYears(d1, d2);
      },

      dateDiffInDays: function(d1, d2) {
        return Math.floor(dateDiffInMs(d1, d2) / (24 * 60 * 60 * 1000));
      },

      dateDiffInHours: function(d1, d2) {
        return Math.floor(dateDiffInMs(d1, d2) / (60 * 60 * 1000));
      },

      dateDiffInMinutes: function(d1, d2, ignoreSeconds) {
        return Math.floor(dateDiffInMs(d1, d2, ignoreSeconds) / (60 * 1000));
      },

      dateDiffInSeconds: function(d1, d2) {
        return Math.floor(dateDiffInMs(d1, d2) /  1000);
      }
    }

    function evaluate(expr, inputCtx) {
      try {
        return $parse(expr)(angular.extend({fns: fns}, inputCtx));
      } catch (e) {
        Alerts.error('common.invalid_expr', {expr: expr});
        throw e;
      }
    }

    function formatDate(date, format) {
      return $filter('date')(date, format);
    }

    function linkify(text, linkText) {
      if (!text || typeof text != 'string') {
        return text;
      }

      var httpsText = linkText || '$1';
      var wwwText = linkText || '$2';
      return text.replace(httpsRe, '<a href="$1" target="_blank">' + httpsText + '</a>')
        .replace(wwwRe, '$1<a href="http://$2" target="_blank">' + wwwText + '</a>')
        .replace(mailRe, '<a href="mailto:$1">' + httpsText + '</a>');
    }

    return {
      clear: clear,

      unshiftAll: unshiftAll,

      assign: assign,

      filterOpts: filterOpts,

      filter: filter,

      splitStr: splitStr,

      getDupObjects: getDupObjects,

      hidePopovers: hidePopovers,

      getNumberInScientificNotation: getNumberInScientificNotation,

      parseDate: parseDate,

      downloadReport : downloadReport,

      downloadFile: downloadFile,

      booleanPromise: booleanPromise,

      appendAll: appendAll,

      merge: merge,

      copyAttrs: copyAttrs,

      addIfAbsent: addIfAbsent,

      showConfirm: showConfirm,

      showErrorMsg: showErrorMsg,

      validateItems: validateItems,

      showItemsValidationResult: showItemsValidationResult,

      copyToClipboard: copyToClipboard,

      evaluate: evaluate,

      fns: fns,

      toBeDateTime: function(input, reqSecs) {
        var fmt = ui.os.global.shortDateFmt + ' HH:mm';
        if (reqSecs) {
          fmt += ':ss';
        }

        return $filter('date')(input, fmt);
      },

      formatDate: formatDate,

      linkify: linkify
    };
  });
