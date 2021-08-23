
<template>
  <div class="os-dropdown">
    <div class="p-float-label" v-if="$attrs['md-type']">
      <MultiSelect
        v-model="selected"
        :options="ctx.options"
        :option-label="displayProp"
        :option-value="selectProp"
        :filter="true"
        :show-clear="showClear"
        @show="searchOptions"
        @filter="searchOptions($event)"
      />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <MultiSelect
        v-model="selected"
        :options="ctx.options"
        :option-label="displayProp"
        :option-value="selectProp"
        :filter="true"
        :show-clear="showClear"
        @show="searchOptions"
        @filter="searchOptions($event)"
      />
    </div>
  </div>
</template>

<script>
import { reactive } from 'vue';
import MultiSelect from 'primevue/multiselect';

import http from '@/common/services/HttpClient.js';

export default {
  props: ['modelValue', 'listSource', 'form'],

  emits: ['update:modelValue'],

  components: {
    MultiSelect
  },

  setup() {
    let ctx = reactive({ options: [] });
    return { ctx }
  },

  data() {
    return { };
  },

  methods: {
    searchOptions(event) {
      let query = (event && event.value) || '';
      query = query.toLowerCase();

      if (this.listSource.options) {
        this.ctx.options = this.dedup(this.selectedValues().concat(this.listSource.options));
      } else if (typeof this.listSource.loadFn == 'function') {
        let self = this;
        this.listSource.loadFn({query: query, maxResults: 100}).then(
          function(options) {
            self.ctx.options = self.dedup(self.selectedValues().concat(options));
          }
        );
      } else if (typeof this.listSource.apiUrl == 'string') {
        let self = this;
        let ls = this.listSource;

        let params = {maxResults: 100};
        params[ls.searchProp || 'query'] = query;
        if (ls.queryParams) {
          if (ls.queryParams.static) {
            Object.keys(ls.queryParams.static).forEach(name => params[name] = ls.static[name]);
          }

          if (ls.queryParams.dynamic) {
            let form = this.form;
            Object.keys(ls.queryParams.dynamic).forEach(
              function(name) {
                let expr = ls.queryParams.dynamic[name];
                params[name] = new Function('return ' + expr).call(form);
              }
            );
          }
        }

        http.get(this.listSource.apiUrl, params).then(
          function(options) {
            self.ctx.options = self.dedup(self.selectedValues().concat(options));
          }
        );
      }
    },

    selectedValues() {
      if (!this.modelValue) {
        return [];
      }

      return (this.modelValue || []).map(
        (value) => {
          if (typeof value == 'object' || !this.listSource.displayProp) {
            return value;
          } else if (value) {
            let selVal = {};
            selVal[this.listSource.selectProp] = selVal[this.listSource.displayProp] = value.toString();
            return selVal;
          }
        }
      );
    },

    dedup(options) {
      let self = this;
      let optionsMap = options.reduce(
        (acc, e) => {
          acc[e[self.listSource.selectProp]] = e;
          return acc;
        },
        {}
      );

      return Object.keys(optionsMap).map((key) => optionsMap[key]);
    }
  },

  computed: {
    selected: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    displayProp: function() {
      return this.listSource.displayProp;
    },

    selectProp: function() {
      return this.listSource.selectProp;
    },

    showClear: function() {
      return true;
    }
  },

  mounted() {
    if (this.modelValue) {
      this.searchOptions({value: ''});
    }
  }
}
</script>

<style scoped>
  .os-dropdown .p-float-label {
    margin-top: 10px;
  }

  .os-dropdown .p-float-label :deep(label) {
    left: 0rem;
  }

  .os-dropdown .p-float-label :deep(.p-multiselect) {
    border: 0px;
    border-bottom: 2px solid #ced4da;
    border-radius: 0px;
    box-shadow: none;
  }

  .os-dropdown :deep(.p-multiselect) {
    width: 100%;
  }

  .os-dropdown .p-float-label :deep(.p-multiselect .p-inputtext) {
    padding: 2px 0px;
  }

  .os-dropdown .p-float-label :deep(.p-multiselect.p-inputwrapper-focus) {
    border-bottom-color: #007bff;
  }

  .os-dropdown .p-float-label :deep(.p-multiselect:not(.p-inputwrapper-focus) ~ label) {
    color: #999;
  }

  .os-dropdown .p-float-label :deep(.p-multiselect.p-inputwrapper-filled .p-inputtext) {
    padding: 2px 0px;
  }

  .os-dropdown .p-float-label :deep(.p-multiselect-trigger-icon) {
    opacity: 0.5;
    font-size: 0.75rem;
  }

  .os-dropdown .p-float-label :deep(.p-multiselect-clear-icon) {
    opacity: 0.5;
    font-size: 0.75rem;
    margin-top: -0.40rem;
  }
</style>
