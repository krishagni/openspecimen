<template>
  <a target="_blank" :href="url" :class="linkClass">
    <span>{{value}}</span>
  </a>
</template>

<script>
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

  computed: {
    linkClass: function() {
      return this.url == null ? ['disabled-link'] : []
    }
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
        } else if (data[match[1]] != undefined && data[match[1]] != null) {
          result = result.replace(match[0], data[match[1]]);
        }
      }

      this.url = result != colDef.url ? result : null;
      this.value = value;
    }
  }
}
</script>

<style scoped>
.disabled-link {
  text-decoration: none;
  color: inherit;
  pointer-events: none;
}
</style>
