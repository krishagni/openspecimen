
angular.module('os.administrative.container.util', ['os.common.box'])
  .factory('ContainerUtil', function(
    $translate, BoxLayoutUtil, NumberConverterUtil, SpecimenUtil, Util, SpecimenTypeUtil) {

    var spmnTypeProps;

    function createSpmnPos(container, label, x, y, oldOccupant) {
      return {
        occuypingEntity: 'specimen', 
        occupyingEntityName: label,
        posOne: NumberConverterUtil.fromNumber(container.columnLabelingScheme, x),
        posTwo: NumberConverterUtil.fromNumber(container.rowLabelingScheme, y),
        posOneOrdinal: x,
        posTwoOrdinal: y,
        oldOccupant: oldOccupant
      };
    }

    function getOccupantDisplayName(container, occupant) {
      if (occupant.occuypingEntity == 'specimen') {
        if (container.cellDisplayProp == 'SPECIMEN_PPID') {
          return occupant.occupantProps.ppid;
        } else if (container.cellDisplayProp == 'SPECIMEN_BARCODE' && !!occupant.occupantProps.barcode) {
          return occupant.occupantProps.barcode;
        }
      }

      return occupant.occupyingEntityName;
    }

    function addColorCode(el, spmnClass, type) {
      var ret = getColorCode(spmnClass, type);
      el.css((ret && ret.css) || {});
    }

    function getColorCode(spmnClass, type) {
      var key = 'container_color_code';
      var props = spmnTypeProps[spmnClass + ':' + type];
      if (props && props.props && props.props[key]) {
        return {specimenClass: spmnClass, type: type, css: styleToJson(props.props[key])};
      } else {
        props = spmnTypeProps[spmnClass + ':*'];
        if (props && props.props && props.props[key]) {
          return {specimenClass: spmnClass, css: styleToJson(props.props[key])};
        }
      }

      return null;
    }

    // input is like: background = red, opacity = 0.6
    function styleToJson(input) {
      var kvList = input.split(',');
      var result = {};
      angular.forEach(kvList,
        function(kv) {
          var kvPair = kv.split('=');
          if (!kvPair || kvPair.length <= 1) {
            return;
          }

          result[kvPair[0].trim()] = kvPair[1].trim();
        }
      );

      return result;
    }

    function getOpts(container, allowClicks, showAddMarker, useBarcode) {
      return {
        box: {
          instance             : container,
          row                  : function(occupant) { return occupant.posTwoOrdinal; },
          column               : function(occupant) { return occupant.posOneOrdinal; },
          numberOfRows         : function() { return container.noOfRows; },
          numberOfColumns      : function() { return container.noOfColumns; },
          positionLabelingMode : function() { return container.positionLabelingMode; },
          rowLabelingScheme    : function() { return container.rowLabelingScheme; },
          columnLabelingScheme : function() { return container.columnLabelingScheme; },
          occupantClick        : function() { /* dummy method to make box allow cell clicks */ }
        },
        toggleCellSelect: function() { },
        allowEmptyCellSelect: true,
        occupants: [],
        occupantName: function(occupant) {
          if (!!useBarcode && occupant.occuypingEntity == 'specimen') {
            return occupant.occupantProps.barcode || '';
          }

          return occupant.occupyingEntityName;
        },
        occupantDisplayHtml: function(occupant) {
          var displayName = undefined;
          var cssClass = '';

          var el = angular.element('<span class="slot-desc"/>');

          if (occupant.occuypingEntity == 'specimen' && !!occupant.occupantProps) {
            displayName = getOccupantDisplayName(container, occupant);

            if (occupant.occupantProps.reserved) {
              cssClass = 'slot-reserved';
            } else {
              var spmnClass = occupant.occupantProps.specimenClass;
              var type = occupant.occupantProps.type;

              if (spmnTypeProps) {
                addColorCode(el, spmnClass, type);
              } else {
                SpecimenTypeUtil.getTypesWithProps().then(
                  function(typeProps) {
                    spmnTypeProps = typeProps;
                    addColorCode(el, spmnClass, type);
                  }
                );
              }
            }
          } else if (!!occupant.occupyingEntityName) {
            displayName = occupant.occupyingEntityName;
          } else if (occupant.blocked) {
            displayName = $translate.instant('container.cell_blocked');
            cssClass = 'slot-blocked';
          }

          return el.addClass(cssClass).attr('title', displayName).append(displayName);
        },
        allowClicks: allowClicks,
        isVacatable: function(occupant) {
          return occupant.occuypingEntity == 'specimen';
        },
        createCell: function(label, x, y, existing) {
          return createSpmnPos(container, label, x, y, existing);
        },
        onAddEvent: showAddMarker ? function() {} : undefined
      };
    }

    function getSpecimens(labels, filterOpts) {
      return SpecimenUtil.getSpecimens(labels, filterOpts).then(
        function(result) {
          if (!result || !result.specimens || result.specimens.length == 0) {
            return null;
          }

          return confirmTransferAction(!labels, result.specimens);
        }
      );
    }

    function confirmTransferAction(useBarcode, specimens) {
      var storedSpmns = specimens
        .filter(function(spmn) { return spmn.storageLocation && spmn.storageLocation.id > 0; })
        .map(function(spmn) { return useBarcode && spmn.barcode || spmn.label; });
      if (storedSpmns.length == 0) {
        return specimens;
      }

      return Util.showConfirm({
        title: 'container.transfer_spmns',
        confirmMsg: 'container.transfer_spmns_warn',
        isWarning: true,
        input: { storedSpmns: storedSpmns }
      }).then(
        function() { return specimens; },
        function() { return undefined; }
      );
    }

    return {
      fromOrdinal: NumberConverterUtil.fromNumber,

      toNumber: NumberConverterUtil.toNumber,

      getOpts: getOpts,

      assignPositions: function(container, occupancyMap, inputLabels, userOpts) {
        userOpts = userOpts || {};

        var opts = getOpts(container, false, false, userOpts.useBarcode);
        opts.occupants = occupancyMap;

        var result = BoxLayoutUtil.assignCells(opts, inputLabels, userOpts.vacateOccupants);
        return {map: result.occupants, noFreeLocs: result.noFreeLocs};
      },

      getSpecimens: getSpecimens,

      getColorCode: getColorCode,

      getTypesProps: function() {
        return spmnTypeProps;
      }
    };
  });
