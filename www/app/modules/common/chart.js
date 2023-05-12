angular.module('openspecimen')
  .directive('osChart', function() {
    var showLegends = ['pie', 'doughnut'];

    function pct(value, total, hideSmaller) {
      var result = '';
      if (total > 0) {
        result = Math.round(value * 100 / total);
        if (hideSmaller && result < 5) {
          result = '';
        } else {
          result += '%';
        }
      }

      return result;
    }

    function sumOf(values) {
      return values.reduce(function(value, sum) { return sum + value; }, 0);
    }

    function getAnimationOpts(pctFn) {
      return {
        duration: 500,
        easing: "easeOutQuart",
        onComplete: function() {
          var ctx = this.chart.ctx;
          ctx.font = Chart.helpers.fontString(
            Chart.defaults.global.defaultFontFamily, 'normal', Chart.defaults.global.defaultFontFamily);
          ctx.textAlign = 'center';
          ctx.textBaseline = 'bottom';

          this.data.datasets.forEach(
            function (dataset) {
              for (var i = 0; i < dataset.data.length; i++) {
                var model = dataset._meta[Object.keys(dataset._meta)[0]].data[i]._model,
                    radius = model.outerRadius - (model.outerRadius - model.innerRadius) / 4
                    startAngle = model.startAngle,
                    endAngle = model.endAngle,
                    midAngle = startAngle + (endAngle - startAngle) / 2;

                var x = radius * Math.cos(midAngle);
                var y = radius * Math.sin(midAngle);

                var pctStr = pctFn(dataset.data[i], true);
                if (pctStr) {
                  ctx.fillStyle = '#fff';
                  ctx.fillText(pctStr, model.x + x, model.y + y + 5);
                }
              }
            }
          )
        }
      };
    }

    function addCategoryTimeAxis(options) {
      options.scales = options.scales || {};
      options.scales.xAxes = [{
        type: 'time',
        time: {
          parser: ui.os.global.shortDateFmt.toUpperCase() + ' HH:mm',
          displayFormat: ui.os.global.dateFmt.toUpperCase()
        }
      }]
    }

    return {
      restrict: 'E',

      replace: 'true',

      scope: {
        data: '=',
        options: '='
      },

      link: function(scope, element, attrs) {
        var type = scope.options.type = scope.options.type || 'line';
        var total = 0;
        scope.categories = [];

        if (showLegends.indexOf(type) != -1) {
          scope.options.legend = {display: true, position: 'bottom'};
        }

        var options = scope.options = scope.options || {};
        if (options.type != 'doughnut' && options.type != 'pie') {
          var scales = options.scales = options.scales || {};
          scales.yAxes = [{
            ticks: {
              beginAtZero: true
            }
          }];
        }

        scope.override = [];
        scope.$watch('data',
          function() {
            if (scope.data.series && scope.data.series.length > 1) {
              scope.options.legend = {display: true, position: 'bottom'};
            }

            scope.override = scope.data.values.map(
              function() {
                return {pointRadius: 0, pointHitRadius: 10};
              }
            );

            scope.categories = scope.data.categories;
            if (showLegends.indexOf(type) != -1) {
              if (scope.options.showLegendValuePct == true) {
                scope.categories = [];
                total = sumOf(scope.data.values);
                angular.forEach(scope.data.categories,
                  function(category, index) {
                    scope.categories[index] = category + ' (' + pct(scope.data.values[index], total) + ')';
                  }
                );
              } else if (scope.options.showSectionValuePct == true) {
                // scope.options.events = false;
                total = sumOf(scope.data.values);
                scope.options.animation = getAnimationOpts(function(value) { return pct(value, total, true); });
              }
            }

            if (scope.data.categoryType == 'DATE') {
              addCategoryTimeAxis(scope.options);
            } else {
              scope.options.scales.xAxes = [];
            }
          }
        );
      },

      template:
        '<div>' +
        '  <canvas class="chart-base" chart-type="options.type" chart-data="data.values" ' +
        '    chart-labels="categories" chart-series="data.series" chart-options="options" ' +
        '    chart-legend="true" chart-dataset-override="override">' +
        '  </canvas>' +
        '</div>'
    }
  });
