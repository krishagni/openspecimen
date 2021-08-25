
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
              return http.get('storage-containers/' + id).then((container) => [container]);
            } catch {
              return http.get('storage-containers/byname/' + opts.value).then((container) => [container]);
            }
          } else {
            return http.get('storage-containers', opts);
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
        if (this.modelValue != null && !isNaN(this.modelValue)) {
          return +this.modelValue;
        }

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
