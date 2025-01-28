<template>
  <div class="os-box-layout">
    <table>
      <tr v-for="(row, rowIdx) of locations" :key="rowIdx">
        <td v-for="(slot, colIdx) of row" :key="rowIdx + '_' + colIdx"
          :class="{'occupied': !!slot.occupied, 'selected': selectedPositions[slot.position],
            'focussed': focussedOccupant && focussedOccupant.position == slot.position}">
          <span class="selection" v-if="allowSelections">
            <os-boolean-checkbox v-model="selectedPositions[slot.position]"
              @update:modelValue="onToggleSelection" />
          </span>
          <span class="coord" v-if="!boxSpec.labelingMode || boxSpec.labelingMode == 'TWO_D'">
            <span>({{slot.rowStr}}, {{slot.columnStr}})</span>
          </span>
          <span class="coord" v-else-if="boxSpec.labelingMode == 'LINEAR'">
            <span>{{slot.position}}</span>
          </span>
          <span class="occupant-wrapper" v-if="slot.occupied">
            <slot name="occupant" :position="slot" :occupant="slot.occupied" />
          </span>
          <span class="empty-wrapper" v-else>
            <slot name="empty" :position="{...slot}" />
          </span>
        </td>
      </tr>
    </table>
  </div>
</template>

<script>
export default {
  props: [ 'boxSpec', 'occupants', 'allowSelections'],

  emits: ['selected-positions'],

  data() {
    return {
      selectedPositions: {}
    }
  },

  computed: {
    locations: function() {
      const boxSpec = this.boxSpec || {};
      const opts = {...boxSpec, occupants: this.occupants};
      return this.$osSvc.boxUtil.getMatrix(opts);
    }
  },

  methods: {
    getSelectedPositions: function() {
      const result = [];
      for (let row of this.locations) {
        for (let slot of row) {
          if (this.selectedPositions[slot.position]) {
            result.push(slot.occupied);
          }
        }
      }

      return result;
    },

    clearSelectedPositions: function() {
      this.selectedPositions = {};
    },

    onToggleSelection: function() {
      this.$emit('selected-positions', this.getSelectedPositions());
    }
  }
}
</script>

<style scoped>
.os-box-layout {
  height: 100%;
  overflow: auto;
}

.os-box-layout table {
  border-collapse: collapse;
}

.os-box-layout table td {
  height: 100px;
  min-height: 100px;
  max-height: 100px;
  width: 120px;
  min-width: 120px;
  max-width: 120px;
  border: 1px solid #ddd;
  position: relative;
}

.os-box-layout table td.occupied {
  background: #f5f5f5;
}

.os-box-layout table td.selected {
  background: #ffc;
}

.os-box-layout table td.focussed {
  background: #d5d5d5;
}

.os-box-layout table td .coord {
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
  font-size: 85%;
  z-index: 10;
}

.os-box-layout table td .selection {
  position: absolute;
  top: 0.25rem;
  left: 0.25rem;
  font-size: 85%;
  z-index: 10;
}

.os-box-layout table td .occupant-wrapper,
.os-box-layout table td .empty-wrapper {
  padding: 5px;
  width: 100%;
  display: inline-block;
}

.os-box-layout :deep(.occupant) {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-decoration: none;
}
</style>
