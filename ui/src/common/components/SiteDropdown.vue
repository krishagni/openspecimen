
<template>
  <Dropdown v-model="inputValue" :list-source="listSource" />
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';

import http from '@/common/services/HttpClient.js';

export default {
  props: ['modelValue', 'selectProp'],

  components: {
    Dropdown
  },

  data() {
    return {
      listSource: {
        loadFn: (opts) => {
          opts = Object.assign( { name: opts.query || '' }, opts || {maxResults: 100});
          if (opts.value || opts.value == 0) {
            try {
              let id = parseInt(opts.value);
              return http.get('sites/' + id).then((site) => [site]);
            } catch {
              return http.get('sites', Object.assign(Object.assign({}, opts), {name: opts.value, exactMatch: true}));
            }
          } else {
            return http.get('sites', opts);
          }
        },
        selectProp: this.selectProp,
        displayProp: 'name'
      }
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  methods: {
  }
}
</script>
