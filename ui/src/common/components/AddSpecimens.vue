
<template>
  <div class="os-add-specimens" :class="{'bottom-options': optionsAtBottom == true}">
    <div class="options">
      <os-boolean-checkbox name="useBarcode" v-model="useBarcode" v-if="barcodingEnabled && !useVisitNames">
        <label v-t="'common.add_specimens.use_barcode'">Use Specimen Barcode</label>
      </os-boolean-checkbox>
      <os-boolean-checkbox name="useVisitNames" v-model="useVisitNames" v-if="allowVisitNames && !useBarcode">
        <label v-t="'common.add_specimens.use_visit_names'">Use Visit Names</label>
      </os-boolean-checkbox>
      <slot name="options"></slot>
    </div>

    <div class="input-group">
      <os-textarea :placeholder="placeholder || label" v-model="inputValue"
        @update:modelValue="$emit('labels-scanned', $event)" />

      <span class="buttons" v-if="!hideButtons">
        <os-button primary :label="$t('common.buttons.add')" @click="addSpecimens" v-if="!hideAddButton" />
        <os-button primary :label="$t('common.buttons.scan')" @click="scanSpecimens" v-if="!hideScanButton" />
        <slot></slot>
      </span>
    </div>

    <os-dialog ref="resolveSpmns">
      <template #header>
        <span v-t="'common.add_specimens.multi_cp_specimens'">Specimens in multiple protocols</span>
      </template>

      <template #content>
        <span v-t="'common.add_specimens.select_specimen_cp'">
          Following specimens are present in multiple collection protocols. Select the right collection protocol before proceeding.
        </span>

        <table class="os-table">
          <thead>
            <tr>
              <th class="os-col-4">
                <span v-if="useBarcode">
                  <span v-t="'specimens.barcode'">Barcode</span>
                </span>
                <span v-else>
                  <span v-t="'specimens.label'">Label</span>
                </span>
              </th>
              <th class="os-col-8">
                <span v-t="'specimens.cp'">Collection Protocol</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(dupLabel, idx) of dupLabels" :key="idx">
              <td>{{dupLabel.label}}</td>
              <td>
                <os-dropdown v-model="dupLabel.selected" 
                  :list-source="{options: dupLabel.specimens, displayProp: 'cpShortTitle'}"  />
              </td>
            </tr>
          </tbody>
        </table>
      </template>

      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="closeResolver" />
        <os-button primary :label="$t('common.buttons.done')" @click="resolved" />
      </template>
    </os-dialog>

    <os-confirm class="os-not-found-confirm" ref="notFoundConfirm">
      <template #title>
        <span v-t="'common.add_specimens.not_found'">Specimens not found</span>
      </template>

      <template #message>
        <div class="message">
          <div v-t="'common.add_specimens.not_found_msg'">Following specimens were not found: </div>

          <div><i>{{notFoundLabels.join(', ')}}</i></div>

          <div v-t="'common.add_specimens.proceed_q'">Do you want to proceed?</div>
        </div>
      </template>
    </os-confirm>

    <os-box-scanner-dialog ref="boxScannerDialog" :fetch-barcodes="true"
      :hide-done="hideAddButton" :done-label="$t('common.buttons.add')"
      @barcodes="addBarcodes" @done="addBoxSpecimens" />
  </div>
</template>

<script>

import http   from '@/common/services/HttpClient.js';
import alerts from '@/common/services/Alerts.js';
import util   from '@/common/services/Util.js';

