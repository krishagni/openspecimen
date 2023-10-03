angular.module('openspecimen')
  .factory('ListPagerOpts', function() {
    var maxRecords = 100;

    var Opts = function(options) {
      angular.extend(this, options);
      if (!angular.isDefined(this.recordsPerPage)) {
        this.recordsPerPage = maxRecords;
      }

      this.modifCnt = 0;
    }

    Opts.prototype.refreshOpts = function(list) {
      this.modifCnt++;
      this.currentPageRecs = list.length;

      if (list.length > this.recordsPerPage) {
        list.splice(this.recordsPerPage, list.length);
      }
    }

    Opts.MAX_PAGE_RECS = maxRecords;

    return Opts;
  })
  .directive('osListPager', function($q) {

    function showListSize(pagerCtx, opts) {
      pagerCtx.listSize = -1;
      pagerCtx.showListSize = true;

      $q.when(opts.listSizeGetter()).then(
        function(resp) {
          pagerCtx.listSize = resp.count;
        }
      );
    }

    return {
      restrict: 'E',

      replace: true,

      scope: {
        opts: '='
      },

      link: function(scope, element, attrs) {
        var pagerCtx = scope.pagerCtx  = {
          showListSize: false,
          listSize: -1,
          viewSize: 0
        };

        scope.$watch('opts',
          function(opts) {
            if (!opts) {
              return;
            }

            if (opts.currentPageRecs > opts.recordsPerPage) {
              angular.extend(pagerCtx, {showMore: true, viewSize: opts.recordsPerPage});
              if (pagerCtx.showListSize && !opts.$$pageSizeChanged) {
                showListSize(pagerCtx, opts);
              }
            } else {
              angular.extend(pagerCtx, {showMore: false, viewSize: opts.currentPageRecs});
            }

            if (angular.isDefined(opts.$$pageSizeChanged) && opts.$$pageSizeChanged > 0) {
              opts.$$pageSizeChanged--;
            }
          }, true);

        scope.showListSize = function() {
          showListSize(pagerCtx, scope.opts);
        }
      },

      templateUrl: 'modules/common/list-pager.html'
    }
  })

  .directive('osListPageSize', function(Alerts, Util) {
    return {
      restrict: 'E',

      replace: true,

      templateUrl: 'modules/common/list-page-size.html',

      scope: {
        opts: '=',

        onChange: '&'
      },

      link: function(scope, element, attrs) {
        var pctx = scope.pctx = {
          enableCustomPageSize: false,
          customPageSize: 100
        };

        var pageSizeChanged = scope.pageSizeChanged = function(newPageSize) {
          var opts = scope.opts;
          if (!opts || opts.recordsPerPage == newPageSize) {
            return;
          }

          opts.recordsPerPage = newPageSize;
          opts.$$pageSizeChanged = 2; // +1 for recordsPerPage and +1 for currentRecsPerPage
          scope.onChange({recordsPerPage: newPageSize});
        }

        scope.changePageSize = function() {
          var opts = scope.opts;
          if (+pctx.customPageSize == opts.recordsPerPage) {
            return;
          }

          if (!pctx.customPageSize || pctx.customPageSize <= 0) {
            Alerts.error('list_pager.invalid_page_size');
            return;
          }

          if (pctx.customPageSize > 500) {
            Util.showConfirm({
              title: 'common.warning',
              confirmMsg: 'list_pager.confirm_large_page_size',
              ok: function() {
                pageSizeChanged(+pctx.customPageSize);
              },
              cancel: function() {
              }
            });
          } else {
            pageSizeChanged(+pctx.customPageSize);
          }
        }
      }
    }
  });
