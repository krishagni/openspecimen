
<template>
  <span v-if="multiple">
    <MultiSelectDropdown v-model="inputValue" :list-source="listSource" />
  </span>
  <span v-else>
    <Dropdown v-model="inputValue" :list-source="listSource" />
  </span>
</template>

<script>
import Dropdown from '@/common/components/Dropdown.vue';
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';

import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'selectProp', 'multiple'],

  components: {
    Dropdown,
    MultiSelectDropdown
  },

  data() {
    return {
      listSource: {
        loadFn: async (opts) => {
          opts = opts || {maxResults: 100};
          if (opts.value) {
            return http.get('users/' + opts.value).then((user) => [user]);
          } else {
            let queryParams = Object.assign({searchString: opts.query || ''}, opts);
            let key = util.queryString(queryParams);
            this.cachedQueries = this.cachedQueries || {};
            let options = this.cachedQueries[key];
            if (!options) {
              this.cachedQueries[key] = options = await http.get('users', queryParams);
            }

            return options;
          }
        },
        selectProp: this.selectProp,
        displayProp: (user) => user.firstName + ' ' + user.lastName
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
