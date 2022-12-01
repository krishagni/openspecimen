<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-t="'containers.transfer_containers'">Transfer Containers</h3>
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

        <os-step :title="$t('containers.transfer')">
          <div>
            <os-table-form ref="transferForm" :schema="transferSchema"
              :data="txCtx" :items="txCtx.containers"
              :remove-items="true" @remove-item="removeContainer($event)"
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
  data() {
    const bcrumb = [
      {url: routerSvc.getUrl('ContainersList', {}), label: 'Containers'}
    ];

    return {
      ctx: {
        bcrumb: bcrumb,

        useBarcode: false,

        containers: []
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
      if (this.ctx.useBarcode) {
        filterOpts.barcode = itemLabels;
      } else {
        filterOpts.naam = itemLabels;
      }

      containerSvc.getContainers(filterOpts).then(
        (containers) => {
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
      const idx = this.ctx.containers.findIndex(ro => ro.container.id == container.id);
      if (idx == -1) {
        return;
      }

      this.ctx.containers.splice(idx, 1);
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
      this.txCtx.containers = this.ctx.containers.map(
        ({container}) => {
          const copy = container.trCtx = container.trCtx || util.clone(container);
          copy.storageLocation = {};
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
            transferComments: container.transferComments
          }
        }
      );
      containerSvc.bulkUpdate(containers).then(
        (result) => {
          if (result) {
            alertsSvc.success({code: 'containers.transferred', args: {count: result.length}});
          } else {
            alertsSvc.info({code: 'containers.transfer_pending'});
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
