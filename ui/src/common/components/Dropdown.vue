
<template>
  <div class="os-dropdown">
    <div class="p-float-label" v-if="$attrs['md-type']">
      <Dropdown
        v-model="selected"
        :options="ctx.options"
        :option-label="displayProp"
        :option-value="selectProp"
        :filter="true"
        :show-clear="showClear"
        @change="onChange"
        @focus="loadOptions"
        @filter="searchOptions($event)"
      />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <Dropdown
        v-model="selected"
        :options="ctx.options"
        :option-label="displayProp"
        :option-value="selectProp"
        :filter="true"
        :show-clear="showClear"
        @change="onChange"
        @focus="loadOptions"
        @filter="searchOptions($event)"
      />
    </div>
  </div>
</template>

<script>
import { reactive } from 'vue';
import Dropdown from 'primevue/dropdown';

import http from '@/common/services/HttpClient.js';
import exprUtil from '@/common/services/ExpressionUtil.js';

export default {
  props: ['modelValue', 'listSource', 'form'],

  emits: ['update:modelValue'],

  components: {
    Dropdown
  },

  setup() {
    let ctx = reactive({ options: [] });

    return { ctx }
  },

  data() {
    return { };
  },

  methods: {
    async loadOptions() {
      this.searchOptions();
    },

    async searchOptions(event) {
      let query = (event && event.value) || '';
      query = query.toLowerCase();

      let selectedVal = await this.selectedValue() || [];
      if (this.listSource.options) {
        this.ctx.options = this.dedup(selectedVal.concat(this.listSource.options));
      } else if (typeof this.listSource.loadFn == 'function') {
        let self = this;
        this.listSource.loadFn({query: query, maxResults: 100}).then(
          function(options) {
            self.ctx.options = self.dedup(selectedVal.concat(options));
          }
        );
      } else if (typeof this.listSource.apiUrl == 'string') {
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
                params[name] = exprUtil.eval(form, expr);
              }
            );
          }
        }

        let key = this.queryString(params);
        this.cachedQueries = this.cachedQueries || {};
        let options = this.cachedQueries[key];
        if (!options) {
          this.cachedQueries[key] = options = await http.get(this.listSource.apiUrl, params);
        }

        this.ctx.options = this.dedup(selectedVal.concat(options));
      }
    },

    async selectedValue() {
      if (!this.modelValue) {
        return [];
      }

      if (typeof this.modelValue == 'object') {
        return [this.modelValue];
      }

      this.cache = this.cache || {};
      if (Object.prototype.hasOwnProperty.call(this.cache, this.modelValue)) {
        return this.cache[this.modelValue];
      }

      let ls = this.listSource;
      let searchOpts = {};
      searchOpts[ls.searchProp || 'value'] = this.modelValue;

      let selected = undefined;
      if (ls.options) {
        selected = ls.options.find(
          (option) => {
            if (ls.selectProp) {
              return option[ls.selectProp] == this.modelValue;
            } else {
              return option == this.modelValue;
            }
          }
        );
        selected = (selected && [selected]) || [];
      } else if (typeof ls.loadFn == 'function') {
        selected = await ls.loadFn(searchOpts);
      } else if (typeof ls.apiUrl == 'string') {
        selected = await http.get(ls.apiUrl, searchOpts);
      }

      this.cache[this.modelValue] = selected;
      return selected;
    },

    dedup(options) {
      let ls = this.listSource;
      let selectProp = ls.selectProp || 'id';
      let optionsMap = options.reduce(
        (acc, option) => {
          if (!acc[option[selectProp]]) {
            acc[option[selectProp]] = option;
          }

          return acc;
        },
        {}
      );

      return Object.keys(optionsMap).map((key) => optionsMap[key]);
    },

    queryString(params) {
      return Object.keys(params || {}).sort().reduce(
        (result, param) => {
          if (result) {
            result += '&';
          }

          if (params[param]) {
            result += param + '=' + params[param];
          }

          return result;
        },
        ''
      );
    },

    onChange: function() {
      this.optionSelected = true;
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

  watch: {
    selected: function(newVal) {
      if (!newVal) {
        return;
      }

      let selectProp = this.listSource.selectProp || 'id';
      this.cache = this.cache || {};
      this.cache[newVal[selectProp]] = newVal;
    },

    modelValue: async function() {
      if (!this.optionSelected) {
        let selectedVal = await this.selectedValue();
        if (this.ctx.options) {
          this.ctx.options = this.dedup(selectedVal.concat(this.ctx.options));
        } else {
          this.ctx.options = selectedVal;
        }
      }

      this.optionSelected = false;
    }
  },

  mounted() {
    if (this.modelValue) {
      this.selectedValue().then((val) => this.ctx.options = val);
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

  .os-dropdown .p-float-label :deep(.p-dropdown) {
    border: 0px;
    border-bottom: 2px solid #ced4da;
    border-radius: 0px;
    box-shadow: none;
  }

  .os-dropdown :deep(.p-dropdown) {
    width: 100%;
  }

  .os-dropdown .p-float-label :deep(.p-dropdown .p-inputtext) {
    padding: 2px 0px;
  }

  .os-dropdown .p-float-label :deep(.p-dropdown.p-inputwrapper-focus) {
    border-bottom-color: #007bff;
  }

  .os-dropdown .p-float-label :deep(.p-dropdown:not(.p-inputwrapper-focus) ~ label) {
    color: #999;
  }

  .os-dropdown .p-float-label :deep(.p-dropdown.p-inputwrapper-filled .p-inputtext) {
    padding: 2px 0px;
  }

  .os-dropdown .p-float-label :deep(.p-dropdown-trigger-icon) {
    opacity: 0.5;
    font-size: 0.75rem;
  }

  .os-dropdown .p-float-label :deep(.p-dropdown-clear-icon) {
    opacity: 0.5;
    font-size: 0.75rem;
    margin-top: -0.40rem;
  }
</style>
