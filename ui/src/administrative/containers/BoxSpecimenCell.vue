<template>
  <a class="occupant" :href="specimenUrl" @click="showOccupantDetails($event, specimen)">
    <os-icon class="specimen-icon"
      :class="{
        'read-error': specimen.barcode == 'READ_ERROR',
        'not-found': specimen.barcode != 'READ_ERROR' && !specimen.id
      }"
      name="vial"
      :style="specimen.colorCode"
      v-os-tooltip="specimen.tooltip" />

    <span class="name" v-os-tooltip="specimen.barcode">
      <span>{{specimen.barcode}}</span>
    </span>
  </a>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['specimen'],

  emits: ['occupant-clicked'],

  computed: {
    specimenUrl: function() {
      return this.specimen && this.specimen.id ? routerSvc.getUrl('SpecimenResolver', {specimenId: this.specimen.id}) : null;
    }
  },

  methods: {
    showOccupantDetails: function(event, occupant) {
      if (this.specimenUrl) {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.button != 0) {
          return;
        }

        event.preventDefault();
      }

      this.$emit('occupant-clicked', {event, occupant});
    }
  }
}
</script>
