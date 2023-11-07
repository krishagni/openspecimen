<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-t="'containers.checkout_containers'" v-if="checkout">Checkout Containers</h3>
        <h3 v-t="'containers.checkin_containers'" v-else-if="checkin">Checkin Containers</h3>
        <h3 v-t="'containers.transfer_containers'" v-else>Transfer Containers</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-steps ref="transferWizard">
        <os-step :title="$t('containers.list')">
          <div>
            <os-add-items ref="addItems" :placeholder="$t('containers.scan_names_or_barcodes')"
              @on-add="addContainers($event)" style="margin-bottom: 10px;" />

            <os-boolean-checkbox name="useBarcode" v-model="ctx.useBarcode">
              <label v-t="'containers.use_barcode'">Use Barcode</label>
            </os-boolean-checkbox>

            <os-list-view
              :data="ctx.containers"
              :schema="listSchema"
              :allowSelection="false"
              :loading="false"
              :showRowActions="true"
              ref="listView">
              <template #rowActions="slotProps">
                <os-button left-icon="times" @click="removeContainer(slotProps.rowObject)" />
              </template>
            </os-list-view>

            <os-divider />

            <div class="os-form-footer">
              <os-button primary :label="$t('common.buttons.next')" @click="next" />

              <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
            </div>
          </div>
        </os-step>

        <os-step :title="checkin ? $t('containers.check_in_button') : checkout ?
          $t('containers.check_out_button') : $t('containers.transfer')">
          <div>
            <os-table-form ref="transferForm" :schema="transferSchema"
              :data="txCtx" :items="txCtx.containers"
              :remove-items="true" @remove-item="removeContainer($event.item)"
              @input="handleContainerInput($event)">
            </os-table-form>

            <os-divider />

            <div class="os-form-footer">
              <os-button secondary :label="$t('common.buttons.previous')" @click="previous" />

              <os-button primary :label="$t('common.buttons.submit')" @click="submit" />

              <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
            </div>
          </div>
        </os-step>
      </os-steps>

      <os-confirm class="os-not-found-confirm" ref="notFoundConfirm">
        <template #title>
          <span v-t="'containers.add_containers.not_found'">Containers not found</span>
        </template>

        <template #message>
          <div class="message">
            <div v-t="'containers.add_containers.not_found_msg'">Following containers were not found: </div>

            <div><i>{{ctx.notFound.join(', ')}}</i></div>

            <div v-t="'containers.add_containers.proceed_q'">Do you want to proceed?</div>
          </div>
        </template>
      </os-confirm>
    </os-page-body>
  </os-page>
</template>

<script>
import listSchema     from '@/administrative/schemas/containers/boxes-list.js';
import transferSchema from '@/administrative/schemas/containers/bulk-transfer.js';

import alertsSvc    from '@/common/services/Alerts.js';
import containerSvc from '@/administrative/services/Container.js';
import routerSvc    from '@/common/services/Router.js';
import util         from '@/common/services/Util.js';

