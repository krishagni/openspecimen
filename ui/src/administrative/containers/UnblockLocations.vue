<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-t="'containers.unblock_locations'">Unblock Locations</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div>
        <os-add-specimens :placeholder="$t('containers.scan_specimens')"
          :criteria="{includeExtensions: true}" @on-add="addSpecimens" />

        <os-list-view style="height: auto;"
          :data="ctx.specimens"
          :schema="specimensListSchema"
          :allowSelection="false"
          :loading="false"
          :showRowActions="true"
          ref="listView">
          <template #rowActions="slotProps">
            <os-button left-icon="times" @click="removeSpecimen(slotProps.rowObject)" />
          </template>
        </os-list-view>

        <div v-if="ctx.specimens.length > 0">
          <os-divider />

          <div class="os-form-footer">
            <os-button primary :label="$t('containers.unblock')" @click="unblock" />

            <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
          </div>
        </div>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import containerSvc from '@/administrative/services/Container.js';

import specimensListSchema from '@/administrative/schemas/containers/specimens-list.js';

export default {
  data() {
    const bcrumb = [
      {url: routerSvc.getUrl('ContainersList', {}), label: this.$t('containers.list')}
    ];

    return {
      ctx: {
        bcrumb,

        specimens: []
      },

      specimensListSchema
    };
  },

  methods: {
    addSpecimens: function({specimens, useBarcode}) {
      const errorSpmns = [];
      for (let specimen of specimens) {
        if (specimen.checkoutPosition && specimen.checkoutPosition.id > 0) {
          this.ctx.specimens.push({specimen});
        } else {
          errorSpmns.push(useBarcode ? specimen.barcode : specimen.label);
        }
      }

      if (errorSpmns.length > 0) {
        alertsSvc.error({code: 'containers.not_checked_out', args: {labels: errorSpmns.join(', ')}});
      }
    },

    removeSpecimen: function({specimen}) {
      const idx = this.ctx.specimens.findIndex((item) => item.specimen.id == specimen.id);
      if (idx > -1) {
        this.ctx.specimens.splice(idx, 1);
      }
    },

    unblock: function() {
      const specimens = this.ctx.specimens.map(({specimen}) => ({id: specimen.id}));
      return containerSvc.unblockCheckoutLocations(specimens).then(
        ({count}) => {
          alertsSvc.success({code: 'containers.n_pos_unblocked', args: {count}});
          this.$goto('ContainersList');
        }
      );
    }
  }
}
</script>
