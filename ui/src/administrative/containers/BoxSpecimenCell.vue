<template>
  <a class="occupant" @click="showOccupantDetails($event, specimen)">
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
export default {
  props: ['specimen'],

  emits: ['occupant-clicked'],

  methods: {
    showOccupantDetails: function(event, occupant) {
      this.$emit('occupant-clicked', {event, occupant});
    }
  }
}
</script>
