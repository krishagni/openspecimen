
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
        @change="onChange"
        @show="loadOptions"
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
        @change="onChange"
        @show="loadOptions"
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
    async loadOptions() {
      if (this.ctx.optionsLoaded) {
        return;
      }

      this.searchOptions();
      this.ctx.optionsLoaded = true;
    },

    async searchOptions(event) {
      let query = (event && event.value) || '';
      query = query.toLowerCase();

      let selectedVals = await this.selectedValues() || [];

      if (this.listSource.options) {
        this.ctx.options = this.dedup(selectedVals.concat(this.listSource.options));
      } else if (typeof this.listSource.loadFn == 'function') {
        let self = this;
        this.listSource.loadFn({query: query, maxResults: 100}).then(
          function(options) {
            self.ctx.options = self.dedup(selectedVals.concat(options));
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
            self.ctx.options = self.dedup(selectedVals.concat(options));
          }
        );
      }
    },

    async selectedValues() {
      if (!(this.modelValue instanceof Array)) {
        return [];
      }

      if (this.modelValue.some((elem) => typeof elem == 'object')) {
        return this.modelValue;
      }

      this.cache = this.cache || {};

      let cached = [], toGet = [];
      for (let item of this.modelValue) {
        if (Object.prototype.hasOwnProperty.call(this.cache, item)) {
          if (this.cache[item]) {
            cached.push(this.cache[item]);
          }
        } else {
          toGet.push(item);
        }
      }

      if (toGet.length == 0) {
        return cached;
      }

      let ls = this.listSource;
      let searchOpts = {};
      searchOpts[ls.searchProp || 'value'] = toGet;

      let selected = undefined;
      if (ls.options) {
        selected = ls.options.filter((option) => toGet.some((testItem) => option[ls.selectProp] == testItem));
      } else if (typeof ls.loadFn == 'function') {
        selected = await ls.loadFn(searchOpts);
      } else if (typeof ls.apiUrl == 'string') {
        selected = await http.get(ls.apiUrl, searchOpts);
      }

      if (selected instanceof Array) {
        selected.forEach((item) => this.cache[item[ls.selectProp]] = item);
      }

      let indices = {};
      for (let idx = 0; idx < this.modelValue.length; ++idx) {
        indices[this.modelValue[idx]] = idx;
      }

      let result = cached.concat(selected);
      result.sort((e1, e2) => indices[e1[ls.selectProp]] - indices[e2[ls.selectProp]]);
      return result;
    },

    dedup(options) {
      let ls = this.listSource;
      let optionsMap = options.reduce(
        (acc, e) => {
          acc[e[ls.selectProp]] = e;
          return acc;
        },
        {}
      );

      return Object.keys(optionsMap).map((key) => optionsMap[key]);
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
      this.cache = this.cache || {};
      newVal.forEach((item) => this.cache[item[this.listSource.selectProp]] = item);   
    },

    modelValue: async function() {
      if (!this.optionSelected) {
        let selectedVals = await this.selectedValues();
        if (this.ctx.optionsLoaded && this.ctx.options) {
          this.ctx.options = this.dedup(selectedVals.concat(this.ctx.options));
        } else {
          this.ctx.options = selectedVals;
        }
      }

      this.optionSelected = false;
    }
  },

  mounted() {
    if (this.modelValue) {
      this.selectedValues().then((values) => this.ctx.options = values);
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
