
<template>
  <div class="os-dropdown">
    <div class="p-float-label" v-if="$attrs['md-type']" :class="!$attrs.placeholder && 'no-label'">
      <Dropdown
        ref="selectWidget"
        v-model="selected"
        :options="ctx.options"
        :data-key="dataKey"
        :option-label="displayProp"
        :option-value="selectProp"
        :filter="true"
        :show-clear="showClear"
        :disabled="disabled"
        :tabindex="tabOrder"
        @change="onChange"
        @focus="loadOptions"
        @filter="filterOptions($event)"
      />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <Dropdown
        ref="selectWidget"
        v-model="selected"
        :options="ctx.options"
        :data-key="dataKey"
        :option-label="displayProp"
        :option-value="selectProp"
        :filter="true"
        :show-clear="showClear"
        :disabled="disabled"
        :tabindex="tabOrder"
        @change="onChange"
        @focus="loadOptions"
        @filter="filterOptions($event)"
      />
    </div>
  </div>
</template>

<script>
import { reactive } from 'vue';
import Dropdown from 'primevue/dropdown';

import http from '@/common/services/HttpClient.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'listSource', 'form', 'disabled', 'context', 'tabOrder', 'dataKey'],

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
    async filterOptions(event) {
      if (this.filterTimeout) {
        clearTimeout(this.filterTimeout);
        this.filterTimeout = null;
      }

      this.filterTimeout = setTimeout(() => this.loadOptions(event), this.$ui.global.appProps.searchDelay || 500);
    },

    async loadOptions(event) {
      if (this.optionSelected) {
        this.optionSelected = false;
        return;
      }

      let query = (event && event.value) || '';
      query = query.toLowerCase();

      let selectedVal = await this.selectedValue() || [];
      if (this.listSource.options) {
        this.ctx.options = this.dedup(selectedVal.concat(this.listSource.options));
      } else if (typeof this.listSource.loadFn == 'function') {
        let self = this;
        this.listSource.loadFn({context: this.context, query: query, maxResults: 100}).then(
          function(options) {
            self.ctx.options = self.dedup(selectedVal.concat(options));
          }
        );
      } else if (typeof this.listSource.apiUrl == 'string') {
        let ls = this.listSource;

        const params = {maxResults: 100};
        params[ls.searchProp || 'query'] = query;
        Object.assign(params, this.queryParams(ls));
        const options = await this.getFromBackend(params);
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

      let ls = this.listSource;
      const searchOpts = {query: this.modelValue};
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
      } else if (ls.initUsingSelectProp && ls.selectProp) {
        selected = {};
        selected[ls.selectProp] = this.modelValue;
        selected = [selected];
      } else if (typeof ls.loadFn == 'function') {
        selected = await ls.loadFn({...searchOpts, context: this.context, _selected: this.modelValue});
      } else if (typeof ls.apiUrl == 'string') {
        Object.assign(searchOpts, this.queryParams(ls));
        selected = this.getFromBackend(searchOpts);
      }

      return selected;
    },

    queryParams(ls) {
      const params = {};
      if (ls.queryParams) {
        if (ls.queryParams.static) {
          Object.keys(ls.queryParams.static).forEach(name => params[name] = ls.queryParams.static[name]);
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

      return params;
    },

    async getFromBackend(params) {
      let cache = (this.form || this.context || {})._formCache || {};
      cache = cache['dropdown'] = cache['dropdown'] || {};

      const url = this.extrapolateUrl(this.listSource.apiUrl);
      const qs = util.queryString(Object.assign({url}, params || {}));
      let promise = cache[qs];
      if (!promise) {
        promise = cache[qs] = http.get(url, params);
      }

      return promise.then(options => options);
    },

    extrapolateUrl(url) {
      const regex = /{{(.*?)}}/g;
      const matches = [...url.matchAll(regex)];

      let result = '';
      let lastMatchIdx = 0;
      let skip = 0;
      for (let match of matches) {
        result += "'" + url.substring(lastMatchIdx + skip, match.index) + "' + " + match[1] + " + ";
        lastMatchIdx = match.index;
        skip = match[0].length;
      }

      result += "'" + url.substring(lastMatchIdx + skip) + "'";
      return exprUtil.eval(this.form, result);
    },

    dedup(options) {
      let ls = this.listSource;
      let selectProp = ls.selectProp || ls.idProp || 'id';
      let seen = [], result = [];
      for (let option of options) {
        if (seen.indexOf(option[selectProp]) == -1) {
          result.push(option);
          seen.push(option[selectProp]);
        }
      }

      return result;
    },

    onChange: function(event) {
      this.optionSelected = !!event.value;
    },

    getSelectedOption: function() {
      if (!this.modelValue) {
        return null;
      }

      if (typeof this.modelValue == 'object') {
        return this.modelValue;
      }

      let options = this.$refs.selectWidget.options || [];
      let selectedOption = null;
      if (this.selectProp) {
        selectedOption = options.find(option => option[this.selectProp] == this.modelValue);
      } else {
        selectedOption = options.find(option => option == this.modelValue);
      }

      if (!selectedOption) {
        if (this.selectProp) {
          selectedOption = {};
          selectedOption[this.selectProp] = this.modelValue;
        } else {
          selectedOption = this.modelValue;
        }
      }

      return selectedOption;
    },

    getDisplayValue() {
      const option = this.getSelectedOption();
      if (!option) {
        return null;
      }

      if (typeof this.displayProp == 'function') {
        return this.displayProp(option);
      } else if (this.displayProp) {
        return option[this.displayProp];
      }

      return option;
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
    modelValue: async function() {
      let selectedVal = await this.selectedValue();
      if (this.ctx.options) {
        this.ctx.options = this.dedup(selectedVal.concat(this.ctx.options));
      } else {
        this.ctx.options = selectedVal;
      }
    }
  },

  mounted() {
    if (this.modelValue) {
      this.selectedValue().then(val => this.ctx.options = val);
    }
  }
}
</script>

<style scoped>
  .os-dropdown .p-float-label:not(.no-label) {
    margin-top: 10px;
  }

  .os-dropdown .p-float-label :deep(label) {
    left: 0rem;
    right: 0rem;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
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
