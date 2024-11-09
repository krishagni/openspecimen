<template>
  <a target="_blank" :href="url">
    <span>{{value}}</span>
  </a>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['params'],

  data() {
    return {
      url: '',

      value: ''
    }
  },

  created() {
    this.updateDisplay(this.params);
  },

  methods: {
    refresh: function(params) {
      this.updateDisplay(params);
    },

    updateDisplay: function(params) {
      const {column: {colDef}, data, value} = params || {column: {colDef: {url: ''}}, data: {}};

      let match = null;
      let result = colDef.url;

      const re = new RegExp('{{(.*?)}}', 'g');
      while ((match = re.exec(colDef.url)) != null) {
        if (match[1] == '$value') {
          result = result.replace('{{$value}}', value);
        } else {
          result = result.replace(match[0], data[match[1]]);
        }
      }

      this.url = routerSvc.ngUrl(result.substring(2));
      this.value = value;
    }
  }
}
</script>
