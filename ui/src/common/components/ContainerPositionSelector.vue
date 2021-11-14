<template>
  <div>
    <table class="os-container-positions">
      <tbody @click="selectPosition($event)">
        <tr v-for="(row, ridx) of matrix" :key="ridx">
          <td v-for="(slot, cidx) of row" :key="ridx + '_' + cidx"
            :position-y="slot.rowStr" :position-x="slot.columnStr" :position="slot.position" 
            :class="slot.occupied ?  'occupied' : (slot.rowStr == selectedPosition.positionY &&
              slot.columnStr == selectedPosition.positionX ? 'selected' : '')"
          >
            <span class="coord" v-show="container.positionLabelingMode == 'TWO_D'">
              <span>({{slot.rowStr}}, {{slot.columnStr}})</span>
            </span>
            <span class="coord" v-show="container.positionLabelingMode == 'LINEAR'">
              <span>{{slot.position}}</span>
            </span>
            <span class="ball"></span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>

import boxUtil from '@/common/services/BoxUtil.js';

export default {
  props: ['container'],

  emits: ['position-selected'],

  data: function() {
    return {
      selectedPosition: {}
    }
  },

  computed: {
    matrix: function() {
      return boxUtil.getMatrix({
        positionAssignerType: this.container.positionAssignment,
        rowLabelingScheme:    this.container.rowLabelingScheme,
        columnLabelingScheme: this.container.columnLabelingScheme,
        numberOfRows:         this.container.noOfRows,
        numberOfColumns:      this.container.noOfColumns,
        occupiedPositions:    this.container.occupiedPositions || []
      });
    }
  },

  methods: {
    selectPosition: function(event) {
      let tgt = event.target;
      while (tgt) {
        if (tgt.tagName.toUpperCase() == 'TABLE') {
          break;
        } else if (tgt.tagName.toUpperCase() == 'TD') {
          if (typeof tgt.classList.contains != 'function' || !tgt.classList.contains('occupied')) {
            this.selectedPosition = {
              positionY: tgt.getAttribute('position-y'),
              positionX: tgt.getAttribute('position-x'),
              position:  tgt.getAttribute('position')
            };
            this.$emit('position-selected', {position: this.selectedPosition});
          }

          break;
        }

        tgt = tgt.parentNode;
      }
    }
  }
}

</script>

<style>

.os-container-positions {
  border-collapse: collapse;
  border-spacing: 0;
  border: 1px solid #ddd;
  margin-bottom: 20px;
  width: 100%;
}

.os-container-positions td {
  border: 1px solid #ddd;
  padding: 0.5rem;
  min-width: 50px;
  height: 50px;
  text-align: center;
  position: relative;
  cursor: pointer;
}

.os-container-positions td.occupied,
.os-container-positions td.selected {
  cursor: default;
}

.os-container-positions td .coord {
  position: absolute;
  top: 0.125rem;
  left: 0.125rem;
  font-size: 60%;
}

.os-container-positions td .ball {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-top: 0.125rem;
  border-radius: 50%;
  background: #a0a0a0;
}

.os-container-positions td.occupied .ball {
  background: #5cb85c;
}

.os-container-positions td.selected {
  background: lightgray;
}
</style>