export default {
  props: ['checkin', 'checkout'],

  data() {
    const bcrumb = [
      {url: routerSvc.getUrl('ContainersList', {}), label: 'Containers'}
    ];

    return {
      ctx: {
        bcrumb: bcrumb,

        useBarcode: false,

        containers: [],

        notFound: []
      },

      txCtx: {
        containers: []
      },

      listSchema,

      transferSchema
    };
  },

  methods: {
    addContainers: function({itemLabels}) {
      if (itemLabels.length == 0) {
        return;
      }

      const filterOpts = {};
      if (this.checkin) {
        filterOpts.status = ['CHECKED_OUT'];
      }

      if (this.ctx.useBarcode) {
        filterOpts.barcode = itemLabels;
      } else {
        filterOpts.naam = itemLabels;
      }

      this.ctx.notFound = [];
      containerSvc.getContainers(filterOpts).then(
        async (containers) => {
          itemLabels = itemLabels.map(i => i.toLowerCase());

          let idxFn, cmpFn;
          if (this.ctx.useBarcode) {
            idxFn = (c) => itemLabels.indexOf(c.barcode.toLowerCase());
            cmpFn = (c, barcode) => barcode == c.barcode.toLowerCase();
          } else {
            idxFn = (c) => itemLabels.indexOf(c.name.toLowerCase());
            cmpFn = (c, label) => label == c.name.toLowerCase();
          }

          containers.sort((c1, c2) => idxFn(c1) - idxFn(c2));

          const notFound = [];
          for (let label of itemLabels) {
            let found = false;
            for (let container of containers) {
              if (cmpFn(container, label)) {
                found = true;
                break;
              }
            }

            if (!found) {
              notFound.push(label);
            }
          }

          if (notFound.length > 0) {
            this.ctx.notFound = notFound;
            const resp = await this.$refs.notFoundConfirm.open();
            if (resp != 'proceed') {
              return;
            }
          }

          for (let container of containers) {
            if (containers.some(c => c.id == container)) {
              continue;
            }

            this.ctx.containers.push({container});
          }

          this.$refs.addItems.clearInput();
        }
      );
    },

    removeContainer: function({container}) {
      let idx = this.ctx.containers.findIndex(ro => ro.container.id == container.id);
      if (idx == -1) {
        return;
      }

      this.ctx.containers.splice(idx, 1);
      if (this.txCtx.containers && this.txCtx.containers.length > 0) {
        let idx = this.txCtx.containers.findIndex(ro => ro.container.id == container.id);
        if (idx >= 0) {
          this.txCtx.containers.splice(idx, 1);
          if (this.txCtx.containers.length == 0) {
            this.previous();
          }
        }
      }
    },

    handleContainerInput: function({field, item}) {
      if (field.name == 'container.siteName') {
        item.container.storageLocation = {};
      }
    },

    next: function() {
      if (this.ctx.containers.length == 0) {
        alertsSvc.error({code: 'containers.add_atleast_one'});
        return;
      }

      const transferredBy = this.$ui.currentUser;
      const transferDate  = new Date().getTime();
      this.txCtx.checkin  = this.checkin;
      this.txCtx.checkout = this.checkout;
      this.txCtx.containers = this.ctx.containers.map(
        ({container}) => {
          const copy = container.trCtx = container.trCtx || util.clone(container);
          copy.storageLocation = this.checkin ? container.blockedLocation : {};
          copy.transferredBy = transferredBy;
          copy.transferDate = transferDate;
          return {container: copy};
        }
      );
          
      this.$refs.transferWizard.next();
    },

    previous: function() {
      this.$refs.transferWizard.previous();
    },

    submit: function() {
      if (!this.$refs.transferForm.validate()) {
        return;
      }

      const containers = this.txCtx.containers.map(
        ({container}) => {
          return {
            id: container.id,
            siteName: container.siteName,
            storageLocation: container.storageLocation,
            transferredBy: container.transferredBy,
            transferDate: container.transferDate,
            transferComments: container.transferComments,
            status: null,
            checkOut: this.checkout
          }
        }
      );
      containerSvc.bulkUpdate(containers).then(
        (result) => {
          let success = 'containers.transferred';
          let pending = 'containers.transfer_pending';
          if (this.checkin) {
            success = 'containers.checked_in';
            pending = 'containers.checkin_pending';
          } else if (this.checkout) {
            success = 'containers.checked_out';
            pending = 'containers.checkout_pending';
          }

          if (result) {
            alertsSvc.success({code: success, args: {count: result.length}});
          } else {
            alertsSvc.info({code: pending});
          }

          this.cancel();
        }
      );
    },

    cancel: function() {
      this.$goto('ContainersList');
    }
  }
}
</script>

<style scoped>
.os-not-found-confirm .message > div {
  padding: 0.5rem 0.25rem;
}
</style>
