
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

export default {
  props: ['modelValue', 'selectProp', 'multiple'],

  components: {
    Dropdown,
    MultiSelectDropdown
  },

  data() {
    return {
      listSource: {
        loadFn: (opts) => {
          opts = opts || {maxResults: 100};
          if (opts.value) {
            return http.get('users/' + opts.value).then((user) => [user]);
          } else {
            return http.get('users', Object.assign({searchString: opts.query || ''}, opts));
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