export default {

  props: [
    'criteria',
    'errorOpts',
    'placeholder',
    'label',
    'optionsAtBottom',
    'hideButtons',
    'hideAddButton',
    'hideScanButton',
    'modelValue',
    'allowVisitNames',
    'scanProp'
  ],

  emits: ['on-add', 'labels-scanned'],

  data() {
    return {
      useBarcode: false,

      useVisitNames: false,

      dupLabels: [],

      inputValue: '',

      barcodingEnabled: false,

      notFoundLabels: []
    }
  },

  async created() {
    this.barcodingEnabled = await http.get('collection-protocols/barcoding-enabled');
    if (this.scanProp == 'specimen-barcodes' && this.barcodingEnabled) {
      this.useBarcode = true;
      this.useVisitNames = false;
    } else if (this.scanProp == 'visit-names' && this.allowVisitNames) {
      this.useBarcode = false;
      this.useVisitNames = true;
    } else {
      this.useBarcode = this.useVisitNames = false;
    }
  },

  methods: {
    getLabels: function() {
      return this.inputValue.split(/,|\t|\n/)
        .map(function(label) { return label.trim(); })
        .filter(function(label) { return label.length != 0; });
    },

    addSpecimens: async function() {
      const {specimens, useBarcode, error} = await this.getSpecimens();
      if (error) {
        return {error, specimens: []};
      }

      if (specimens && specimens.length > 0) {
        this.clearInput();
        this.$emit('on-add', {specimens, useBarcode});
      } else if (specimens) {
        const opts = this.errorOpts || {};
        alerts.error(opts.no_match || {code: 'common.add_specimens.no_matching_specimens'});
      }

      return {error, specimens};
    },

    scanSpecimens: function() {
      this.$refs.boxScannerDialog.open();
    },

    addBarcodes: function(barcodes) {
      this.inputValue = barcodes;
      this.useBarcode = true;
      this.$emit('labels-scanned', barcodes);
      this.$refs.boxScannerDialog.close();
    },

    addBoxSpecimens: function({specimens}) {
      this.$emit('on-add', {specimens, useBarcode: true});
      this.$refs.boxScannerDialog.close();
    },

    setInput: function(text) {
      this.inputValue = text;
    },

    clearInput: function() {
      this.inputValue = '';
      // this.input = '';
    },

    getSpecimens: async function() {
      const labels = util.splitStr(this.inputValue, /,|\t|\n/, false);
      if (labels.length == 0) {
        return {specimens: [], useBarcode: this.useBarcode, useVisitNames: this.useVisitNames};
      }

      return this.getSpecimensByLabel(labels);
    },

    getSpecimensByLabel: async function(labels) {
      labels = labels || [];
      if (labels.length == 0) {
        return {specimens: [], useBarcode: this.useBarcode, useVisitNames: this.useVisitNames};
      }

      const dupLabels = util.getDupItems(labels);
      if (dupLabels.length > 0) {
        alerts.error({code: 'common.add_specimens.dup_labels', args: {labels: dupLabels.join(', ')}});
        return {specimens: [], useBarcode: this.useBarcode, useVisitNames: this.useVisitNames, error: true};
      }

      let searchReq = Object.assign({exactMatch: true}, this.criteria || {});
      if (this.useVisitNames) {
        searchReq.visitNames = labels;
      } else if (this.useBarcode) {
        searchReq.barcodes = labels;
      } else {
        searchReq.labels = labels;
      }

      searchReq.maxResults = 1000;
      return http.post(searchReq.url || 'specimens/search', searchReq).then(
        specimens => this.resolveSpecimens(searchReq.labels, searchReq.barcodes, specimens).then(
          resolvedSpmns => ({...resolvedSpmns, useBarcode: this.useBarcode})
        )
      );
    },

    resolveSpecimens: async function(labels, barcodes, specimens) {
      let inputs, attr;
      if (!!labels && labels.length > 0) {
        inputs = labels;
        attr = 'label';
      } else if (!!barcodes && barcodes.length > 0) {
        inputs = barcodes;
        attr = 'barcode';
      } else {
        return { specimens };
      }

      return this.resolveSpecimens1(inputs, attr, specimens);
    },

    resolveSpecimens1: async function(labels, attr, specimens) {
      let specimensMap = {};
      specimens.forEach(
        (spmn) => {
          let key = spmn[attr] && spmn[attr].toLowerCase();
          specimensMap[key] = specimensMap[key] || [];
          specimensMap[key].push(spmn);
        }
      );

      //
      // {label: label/barcode, specimens; [s1, s2], selected: s1}
      //
      var labelsInfo = [];
      var dupLabels = [], notFoundLabels = [];

      labels.forEach(
        (label) => {
          let labelInfo = {label: label, selected: undefined};
          let spmns = specimensMap[label.toLowerCase()];
          if (!spmns || spmns.length == 0) {
            notFoundLabels.push(label);
            return;
          }

          labelInfo.specimens = spmns;
          if (spmns.length > 1) {
            dupLabels.push(labelInfo);
          } else {
            labelInfo.selected = spmns[0];
          }

          labelsInfo.push(labelInfo);
        }
      );

      if (notFoundLabels.length != 0) {
        const resp = await this.showError(notFoundLabels);
        if (resp != 'proceed') {
          return {specimens, error: true};
        }
      }

      if (dupLabels.length == 0) {
        return { specimens };
      }

      this.labelsInfo = labelsInfo;
      this.dupLabels  = dupLabels;
      this.$refs.resolveSpmns.open();
      return new Promise((resolve) => this.specimensResolver = resolve);
    },

    showError: function(notFoundLabels) {
      this.notFoundLabels = notFoundLabels;
      return this.$refs.notFoundConfirm.open();
    },

    resolved: function() {
      let unresolved = this.labelsInfo
        .filter(labelInfo => !labelInfo.selected)
        .map(labelInfo => labelInfo.label);
      if (unresolved.length > 0) {
        alerts.error({code: 'common.add_specimens.unresolved_specimens', args: {labels: unresolved.join(', ')}});
        return;
      }

      this.specimensResolver({specimens: this.labelsInfo.map(labelInfo => labelInfo.selected)});
      this.closeResolver();
    },

    closeResolver: function() {
      this.$refs.resolveSpmns.close();
    }
  }
}

</script>

<style scoped>

.os-add-specimens {
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
}

.os-add-specimens.bottom-options {
  flex-direction: column-reverse;
}

.os-add-specimens .input-group {
  display: flex;
}

/* Make the text area consume as much free space available */
.os-add-specimens .input-group :deep(.os-input-text) {
  flex: 1;
}

.os-add-specimens .input-group :deep(.os-input-text textarea) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.os-add-specimens .input-group :deep(.os-input-text textarea:focus) {
  border-color: #ced4da;
}

.os-add-specimens .input-group .buttons {
  height: 58px;
}

.os-add-specimens .input-group .buttons :deep(button) {
  height: 100%;
  border-radius: 0;
  border-left: 0;
}

.os-add-specimens .input-group .buttons :deep(button:last-child) {
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}

.os-add-specimens .options {
  display: flex;
  flex-direction: row;
}

.os-add-specimens .options > div {
  margin-right: 1.25rem;
}

.os-not-found-confirm .message > div {
  padding: 0.5rem 0.25rem;
}
</style>
