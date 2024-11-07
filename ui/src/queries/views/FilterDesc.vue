<template>
  <span class="filter-description">
    <span v-if="desc">{{desc}}</span>
    <span v-else>
      <span class="field">{{field}}</span>
      <span class="op" v-if="op">&nbsp;{{op}}</span>
      <span class="value" v-if="value">&nbsp;{{value}}</span>
      <span class="value" v-else-if="subQuery">
        <span>&nbsp;</span>
        <router-link :to="{name: 'QueryDetail.AddEdit', params: {queryId: subQuery.id}}" target="_blank">
          <span>{{subQuery.title}}</span>
        </router-link>
      </span>
    </span>
  </span>
</template>

<script>
export default {
  props: ['filter'],

  computed: {
    desc: function() {
      return this.filter.desc;
    },

    field: function() {
      if (this.filter.expr) {
        return this.filter.desc || this.$t('queries.unknown');
      }

      const {formCaption, caption} = this.filter.fieldObj || {formCaption: [], caption: ''};
      return formCaption.join(' >> ') + ' >> ' + caption;
    },

    op: function() {
      return this.filter.op && this.$t('queries.op.' + this.filter.op);
    },

    value: function() {
      const {op, values, subQuery} = this.filter;
      if (!values || values.length == 0 || subQuery) {
        return null;
      }

      if (op == 'BETWEEN') {
        return values[0] + ' and ' + values[1];
      } else if (op == 'IN' || op == 'NOT_IN') {
        return '["' + values.join('", "') + '"]';
      } else {
        return values[0];
      }
    },

    subQuery: function() {
      return this.filter.subQuery;
    }
  }
}
</script>

<style scoped>
.filter-description .field {
  font-style: italic;
}

.filter-description .op {
  font-weight: bold;
}

.filter-description .value {
  font-style: italic;
}
</style>
