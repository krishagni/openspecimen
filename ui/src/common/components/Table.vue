<template>
  <span v-if="headerRow.length > 0 && bodyRows.length > 0">
    <table class="os-table muted-header os-border">
      <thead>
        <tr>
          <th v-for="(header, hdrIdx) of headerRow" :key="hdrIdx">
            <span>{{header}}</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, rowIdx) of bodyRows" :key="rowIdx">
          <td v-for="(col, colIdx) of row" :key="colIdx">
            <os-span v-model="bodyRows[rowIdx][colIdx]" :displayType="columnTypes[colIdx]"></os-span>
          </td>
        </tr>
      </tbody>
    </table>
  </span>
</template>

<script>
export default {
  props: ['modelValue'],

  created() {
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    tableObj: function() {
      if (!this.inputValue) {
        return {};
      } else if (typeof this.inputValue == 'string') {
        try {
          return JSON.parse(this.inputValue);
        } catch (e) {
          console.log('Input value ' + this.inputValue + ' is not a valid JSON');
        }
      } else if (typeof this.inputValue == 'object') {
        return this.inputValue;
      }

      return {};
    },

    headerRow: function() {
      if (this.tableObj.header instanceof Array) {
        return this.tableObj.header;
      }

      return [];
    },

    columnTypes: function() {
      if (this.tableObj.columnTypes instanceof Array) {
        return this.tableObj.columnTypes;
      }

      return [];
    },

    bodyRows: function() {
      if (this.tableObj.rows instanceof Array) {
        return this.tableObj.rows;
      }

      return [];
    }
  }
}
</script>
