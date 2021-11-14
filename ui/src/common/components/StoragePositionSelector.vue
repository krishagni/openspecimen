<template>
  <os-dialog ref="selectorDialog" size="lg">
    <template #header>
      <span v-if="entityType == 'storage_container'">Container Position Selector</span>
      <span v-else>Specimen Position Selector</span>
    </template>
    <template #content>
      <os-steps ref="selectorWizard">
        <os-step title="Select Container">
          <os-container-selector :entity-type="entityType" :criteria="criteria" :query="query"
            @container-selected="onContainerSelection($event)" />
        </os-step>

        <os-step title="Select Position">
          <span v-if="selectedContainer">
            <os-container-position-selector :container="selectedContainer"
              @position-selected="onPositionSelection($event)" />
          </span>
        </os-step>
      </os-steps>
    </template>
    <template #footer>
      <os-button label="Cancel" @click="close" />
      <os-button label="Previous" v-if="step == 'position_selection'" @click="previous" />
      <os-button label="Done" @click="done" />
    </template>
  </os-dialog>
</template>

<script>

import http from '@/common/services/HttpClient.js';

export default {
  props: ['entityType', 'criteria', 'query'],

  emits: ['position-selected'],

  data: function() {
    return {
      selectedContainer: undefined,

      step: 'container_selection'
    };
  },

  methods: {
    open: function() {
      this.$refs.selectorDialog.open();
    },

    close: function() {
      this.$refs.selectorDialog.close();
    },

    onContainerSelection: function({container}) {
      this.selectedContainer = container;
      if (!container) {
        return;
      }

      this.position = null;
      if (container.positionLabelingMode == 'NONE') {
        this.done();
      } else if (!container.occupiedPositions) {
        http.get('storage-containers/' + container.id).then(
          (dbContainer) => {
            Object.assign(container, dbContainer);
            this.next();
          }
        );
      } else {
        this.next();
      }
    },

    onPositionSelection: function({position}) {
      this.position = position; 
    },

    next: function() {
      this.$refs.selectorWizard.next();
      this.step = 'position_selection';
    },

    previous: function() {
      this.$refs.selectorWizard.previous();
      this.step = 'container_selection';
    },

    done: function() {
      if (this.selectedContainer) { 
        let position = { name: this.selectedContainer.name, mode: this.selectedContainer.positionLabelingMode };
        if (this.position) {
          Object.assign(position, this.position);
        }

        this.$emit('position-selected', { position });
      }

      this.$refs.selectorDialog.close();
    }
  },
}
</script>
