<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-t="'containers.archive_containers'">Archive Containers</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div>
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
            <os-button primary :label="$t('common.buttons.archive')" @click="archive" />

            <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
          </div>
        </div>
      </div>

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

import alertsSvc    from '@/common/services/Alerts.js';
import containerSvc from '@/administrative/services/Container.js';
import routerSvc    from '@/common/services/Router.js';

export default {
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

      listSchema
    };
  },

  methods: {
    addContainers: function({itemLabels}) {
      if (itemLabels.length == 0) {
        return;
      }

      const filterOpts = {status: ['AVAILABLE', 'CHECKED_OUT']};
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
      const idx = this.ctx.containers.findIndex(ro => ro.container.id == container.id);
      if (idx == -1) {
        return;
      }

      this.ctx.containers.splice(idx, 1);
    },

    archive: function() {
      if (this.ctx.containers.length == 0) {
        alertsSvc.error({code: 'containers.add_atleast_one'});
        return;
      }

      const containers = this.ctx.containers.map(
        ({container}) => ({id: container.id, activityStatus: 'Closed', transferComments: 'Archived'})
      );

      containerSvc.bulkUpdate(containers).then(
        (result) => {
          if (result) {
            alertsSvc.success({code: 'containers.archived', args: {count: result.length}});
          } else {
            alertsSvc.info({code: 'containers.archive_pending'});
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
