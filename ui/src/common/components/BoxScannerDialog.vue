<template>
  <os-dialog ref="scannerDialog" :size="'lg'">
    <template #header>
      <span v-t="'common.scan_box'">Scan box</span>
    </template>
    <template #content>
      <div v-if="ctx.scanners && ctx.scanners.length > 0">
        <div class="row">
          <os-box-scanner ref="scanner" :scanners="ctx.scanners" :fetch-box-details="true"
            :fetch-specimen-details="searchSpecimens"
            @scan-started="onScanningStart" @scan-results="handleScanResults" />
        </div>

        <div class="results" v-if="scanCtx.results">
          <os-box-scan-results :scan-results="scanCtx" />
        </div>

        <div class="row" v-if="scanCtx.container">
          <box-layout class="map" :container="scanCtx.container" :occupants="scanCtx.tubes">
            <template #occupant_specimen="slotProps">
              <specimen-cell :specimen="slotProps.occupant" />
            </template>
          </box-layout>
        </div>
      </div>
      <div v-else-if="ctx.scanners && ctx.scanners.length == 0">
        <os-message type="danger">
          <span v-t="'common.no_box_scanners_configured'">No box scanners configured</span>
        </os-message>
      </div>
      <div v-else>
        <os-message type="info">
          <span v-t="'common.loading_box_scanners'">Loading box scanners configuration...</span>
        </os-message>
      </div>
    </template>
    <template #footer>
      <os-button text :label="$t('common.buttons.close')" @click="close" />

      <os-button secondary :label="$t('common.buttons.rescan')" @click="rescan"
        :disabled="!scanCtx.results" />

      <os-button primary :label="doneLabel || $t('common.buttons.done')" @click="done"
        :disabled="!scanCtx.specimens || scanCtx.specimens.length == 0" />
    </template>
  </os-dialog>
</template>

<script>
import specimenSvc from '@/biospecimen/services/Specimen.js';
import wfSvc       from '@/common/services/Workflow.js';

import BoxSpecimenCell from '@/administrative/containers/BoxSpecimenCell.vue';
import Layout          from '@/administrative/containers/Layout.vue';

export default {
  props: ['fetch-specimens', 'doneLabel'],

  emits: ['done'],

  components: {
    'box-layout': Layout,

    'specimen-cell': BoxSpecimenCell
  },

  data() {
    return {
      ctx: {
      },

      scanCtx: {
      }
    }
  },

  methods: {
    open: function() {
      if (!this.ctx.scanners) {
        this._loadScanners();
      }

      this.scanCtx = {};
      this.$refs.scannerDialog.open();
    },

    close: function() {
      this.scanCtx = {};
      this.$refs.scannerDialog.close();
    },

    rescan: function() {
      this.scanCtx = {};
      this.$refs.scanner.scan();
    },

    onScanningStart: function() {
      this.scanCtx = {scanning: true};
    },

    searchSpecimens: async function(filters) {
      filters = filters || {};
      if (typeof this.fetchSpecimens == 'function') {
        return this.fetchSpecimens(filters);
      } else {
        return specimenSvc.search(filters);
      }
    },

    handleScanResults: function({scanner, box, tubes, container, readErrorsCount, noTubesCount, barcodes, notFound}) {
      this.scanCtx = {
        scanning: false,
        results: true,
        scanner,
        noBoxBarcode: !box || !box.barcode,
        readErrorsCount,
        noTubesCount,
        container,
        tubes,
        scannedBarcodesCount: barcodes.length,
        notFound,
        specimens: (tubes || []).filter(tube => tube.specimen && tube.specimen.id > 0).map(tube => tube.specimen)
      };
    },

    done: function() {
      this.$emit('done', this.scanCtx);
    },

    _loadScanners: function() {
      wfSvc.getSysWorkflow('box-scanners').then(
        cfg => {
          const {scannedBoxField, scanners} = cfg || {};
          const options = this.ctx.scanners = (scanners || []).map((s, idx) => ({id: idx, ...s}));
          if (options.length == 1) {
            this.ctx.scanner = options[0];
          }

          this.ctx.scannedBoxField = scannedBoxField || 'barcode';
        }
      );
    }
  }
}
</script>
