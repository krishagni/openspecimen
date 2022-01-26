
<template>
  <div>
    <div v-if="barcodingEnabled">
      <os-boolean-checkbox name="useBarcode" v-model="useBarcode">
        <label>Use Specimen Barcode</label>
      </os-boolean-checkbox>
    </div>

    <div class="os-add-specimens">
      <os-textarea :placeholder="label" v-model="input" />

      <span class="buttons">
        <os-button primary label="Add" @click="addSpecimens" />
        <slot></slot>
      </span>
    </div>

    <os-dialog ref="resolveSpmns">
      <template #header>
        <span>Specimens in multiple protocols</span>
      </template>

      <template #content>
        <span>
          Following specimens are present in multiple collection protocols. Select the right collection protocol before proceeding.
        </span>

        <table class="os-table">
          <thead>
            <tr>
              <th class="os-col-4">
                <span v-if="useBarcode">Barcode</span>
                <span v-else>Label</span>
              </th>
              <th class="os-col-8">
                <span>Collection Protocol</span>
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
        <os-button text label="Cancel" @click="closeResolver" />
        <os-button primary label="Done" @click="resolved" />
      </template>
    </os-dialog>
  </div>
</template>

<script>

import http   from '@/common/services/HttpClient.js';
import alerts from '@/common/services/Alerts.js';

export default {

  props: ['criteria', 'errorOpts', 'label'],

  emits: ['on-add'],

  data() {
    return {
      useBarcode: false,

      input: '',

      dupLabels: [],

      barcodingEnabled: false,
    }
  },

  async created() {
    this.barcodingEnabled = await http.get('collection-protocols/barcoding-enabled');
  },

  methods: {
    getLabels: function() {
      return this.input.split(/,|\t|\n/)
        .map(function(label) { return label.trim(); })
        .filter(function(label) { return label.length != 0; });
    },

    addSpecimens: function() {
      let labels = this.getLabels();
      if (labels.length == 0) {
        return;
      }

      let searchReq = Object.assign({exactMatch: true}, this.criteria || {});
      if (this.useBarcode) {
        searchReq.barcodes = labels;
      } else {
        searchReq.labels = labels;
      }

      searchReq.maxResults = 1000;
      http.post('specimens/search', searchReq).then(
        specimens => {
          this.resolveSpecimens(searchReq.labels, searchReq.barcodes, specimens).then(
            resolvedSpmns => {
              if (resolvedSpmns && resolvedSpmns.length > 0) {
                this.input = '';
                this.$emit('on-add', {specimens: resolvedSpmns, useBarcode: this.useBarcode});
              } else if (resolvedSpmns) {
                const opts = this.errorOpts || {};
                alerts.error(opts.no_match || 'No specimens match the input criteria.');
              }
            }
          );
        }
      );
    },

    resolveSpecimens: async function(labels, barcodes, specimens) {
      let inputs, attr;
      if (!!labels && labels.length > 0) {
        inputs = labels;
        attr = 'label';
      } else {
        inputs = barcodes;
        attr = 'barcode';
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
        this.showError(notFoundLabels);
        if (!specimens || specimens.length == 0) {
          return undefined;
        }
      }

      if (dupLabels.length == 0) {
        return specimens;
      }

      this.labelsInfo = labelsInfo;
      this.dupLabels  = dupLabels;
      this.$refs.resolveSpmns.open();
      return new Promise((resolve) => this.specimensResolver = resolve);
    },

    showError: function(notFoundLabels) {
      let opts = this.errorOpts || {};
      let msg = '';

      if (notFoundLabels.length > 1) {
        msg = opts.m_not_found || 'Specimens not found';
      } else {
        msg = opts.not_found || 'Specimen not found';
      }

      alerts.error(msg + ': ' + notFoundLabels.join(', '));
    },

    resolved: function() {
      let unresolved = this.labelsInfo
        .filter(labelInfo => !labelInfo.selected)
        .map(labelInfo => labelInfo.label);
      if (unresolved.length > 0) {
        alerts.error('One or more specimens not resolved: ' + unresolved.join(', '));
        return;
      }

      this.specimensResolver(this.labelsInfo.map(labelInfo => labelInfo.selected));
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
  display: flex;
  margin-bottom: 1.25rem;
}

/* Make the text area consume as much free space available */
.os-add-specimens :deep(.os-input-text) {
  flex: 1;
}

.os-add-specimens :deep(.os-input-text textarea) {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.os-add-specimens :deep(.os-input-text textarea:focus) {
  border-color: #ced4da;
}

.os-add-specimens .buttons :deep(button) {
  height: calc(100% - 1.75px);
  border-radius: 0;
}

.os-add-specimens .buttons :deep(button) {
  border-left: 0;
}

.os-add-specimens .buttons :deep(button:last-child) {
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}

</style>
