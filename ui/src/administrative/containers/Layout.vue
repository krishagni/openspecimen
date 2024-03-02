<template>
  <div class="os-container-layout">
    <table>
      <tr v-for="(row, rowIdx) of locations" :key="rowIdx">
        <td v-for="(slot, colIdx) of row" :key="rowIdx + '_' + colIdx"
          :class="{'occupied': !!slot.occupied, 'selected': selectedPositions && selectedPositions[slot.position]}">
          <span class="coord" v-show="container.positionLabelingMode == 'TWO_D'">
            <span>({{slot.rowStr}}, {{slot.columnStr}})</span>
          </span>
          <span class="coord" v-show="container.positionLabelingMode == 'LINEAR'">
            <span>{{slot.position}}</span>
          </span>
          <span class="occupant-wrapper" v-if="slot.occupied">
            <slot :name="'occupant_' + (slot.occupied.occuypingEntity || 'empty')"
              :position="{row: slot.row, column: slot.column,
                rowStr: slot.rowStr, columnStr: slot.columnStr,
                position: slot.position}"
              :occupant="slot.occupied" />
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

import boxUtil from '@/common/services/BoxUtil.js';
import numConvUtil from '@/common/services/NumberConverterUtil.js';

export default {
  props: ['container', 'occupants', 'selectedPositions'],

  emits: ['occupant-clicked'],

  data() {
    return {
      ctx: {}
    }
  },

  computed: {
    locations: function() {
      return boxUtil.getMatrix({
        positionAssignerType: this.container.positionAssignment,
        rowLabelingScheme:    this.container.rowLabelingScheme,
        columnLabelingScheme: this.container.columnLabelingScheme,
        numberOfRows:         this.container.noOfRows,
        numberOfColumns:      this.container.noOfColumns,
        occupants:            this.occupants || [],
        rowProp:              'posTwoOrdinal',
        colProp:              'posOneOrdinal'
      });
    }
  },

  methods: {
    getSelectedPositions: function() {
      const result = [];
      if (!this.selectedPositions || Object.keys(this.selectedPositions).length == 0) {
        return result;
      }

      for (const row of this.locations) {
        for (const location of row) {
          if (this.selectedPositions[location.position]) {
            result.push(location);
          }
        }
      }

      return result;
    },

    assignPositions: function(occupants, labels, vacateOccupants = false) {
      return boxUtil.assignPositions({
        positionAssignerType: this.container.positionAssignment,
        rowLabelingScheme:    this.container.rowLabelingScheme,
        columnLabelingScheme: this.container.columnLabelingScheme,
        numberOfRows:         this.container.noOfRows,
        numberOfColumns:      this.container.noOfColumns,
        occupants:            occupants || [],
        rowProp:              'posTwoOrdinal',
        colProp:              'posOneOrdinal',
        isVacatable:          (occupant) => occupant.occuypingEntity == 'specimen',
        occupantName:         (occupant) => occupant.occupyingEntityName || occupant.blockedEntityName,
        createCell:           (label, row, column, oldOccupant) => (
                                {
                                  occuypingEntity: 'specimen',
                                  occupyingEntityName: label,
                                  posOne: numConvUtil.fromNumber(this.container.columnLabelingScheme, column),
                                  posTwo: numConvUtil.fromNumber(this.container.rowLabelingScheme, row),
                                  posTwoOrdinal: row,
                                  posOneOrdinal: column,
                                  oldOccupant: oldOccupant
                                }
                              )
      }, labels, vacateOccupants);
    }
  }
}
</script>

<style scoped>

.os-container-layout {
  height: 100%;
  overflow: auto;
}

.os-container-layout table {
  border-collapse: collapse;
}

.os-container-layout table td {
  height: 100px;
  min-height: 100px;
  max-height: 100px;
  width: 120px;
  min-width: 120px;
  max-width: 120px;
  border: 1px solid #ddd;
  position: relative;
}

.os-container-layout table td.occupied {
  background: #f5f5f5;
}

.os-container-layout table td.selected {
  background: #ffc;
}

.os-container-layout table td .coord {
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
  font-size: 85%;
  z-index: 10;
}

.os-container-layout table td .occupant-wrapper,
.os-container-layout table td .empty-wrapper {
  padding: 5px;
  width: 100%;
  display: inline-block;
}

.os-container-layout {
  flex: 1;
}

.os-container-layout :deep(.occupant) {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-decoration: none;
}

.os-container-layout :deep(.occupant .specimen-icon) {
  display: inline-block;
  font-size: 175%;
  height: 35px;
  width: 35px;
  border-radius: 50%;
  background: #3a87ad;
  color: #fff;
  text-align: center;
  z-index: 10;
}

.os-container-layout :deep(.occupant:hover .icon) {
  color: #23527c;
  color: #fff;
}

.os-container-layout :deep(.occupant .specimen-icon.not-found:after) {
  content: '\2573';
  display: block;
  color: red;
  font-size: 60px;
  margin-top: -55px;
  margin-left: -12px;
}

.os-container-layout :deep(.occupant .specimen-icon.read-error) {
  background: #a0a0a0;
  color: #fff;
}

.os-container-layout :deep(.occupant .specimen-icon.read-error:after) {
  content: '?';
  display: block;
  color: red;
  font-size: 60px;
  font-weight: 200;
  margin-top: -60px;
}

.os-container-layout :deep(.occupant .name) {
  width: 100%;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  border: #3a87ad;
  color: #666;
  border-radius: 3px;
  padding: 2px;
  display: block;
  text-align: center;
  margin-top: 2px;
  z-index: 10;
}

.os-container-layout :deep(.occupant:hover .name) {
  color: #333;
}
</style>
