<template>
  <os-section>
    <template #title>
      <span>{{title}}</span>
    </template>
    <template #content>
      <os-table-span v-model="results" />
    </template>
  </os-section>
</template>

<script>
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['title', 'context', 'aql', 'params'],

  data() {
    return {
      results: {}
    }
  },

  mounted() {
    this._executeQuery();
  },

  computed: {
    instantiatedParams: function() {
      return util.getInstantiatedParams(this.context || {}, this.params);
    },

    instantiatedAql: function() {
      let aql = this.aql;

      const params = this.instantiatedParams;
      for (const name of Object.keys(params)) {
        let value = params[name];
        if (typeof value == 'string') {
          value = '"' + value + '"';
        } else if (value instanceof Array) {
          const array = [];
          for (const ele of value) {
            if (typeof ele == 'string') {
              array.push('"' + ele + '"');
            } else {
              array.push(ele);
            }
          }

          value = array.join(',');
        }

        aql = aql.replaceAll(':' + name, value);
      }
 
      return aql;
    }
  },

  watch: {
    instantiatedAql: function() {
      this._executeQuery();
    }
  },

  methods: {
    _executeQuery: function() {
      const params = this.instantiatedParams;
      const payload = {
        cpId: params.cpId,
        cpGroupId: params.cpGroupId,
        drivingForm: params.drivingForm || 'Participant',
        runType: 'Data',
        aql: this.instantiatedAql,
        wideRowMode: params.wideRowMode || 'OFF',
        outputIsoDateTime: true, // TODO: (outputIsoFmt || false),
        caseSensitive: (params.caseSensitive == undefined || params.caseSensitive == null || params.caseSensitive)
      }

      return http.post('query', payload).then(
        results => {
          const header = [];
          for (const column of results.columnLabels) {
            const idx = column.lastIndexOf('#');
            header.push(column.substring(idx + 1).trim());
          }

          this.results = {header, rows: results.rows};
        }
      );
    }
  }
}
</script>
