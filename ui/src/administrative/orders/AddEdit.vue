<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.order.id">Create Order</h3>
        <h3 v-else>Update {{dataCtx.order.name}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="ctx.loading">
        <os-message type="info">
          <span>Loading the form. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else>
        <os-steps ref="orderWizard">
          <os-step title="Order Details" :validate="validateDetails">
            <os-form ref="orderDetails" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
              <div>
                <os-button class="next" primary label="Next" @click="next" />

                <os-button v-if="dataCtx.order.status == 'EXECUTED'" primary label="Update" @click="updateOrder" />

                <span v-if="dataCtx.order.status != 'EXECUTED' && dataCtx.orderItems.length > 0">
                  <os-button primary label="Save Draft" @click="saveDraft" />

                  <os-button primary label="Distribute" @click="distribute" />
                </span>

                <os-button text label="Cancel"  @click="cancel" />
              </div>
            </os-form>
          </os-step>

          <os-step title="Specimens" :validate="validateSpecimenDetails" v-if="dataCtx.order.status == 'PENDING'">
            <span v-if="ctx.maxSpmnsLimitExceeded">
              <os-message type="warn">
                <span>
                  The order has more specimens {{ctx.itemsCount && '(' + ctx.itemsCount + ')'}} than the 
                  allowed limit ({{ctx.maxSpmnsLimit}}) for displaying on UI. Please use CSV import if you 
                  like to specify additional details (quantity, cost etc). 
                  Contact your Super Administrator to modify the UI limit.
                </span>
              </os-message>
            </span>

            <span v-else>
              <os-add-specimens ref="addSpmns" :criteria="ctx.specimensCriteria"
                label="Add specimens by scanning labels or barcodes separated by a comma, tab, or newline"
                @on-add="addSpecimens">
                <os-button label="Validate" @click="validateSpecimenLabels" />
              </os-add-specimens>

              <os-message type="error" v-if="!dataCtx.orderItems || dataCtx.orderItems.length == 0">
                <span>No specimens in the order. Add at least one specimen.</span>
              </os-message>

              <os-table-form v-else ref="specimenDetails" :schema="ctx.specimensSchema"
                :data="dataCtx" :items="dataCtx.orderItems"
                :remove-items="true" @remove-item="removeSpecimen($event)"
                @input="handleOrderItemInput($event)">
              </os-table-form>
            </span>

            <os-divider />

            <div class="os-form-footer">
              <os-button class="previous" secondary label="Previous" @click="previous" />
              <os-button class="next" primary label="Next" @click="next" />
              <os-button primary label="Save Draft" @click="saveDraft"   v-if="dataCtx.order.status == 'PENDING'" />
              <os-button primary label="Distribute" @click="distribute"  v-if="dataCtx.order.status == 'PENDING'" />
              <os-button primary label="Update"     @click="updateOrder" v-if="dataCtx.order.status == 'EXECUTED'" />
              <os-button text    label="Cancel"     @click="cancel" />
            </div>

            <os-items-validation ref="validationsDialog" :report-messages="validationReportMsgs">
              <template #title>
                <span>Specimens Validation Report</span>
              </template>
              <template #found>
                <span>Passed</span>
              </template>
              <template #notFound>
                <span>Failed: Specimens not present in the order</span>
              </template>
              <template #extras>
                <span>Failed: Additional specimens present in the order</span>
              </template>
            </os-items-validation>
          </os-step>

          <os-plugin-views page="order-addedit" view="steps" :view-props="{order: dataCtx.order}" />
        </os-steps>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc    from '@/common/services/Alerts.js';
import formUtil    from '@/common/services/FormUtil.js';
import routerSvc   from '@/common/services/Router.js';
import settingsSvc from '@/common/services/Setting.js';
import util        from '@/common/services/Util.js';
import orderSvc    from '@/administrative/services/Order.js';

import specimensSchema  from '@/administrative/schemas/orders/order-specimens.js';

export default {
  props: ['orderId'],

  inject: ['ui'],

  setup() {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('OrdersList', {orderId: -1}), label: 'Orders'}
      ],

      addEditFs: {rows: []},

      specimensSchema: {columns: []},

      loading: true,

      specimensCriteria: {exactMatch: true},

      maxSpmnsLimit: undefined,

      maxSpmnsLimitExceeded: false,

      itemsCount: undefined
    });

    let dataCtx = reactive({
      order: {},

      currentUser: ui.currentUser,

      receivingInstitute: undefined,

      invoicingEnabled: ui.global.appProps.plugins.indexOf('distribution-invoicing') != -1
    });

    return { ctx, dataCtx };
  },

  created: async function() {
    const setting = await settingsSvc.getSetting('administrative', 'max_order_spmns_ui_limit');
    this.ctx.maxSpmnsLimit = +setting[0].value || 100;

    let columns = specimensSchema.columns;
    const customFields = await orderSvc.getCustomFields();
    if (customFields && customFields.length > 0) {
      columns = columns.filter(column => column.name.indexOf('specimen.') != 0);
      columns.splice(0, 0, ...formUtil.fromSde(customFields, true));
      columns[0].href = (rowObject) => this.$ui.ngServer + '#/specimens/' + rowObject.specimen.id;
      this.ctx.hasCustomSpecimenFields = true;
      this.ctx.specimensCriteria.includeExtensions = true;
    }

    this.ctx.specimensSchema = { columns };
    this.loadOrder();
  },

  mounted: function() {
    this.$el.addEventListener('saveDraft',  () => this.saveDraft());
    this.$el.addEventListener('distribute', () => this.distribute());
    this.$el.addEventListener('update',     () => this.updateOrder());
    this.$el.addEventListener('cancel',     () => this.cancel());
  },

  computed: {
    validationReportMsgs: function() {
      return {
        label: this.$refs.addSpmns && this.$refs.addSpmns.useBarcode ? 'Barcode' : 'Label',
        error: 'Error',
        notFound: 'Not present in the order',
        extra: 'Additional specimen present in the order'
      }
    }
  },

  watch: {
    'orderId': function (newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.loadOrder();
    }
  },

  methods: {
    loadOrder: async function() {
      const ctx        = this.ctx;
      const dataCtx    = this.dataCtx;

      ctx.loading = true;
      let order = dataCtx.order = {
        executionDate: new Date(),
        status: 'PENDING',
      };

      if (this.orderId && +this.orderId > 0) {
        order = dataCtx.order = await orderSvc.getOrder(this.orderId);
        formUtil.createCustomFieldsMap(order);
      }

      let input = localStorage['os.orderDetails'] && JSON.parse(localStorage['os.orderDetails']);
      if (order.id && order.status != 'EXECUTED') {
        input = {
          clearFromCart: order.clearListId,
          clearCart: order.clearListMode,
          dp: order.distributionProtocol,
          comments: order.comments,
          specimenListId: order.specimenList && order.specimenList.id,
          clearListId: order.clearListId,
          clearListMode: order.clearListMode,
          allReserved: !!order.allReservedSpmns
        };
      }

      const promises = [ orderSvc.getAddEditFormSchema(order.distributionProtocol) ];
      if (input) {
        if (input.specimenIds) {
          //
          // specific specimens selected from any list view
          //
          promises.push(orderSvc.createOrderItemsFromSpecimens(input.specimenIds, input.printLabel));

          if (input.requestId) {
            //
            // specimens were selected from catalog request
            //
            order.request = {id: input.requestId, catalogId: input.catalogId};
            order.requester = input.requestor;
          }
        } else if (input.specimenListId) {
          //
          // distribute entire cart
          //
          const size = this.ctx.itemsCount = await orderSvc.getCartSize(input.specimenListId)
          if (size > this.ctx.maxSpmnsLimit) {
            order.specimenList = {id: input.specimenListId};
            this.ctx.maxSpmnsLimitExceeded = true;
            promises.push(new Promise((resolve) => resolve([])));
          } else {
            order.specimenList = null;
            promises.push(orderSvc.createOrderItemsFromCart(input.specimenListId, input.printLabel));
          }
        } else if (input.allReserved && input.dp) {
          //
          // distribute all reserved specimens of the selected DP
          //
          const size = this.ctx.itemsCount = await orderSvc.getReservedSpecimensListSize(input.dp.id);
          if (size > this.ctx.maxSpmnsLimit) {
            order.allReservedSpmns = true;
            this.ctx.maxSpmnsLimitExceeded = true;
            promises.push(new Promise((resolve) => resolve([])));
          } else {
            order.allReservedSpmns = false;
            promises.push(orderSvc.createOrderItemsFromReservedSpecimens(input.dp.id, input.printLabel));
          }
        } else if (input.requestId) {
          order.request = {id: input.requestId, catalogId: input.catalogId};
          order.requester = input.requestor;
          if (!input.specimenIds || input.specimenIds.length == 0) {
            //
            // distribute all specimens of the catalog request
            //
            promises.push(orderSvc.createOrderItemsFromRequest(input.catalogId, input.requestId, input.printLabel));
          }
        } else if (order.id) {
          promises.push(orderSvc.getOrderItems(this.orderId, {maxResults: this.ctx.maxSpmnsLimit + 1}));
        }

        order.clearListId   = input.clearFromCart || input.clearListId;
        order.clearListMode = input.clearCart || input.clearListMode;
        order.distributionProtocol = input.dp;
        order.comments      = input.comments;
      } else {
        dataCtx.orderItems = [];
      }

      localStorage.removeItem('os.orderDetails');
      Promise.all(promises).then(
        async (result) => {
          ctx.loading = false;

          let idx = 0;
          const {schema, defaultValues} = result[idx++];

          ctx.addEditFs = schema;
          if (!order.id) {
            order.extensionDetail = { attrsMap: defaultValues || {} };
          }

          if (result.length > 1) {
            const items = dataCtx.orderItems = result[idx++];
            items.forEach(item => item.dispose = item.status == 'DISTRIBUTED_AND_CLOSED');
            if (items.length > this.ctx.maxSpmnsLimit) {
              this.ctx.maxSpmnsLimitExceeded = true;
              if (order.id) {
                dataCtx.orderItems = [];
                order.copyItemsFromExistingOrder = true;
              }
            }

            if (this.ctx.hasCustomSpecimenFields) {
              const specimenIds = [];
              const itemsMap = {};
              items.forEach(item => {
                itemsMap[item.specimen.id] = item;
                specimenIds.push(item.specimen.id);
              });

              const specimens = await this.$osSvc.specimenSvc.getByIds(specimenIds, true);
              specimens.forEach(specimen => {
                itemsMap[specimen.id].specimen = formUtil.createCustomFieldsMap(specimen, true);
              });
            }
          }

          if (!order.id && order.distributionProtocol) {
            //
            // to drive automatic selection of fields based on the selected DP
            //
            this.handleInput({
              field: {name: 'order.distributionProtocol'},
              value: order.distributionProtocol,
              data: dataCtx
            });
          }
        },

        () => {
          ctx.loading = false;
        }
      );
    },

    handleInput: async function({field, value, data}) {
      if (field.name == 'order.distributionProtocol') {
        const dp    = value;
        const order = this.dataCtx.order;

        order.instituteName = dp.instituteName;
        order.siteName      = dp.defReceivingSiteName;
        order.requester     = order.requester || dp.principalInvestigator;
        if (!order.id) {
          const dateFmt = this.$ui.global.locale.shortDateFmt;
          order.name = dp.shortTitle + '_' + util.formatDate(new Date(), dateFmt + '_HH:mm:ss');
        }

        const {schema, defaultValues} = await orderSvc.getAddEditFormSchema(dp);
        this.ctx.addEditFs = schema;
        this.dataCtx.order.extensionDetail = { attrsMap: defaultValues || {} };

        await this.addItemCosts(this.dataCtx.orderItems);
      }

      Object.assign(this.dataCtx, data);
    },

    next: function() {
      if (this.$refs.orderWizard.currentStep == 0) {
        this.showOrHideHoldingLocation();
      }

      this.$refs.orderWizard.next();
    },

    previous: function() {
      this.$refs.orderWizard.previous();
    },

    validateDetails: function() {
      return this.$refs.orderDetails.validate();
    },

    validateSpecimenDetails: function(forward) {
      if (this.ctx.maxSpmnsLimitExceeded) {
        // no table is displayed. so move on
        return true;
      } else if (!this.$refs.specimenDetails) {
        // max. limit is not breached. specimen table is not displayed.
        if (forward) {
          alertSvc.error('No specimens in the order. Add at least one specimen.');
          return false;
        } else {
          return true;
        }
      } else {
        return this.$refs.specimenDetails.validate();
      }
    },

    validateSpecimenLabels: function() {
      const labels = this.$refs.addSpmns.getLabels();
      const prop   = this.$refs.addSpmns.useBarcode ? 'specimen.barcode' : 'specimen.label';
      this.$refs.validationsDialog.validate(this.dataCtx.orderItems, labels, prop);
    },

    handleOrderItemInput: function({item, itemIdx}) {
      Object.assign(this.dataCtx.orderItems[itemIdx], item);
    },

    addSpecimens: async function({specimens}) {
      const orderItems = specimens.filter(specimen => specimen.activityStatus == 'Active')
        .map((specimen) => ({
          specimen: formUtil.createCustomFieldsMap(specimen, true),
          quantity: specimen.availableQty,
          dispose: true
        }));
      if (specimens.length != orderItems.length) {
        alertSvc.error('One or more specimens not added to the order, as they are already distributed or disposed.');
      }

      const limit    = this.ctx.maxSpmnsLimit;
      const newItems = util.getAbsentItems(this.dataCtx.orderItems, orderItems, 'specimen.id');
      if (newItems.length > 0) {
        if ((newItems.length + this.dataCtx.orderItems.length) > limit) {
          const allowed = limit - this.dataCtx.orderItems.length;

          alertSvc.error(
            (allowed == 0 ?
              'No more specimens can be added. ' :
              'You can add only ' + allowed + ' more specimens using UI. ') +
            'If you want to distribute more than ' + limit + ' specimens then ' +
            'consider using CSV import or specimens cart. Contact your super Administrator to ' +
            'modify the UI limit.'
          );
          return;
        }

        await this.addItemCosts(newItems);
        this.dataCtx.orderItems.push(...newItems);
      }
    },

    removeSpecimen: function({idx}) {
      this.dataCtx.orderItems.splice(idx, 1);
    },

    showOrHideHoldingLocation: async function() {
      const dp         = this.dataCtx.order.distributionProtocol;
      const orderItems = this.dataCtx.orderItems || [];

      let allowHoldingLocations;
      if (!dp) {
        allowHoldingLocations = false;
      } else {
        allowHoldingLocations = await orderSvc.hasDistributionContainers(dp);
      }

      this.dataCtx.allowHoldingLocations = allowHoldingLocations;
      if (!allowHoldingLocations) {
        orderItems.forEach(item => item.holdingLocation = null);
      }
    },

    addItemCosts: async function(items) {
      if (!this.dataCtx.invoicingEnabled) {
        return;
      }

      const dp = this.dataCtx.order.distributionProtocol;
      if (!dp || !dp.id) {
        return;
      }

      const spmnIds = (items || []).map(item => item.specimen.id);
      if (spmnIds.length == 0) {
        return;
      }

      const { specimenCosts } = await orderSvc.getCosts(dp.id, spmnIds);
      items.forEach(item => item.cost = specimenCosts[item.specimen.id]);
    },

    saveDraft: async function() {
      const saved = await this.saveOrUpdate('PENDING');
      alertSvc.success('Draft order "' + saved.name + '" saved!');
    },

    distribute: async function() {
      const saved = await this.saveOrUpdate('EXECUTED');
      alertSvc.success('Order "' + saved.name + '" distributed!');
    },

    updateOrder: async function() {
      const saved = await this.saveOrUpdate(null);
      alertSvc.success('Order "' + saved.name + '" updated!');
    },

    saveOrUpdate: async function(status) {
      const order      = this.dataCtx.order;
      const orderItems = this.dataCtx.orderItems;

      const fromList = order.specimenList || order.allReservedSpmns || order.copyItemsFromExistingOrder;
      if (order.status != 'EXECUTED' && !fromList && (!orderItems || orderItems.length == 0)) {
        alertSvc.error('No specimens in the order. Add at least one specimen.');
        return;
      }

      if (this.$refs.specimenDetails && !this.$refs.specimenDetails.validate()) {
        alertSvc.error('There are validation errors in the specimens step. Please correct them.');
        return;
      }

      const toSave = JSON.parse(JSON.stringify(order));
      if (orderItems) {
        toSave.orderItems = JSON.parse(JSON.stringify(orderItems));
        toSave.orderItems.forEach(
          item => {
            item.status = item.dispose ? 'DISTRIBUTED_AND_CLOSED' : 'DISTRIBUTED';
            delete item.dispose;
          }
        );
      }

      if (status) {
        toSave.status = status;
      }

      const savedOrder = await orderSvc.saveOrUpdate(toSave);
      if (!toSave.id) {
        routerSvc.goto('OrderDetail.Overview', {orderId: savedOrder.id});
      } else {
        routerSvc.back();
      }

      return savedOrder;
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
