
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
          opts = opts || {maxResults: 100};
          if (opts.value) {
            return http.get('users/' + opts.value).then(
              (user) => [ {id: user.id, name: user.firstName + ' ' + user.lastName} ]
            );
          } else {
            return http.get('users', Object.assign({searchString: opts.query || ''}, opts)).then(
              (users) => {
                users.forEach((user) => user.name = user.firstName + ' ' + user.lastName);
                return users;
              }
            );
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
