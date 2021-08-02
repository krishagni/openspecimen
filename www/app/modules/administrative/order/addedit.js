
angular.module('os.administrative.order.addedit', ['os.administrative.models', 'os.biospecimen.models'])
  .controller('OrderAddEditCtrl', function(
    $scope, $state, $translate, $injector, $q, order, spmnRequest, requestDp, maxSpmnsLimit, customFields,
    PluginReg, Specimen, SpecimensHolder, Site, DistributionProtocol,
    DistributionOrder, SpecimenList, Alerts, Util, SpecimenUtil, ExtensionsUtil) {

    var ctx;
    var ignoreQtyWarning = false;

    function init() {
      $scope.order = order;
      ctx = $scope.ctx = {
        extnFormCtrl: {},
        extnOpts: undefined,
        customFields: customFields,
        spmnFilterOpts: {exactMatch: true, includeExtensions: customFields.length > 0},
        itemFieldsHdrTmpls:  PluginReg.getTmpls('order-addedit', 'item-fields-header', ''),
        itemFieldsCellTmpls: PluginReg.getTmpls('order-addedit', 'item-fields-addedit', '')
      };

      order.request = spmnRequest;
      if (requestDp) {
        order.distributionProtocol = requestDp;
      } else {
        loadDps();
      }

      $scope.dpList = [];
      $scope.siteList = [];
      $scope.userFilterOpts = {};

      if (!order.id && !!order.distributionProtocol) {
        $scope.onDpSelect();
      } else {
        setExtnFormCtxt(order);
        setUserAndSiteList(order);
        showOrHideHoldingLocation(order.distributionProtocol, order.orderItems);
      }

      $scope.input = {
        allItemStatus: false,
        noQtySpmnsPresent: false,
        spmnsFromExternalList: (!!order.specimenList && !!order.specimenList.id) || order.allReservedSpmns,
        limit: maxSpmnsLimit,
        order: order
      };

      if (!$scope.input.spmnsFromExternalList) {
        if (!order.id) {
          if (angular.isArray(SpecimensHolder.getSpecimens())) {
            var printLabels = false;
            var extra = SpecimensHolder.getExtra();
            if (typeof extra == 'string') {
              order.comments = extra;
            } else if (extra && typeof extra == 'object') {
              order.comments = extra.comments;
              printLabels = extra.printLabels;
            }

            order.orderItems = getOrderItems(SpecimensHolder.getSpecimens(), printLabels);
            $scope.input.allItemPrint = printLabels;
            SpecimensHolder.setSpecimens(null);
          } else if (!!order.request) {
            order.orderItems = getOrderItemsFromReq(order.request.items, []);
          }

          //
          // newly created order. item costs are auto-populated
          //
          $scope.input.moreSpmnsThanLimit = (order.orderItems && order.orderItems.length > maxSpmnsLimit);
          $scope.input.spmnsCount = order.orderItems ? order.orderItems.length : 0;
          addItemCosts(order.distributionProtocol, order.orderItems);
          loadCustomFields(order.orderItems);
        } else if (!!order.id) {
          loadOrderItems();
        }
      }

      if (!order.executionDate) {
        order.executionDate = new Date();
      }
      
      setHeaderStatus();
      setNoQtySpmnPresent();
    }

    function loadDps(name) {
      var filterOpts = {activityStatus: 'Active', query: name, excludeExpiredDps: true};
      DistributionProtocol.query(filterOpts).then(
        function(dps) {
          $scope.dpList = dps;
        }
      );
    }
    
    function loadOrderItems() {
      if (order.id && order.status == 'EXECUTED') {
        return;
      }

      order.getOrderItems({maxResults: maxSpmnsLimit + 1}).then(
        function(orderItems) {
          if (orderItems.length > maxSpmnsLimit) {
            order.copyItemsFromExistingOrder = $scope.input.moreSpmnsThanLimit = true;
            return;
          }

          order.orderItems = orderItems;

          if (!!spmnRequest) {
            order.orderItems = getOrderItemsFromReq(spmnRequest.items, order.orderItems);
          }

          loadCustomFields(order.orderItems);
        }
      );
    }
    
    function setUserFilterOpts(institute) {
      $scope.userFilterOpts = {institute: institute};
    }

    function setUserAndSiteList(order) {
      var instituteName = order.instituteName;
      setUserFilterOpts(instituteName);
    }

    function setExtnFormCtxt(order) {
      if (!order.distributionProtocol) {
        return;
      }

      ctx.extnOpts = undefined;
      DistributionProtocol.getOrderExtnCtxt(order.distributionProtocol.id).then(
        function(extnCtxt) {
          ctx.extnOpts = ExtensionsUtil.getExtnOpts(order, extnCtxt);
        }
      );
    }

    function getOrderItems(specimens, printLabel) {
      var result = specimens.filter(
        function(specimen) {
          return specimen.activityStatus == 'Active';
        }
      ).map(
        function(specimen) {
          ExtensionsUtil.createExtensionFieldMap(specimen, true);
          return {
            specimen: specimen,
            quantity: specimen.availableQty,
            status: 'DISTRIBUTED_AND_CLOSED',
            printLabel: printLabel
          }
        }
      );

      if (result.length < specimens.length) {
        var removedSpmns = specimens.filter(
          function(specimen) {
            return specimen.activityStatus != 'Active'
          }
        ).map(
          function(specimen) {
            return specimen.label;
          }
        );
        Util.showErrorMsg({msg: 'orders.specimens_removed', args: {specimens: removedSpmns}});
      }

      return result;
    }

    function getOrderItemsFromReq(reqItems, orderItems) {
      var orderItemsMap = {};
      angular.forEach(orderItems,
        function(orderItem) {
          orderItemsMap[orderItem.specimen.id] = orderItem;
        }
      );

      var items = [];
      angular.forEach(reqItems,
        function(reqItem) {
          var orderItem = orderItemsMap[reqItem.specimen.id];
          if (orderItem) {
            orderItem.specimen.selected = true;
          } else if (reqItem.status == 'PENDING') {
            orderItem = {
              specimen: reqItem.specimen,
              quantity: reqItem.specimen.availableQty,
              status: 'DISTRIBUTED_AND_CLOSED',
            }

            reqItem.specimen.selected = reqItem.selected;
          }

          if (orderItem) {
            items.push(orderItem);
          }
        }
      );

      return items;
    }

    function areItemQuantitiesValid(order, callback) {
      if (ignoreQtyWarning) {
        return true;
      }

      var moreQtyItems = order.orderItems.filter(
        function(item) {
          return angular.isNumber(item.specimen.availableQty) && item.specimen.availableQty < item.quantity;
        }
      );

      if (moreQtyItems.length > 0) {
        showInsufficientQtyWarning(moreQtyItems, callback);
        return false;
      }

      return true;
    }

    function showInsufficientQtyWarning(items, callback) {
      Util.showConfirm({
        ok: function () {
          ignoreQtyWarning =  true;
          callback();
        },
        title: "common.warning",
        isWarning: true,
        confirmMsg: "orders.errors.insufficient_qty",
        input: {
          count: items.length
        }
      });
    }

    function anyItemHasHoldingLocation(order) {
      return (order.orderItems || []).some(function(item) { return !!item.holdingLocation && !!item.holdingLocation.name; });
    }

    function saveOrUpdate(order, status) {
      if (!areItemQuantitiesValid(order, function() { saveOrUpdate(order, status); })) {
        return;
      }

      order.siteId = undefined;

      var orderClone = angular.extend(angular.copy(order), {status: status});
      if (!!spmnRequest && order.status != 'EXECUTED') {
        var items = [];
        angular.forEach(orderClone.orderItems,
          function(item) {
            if (!item.specimen.selected) {
              return;
            }

            delete item.specimen.selected;
            items.push(item);
          }
        );

        if (items.length == 0) {
          Alerts.error('orders.no_specimens_in_list');
          return;
        }

        orderClone.orderItems = items;
      }

      var formCtrl = ctx.extnFormCtrl.ctrl;
      if (formCtrl) {
        orderClone.extensionDetail = formCtrl.getFormData();
      }

      orderClone.$saveOrUpdate().then(
        function(savedOrder) {
          if (savedOrder.completed) {
            $state.go('order-detail.overview', {orderId: savedOrder.id});
          } else {
            Alerts.info('orders.more_time');
            $state.go('order-list');
          }
        },

        function(errResp) {
          Util.showErrorMsg(errResp);
        }
      );
    };
    
    function setHeaderStatus() {
      var isOpenPresent = false;
      angular.forEach($scope.order.orderItems,
        function(item) {
          if (item.status == 'DISTRIBUTED') {
            isOpenPresent = true;
          }
        }
      );

      if (isOpenPresent) {
        $scope.input.allItemStatus = false;
      } else {
        $scope.input.allItemStatus = true;
      }
    }

    function setNoQtySpmnPresent() {
      if (!!$scope.order.orderItems && $scope.order.orderItems.length > 0) {
        $scope.input.noQtySpmnsPresent = anyNoQtySpmns($scope.order.orderItems);
      }
    }

    function anyNoQtySpmns(items) {
      return (items || []).some(
        function(item) {
          return !item.specimen.availableQty;
        }
      );
    }

    function addItemCosts(dp, items) {
      if (!$injector.has('distInvDistributionCost') || items.length > maxSpmnsLimit) {
        return;
      }

      var spmnIds = (items || []).map(function(item) { return item.specimen.id });
      if (!dp || !dp.id || spmnIds.length == 0) {
        return;
      }

      $injector.get('distInvDistributionCost').getCosts(dp.id, spmnIds).then(
        function(resp) {
          var spmnCosts = resp.specimenCosts;

          angular.forEach(items,
            function(item) {
              item.cost = spmnCosts[item.specimen.id];
            }
          );
        }
      );
    }

    function showOrHideHoldingLocation(dp, items) {
      if (!dp) {
        ctx.allowHoldingLocations = false;
        return;
      }

      dp.hasDistributionContainers().then(
        function(result) {
          if (!result) {
            (items || []).forEach(function(item) { delete item.holdingLocation; });
          }

          ctx.allowHoldingLocations = result;
        }
      );
    }

    function getValidationMsgKeys(useBarcode) {
      return {
        title:         'orders.specimen_validation.title',
        foundCount:    'orders.specimen_validation.found_count',
        notFoundCount: 'orders.specimen_validation.not_found_count',
        notFoundError: 'orders.specimen_validation.not_found_error',
        extraCount:    'orders.specimen_validation.extra_count',
        extraError:    'orders.specimen_validation.extra_error',
        itemLabel:     useBarcode ? 'specimens.barcode' : 'specimens.label',
        error:         'common.error',
        reportCopied:  'orders.specimen_validation.report_copied'
      }
    }

    function loadCustomFields(items) {
      if (customFields.length <= 0) {
        return;
      }

      var spmnIds = [];
      var itemsMap = {};
      angular.forEach(items,
        function(item) {
          spmnIds.push(item.specimen.id);
          itemsMap[item.specimen.id] = item;
        }
      );

      Specimen.getByIds(spmnIds, true).then(
        function(spmns) {
          angular.forEach(spmns,
            function(spmn) {
              itemsMap[spmn.id].specimen = spmn;
              ExtensionsUtil.createExtensionFieldMap(spmn, true);
            }
          );

          initCustomTableFields(items);
        }
      );
    }

    function initCustomTableFields(items) {
      if (customFields.length <= 0) {
        return;
      }

      var fieldsSvc = $injector.get('sdeFieldsSvc');
      angular.forEach(items,
        function(item) {
          item.$$customFields = customFields.map(
            function(field) {
              var result = {type: field.type, value: undefined};
              if (field.type == 'specimen-desc') {
                return result;
              }

              $q.when(fieldsSvc.commonFns().getValue({field: field}, item)).then(
                function(value) {
                  result.value = value;
                }
              );

              return result;
            }
          );
        }
      );
    }

    $scope.onDpSelect = function() {
      var ord = $scope.order;
      if (!ord.id) {
        ord.name = ord.distributionProtocol.shortTitle + '_' + Util.toBeDateTime(new Date(), true);
      }

      ord.instituteName = ord.distributionProtocol.instituteName;
      ord.siteName = ord.distributionProtocol.defReceivingSiteName;
      if (spmnRequest && spmnRequest.requestor) {
        ord.requester = spmnRequest.requestor;
      } else {
        ord.requester = ord.distributionProtocol.principalInvestigator;
      }
      setUserAndSiteList(ord);
      setExtnFormCtxt(ord);

      showOrHideHoldingLocation(ord.distributionProtocol, ord.orderItems);
      addItemCosts(ord.distributionProtocol, ord.orderItems);
    }
    
    $scope.onInstSelect = function (instituteName) {
      setUserFilterOpts(instituteName);
      $scope.order.siteName = '';
      $scope.order.requester = '';
    }

    $scope.addSpecimens = function(specimens) {
      if (!specimens) {
        return false;
      }

      ignoreQtyWarning = false;
      var newItems = getOrderItems(specimens);
      if (newItems.length + $scope.order.orderItems.length > maxSpmnsLimit) {
        var params = {allowed: (maxSpmnsLimit - $scope.order.orderItems.length), limit: maxSpmnsLimit};
        Alerts.error('orders.more_specimens_warning', params, 15000);
        return;
      }

      var addedItems = Util.addIfAbsent($scope.order.orderItems, newItems, 'specimen.id');
      $scope.input.noQtySpmnsPresent = $scope.input.noQtySpmnsPresent || anyNoQtySpmns(newItems);
      addItemCosts($scope.order.distributionProtocol, addedItems);

      initCustomTableFields(addedItems);
      return true;
    }

    $scope.removeOrderItem = function(orderItem) {
      var idx = order.orderItems.indexOf(orderItem);
      if (idx == -1) {
        return;
      }

      order.orderItems.splice(idx, 1);
      if ($scope.input.noQtySpmnsPresent && !orderItem.specimen.availableQty) {
        $scope.input.noQtySpmnsPresent = anyNoQtySpmns(order.orderItems);
      }
    }

    $scope.distribute = function() {
      saveOrUpdate($scope.order, 'EXECUTED');
    }

    $scope.saveDraft = function() {
      if(anyItemHasHoldingLocation($scope.order)) {
        Alerts.error('orders.holding_loc_draft_not_allowed');
        return;
      }

      saveOrUpdate($scope.order, 'PENDING');
    }

    $scope.passThrough = function() {
      return true;
    }

    $scope.showSpecimens = function() {
      var formCtrl = ctx.extnFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        Alerts.error('common.form_validation_error');
        return false;
      }

      if (order.status == 'EXECUTED') {
        return true;
      }

      var countFn, spmnsFn;
      if (order.specimenList && order.specimenList.id) {
        countFn = function() { return order.specimenList.getSpecimensCount({available: true}) };
        spmnsFn = function(maxResults) { return order.specimenList.getSpecimens({available: true, maxResults: maxResults}) };
      } else if (order.allReservedSpmns) {
        countFn = function() { return order.distributionProtocol.getReservedSpecimensCount() };
        spmnsFn = function(maxResults) { return order.distributionProtocol.getReservedSpecimens({maxResults: maxResults}) };
      }

      if (countFn && spmnsFn) {
        countFn().then(
          function(spmnsCount) {
            $scope.input.spmnsCount = spmnsCount;
            if (spmnsCount > maxSpmnsLimit) {
              $scope.input.moreSpmnsThanLimit = true;
              return;
            }

            spmnsFn(spmnsCount + 1).then(
              function(spmns) {
                order.orderItems = getOrderItems(spmns);
                loadCustomFields(order.orderItems);
                order.allReservedSpmns = order.specimenList = null;
                $scope.input.spmnsFromExternalList = false;
                setNoQtySpmnPresent();
              }
            );
          }
        );
      }

      return true;
    }

    $scope.areItemQuantitiesValid = function(doWizard) {
      return areItemQuantitiesValid($scope.order, function () { doWizard.next(false); });
    }

    $scope.setStatus = function(item) {
      var selected = !spmnRequest ? true : item.specimen.selected;

      if (((item.holdingLocation && item.holdingLocation.name) ||
           (!item.specimen.availableQty || item.quantity >= item.specimen.availableQty)) &&
           selected) {
        item.status = 'DISTRIBUTED_AND_CLOSED';
      } else {
        item.status = 'DISTRIBUTED';
      }
      
      setHeaderStatus();
    }
    
    $scope.setHeaderStatus = setHeaderStatus;

    $scope.toggleAllItemStatus = function() {
      var allStatus = $scope.input.allItemStatus ? 'DISTRIBUTED_AND_CLOSED' : 'DISTRIBUTED';
      angular.forEach($scope.order.orderItems,
        function(item) {
          if (!item.holdingLocation || !item.holdingLocation.name) {
            item.status = allStatus;
          }
        }
      );
    }
    
    $scope.searchDp = loadDps;

    $scope.toggleAllSpecimensSelect = function() {
      angular.forEach($scope.order.orderItems,
        function(item) {
          item.specimen.selected = $scope.input.allSelected;
        }
      );
    }

    $scope.toggleSpecimenSelect = function(item) {
      if (!item.specimen.selected) {
        $scope.input.allSelected = false;
      } else {
        var allNotSelected = $scope.order.orderItems.some(
          function(item) {
            return !item.specimen.selected;
          }
        );

        $scope.input.allSelected = !allNotSelected;
      }

      $scope.setStatus(item);
    }

    $scope.setHeaderPrint = function(item) {
      if (!item.printLabel) {
        $scope.input.allItemPrint = false;
      } else {
        $scope.input.allItemPrint = !(order.orderItems.some(function(oi) { return !oi.printLabel; }));
      }
    }

    $scope.toggleAllItemPrint = function() {
      angular.forEach($scope.order.orderItems,
        function(item) {
          item.printLabel = $scope.input.allItemPrint;
        }
      );
    }

    $scope.validateSpecimens = function(ctrl) {
      var prop = ctrl.useBarcode() ? 'specimen.barcode' : 'specimen.label';
      var result = Util.validateItems($scope.order.orderItems, ctrl.getLabels(), prop);
      Util.showItemsValidationResult(getValidationMsgKeys(ctrl.useBarcode()), result);
    }

    $scope.copyFirstLocationToAll = function() {
      var orderItems = $scope.order.orderItems;
      if (!orderItems || orderItems.length <= 1) {
        return;
      }

      var location = {};
      if (orderItems[0].holdingLocation && orderItems[0].holdingLocation.name) {
        location = {name: orderItems[0].holdingLocation.name, mode: orderItems[0].holdingLocation.mode};
      }

      for (var i = 1; i < orderItems.length; ++i) {
        orderItems[i].holdingLocation = angular.extend({}, location);
        if (orderItems[i].holdingLocation.name) {
          orderItems[i].status = 'DISTRIBUTED_AND_CLOSED';
        }
      }

      setHeaderStatus();
    }

    $scope.copyFirstQtyToAll = function() {
      var orderItems = $scope.order.orderItems;
      if (!orderItems || orderItems.length <= 1) {
        return;
      }

      var qty = orderItems[0].quantity;
      for (var i = 0; i < orderItems.length; ++i) {
        var item = orderItems[i];
        item.quantity = qty;

        var selected = !spmnRequest ? true : item.specimen.selected;
        if ((!item.specimen.availableQty || item.quantity >= item.specimen.availableQty) && selected) {
          item.status = 'DISTRIBUTED_AND_CLOSED';
        } else {
          item.status = 'DISTRIBUTED';
        }
      }

      setHeaderStatus();
    }
    
    init();
  });
