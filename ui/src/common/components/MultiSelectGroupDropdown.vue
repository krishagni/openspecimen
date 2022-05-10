
<template>
  <div class="os-dropdown">
    <div class="p-float-label" v-if="$attrs['md-type']">
      <MultiSelect
        ref="selectWidget"
        v-model="selected"
        :options="options"
        :option-label="listSource.displayProp"
        :option-value="listSource.selectProp"
        :option-group-label="listSource.groupDisplayProp"
        :option-group-children="listSource.groupItemsProp"
        :filter="true"
        :show-clear="showClear"
        :disabled="disabled"
        @change="onChange"
        @show="loadOptions"
        @filter="filterOptions($event)">
        <template #value> {{selectedString}} </template>
      </MultiSelect>

      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <MultiSelect
        ref="selectWidget"
        v-model="selected"
        :options="options"
        :option-label="listSource.displayProp"
        :option-value="listSource.selectProp"
        :option-group-label="listSource.groupDisplayProp"
        :option-group-children="listSource.groupItemsProp"
        :filter="true"
        :show-clear="showClear"
        :disabled="disabled"
        @change="onChange"
        @show="loadOptions"
        @filter="filterOptions($event)">
        <template #value> {{selectedString}} </template>
      </MultiSelect>
    </div>
  </div>
</template>

<script>
import MultiSelect from 'primevue/multiselect';

export default {
  props: ['modelValue', 'listSource', 'placeholder', 'form', 'disabled', 'context'],

  emits: ['update:modelValue'],

  components: {
    MultiSelect
  },

  data() {
    return { 
      options: [],
    };
  },

  methods: {
    async filterOptions({value}) {
      if (this.filterTimeout) {
        clearTimeout(this.filterTimeout);
        this.filterTimeout = null;
      }

      const self = this;
      this.filterTimeout = setTimeout(
        () => {
          if (!self.allOptions || !value) {
            self.options = self.allOptions || [];
            return;
          }

          const result = [];
          value = value.toLowerCase();
          for (let group of self.allOptions) {
            const matchedGroup = { }
            matchedGroup[self.listSource.groupDisplayProp] = group[self.listSource.groupDisplayProp];

            const matches = matchedGroup[self.listSource.groupItemsProp] = [];
            for (let item of group[self.listSource.groupItemsProp]) {
              if (item[self.listSource.displayProp].toLowerCase().indexOf(value) != -1) {
                matches.push(item);
              }
            }

            if (matches.length > 0) {
              result.push(matchedGroup);
            }
          }

          this.options = result;
        },
        this.$ui.global.appProps.searchDelay || 500
      );
    },

    async loadOptions() {
      this.$refs.selectWidget.filterValue = '';
      this.reloadOptions();
    },

    async reloadOptions() {
      if (this.listSource.options) {
        this.allOptions = this.options = this.listSource.options;
      } else if (typeof this.listSource.loadFn == 'function') {
        this.listSource.loadFn({context: this.context}).then(
          options => {
            this.allOptions = this.options = options;
            console.log(options);
          }
        );
      }
    },

    onChange: function(event) {
      console.log(event);
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

    selectedString: function() {
      if (this.selected) {
        return this.selected.map(s => s[this.listSource.displayProp]).join(', ') || 'empty';
      } else {
        return this.placeholder || 'empty';
      }
    },

    showClear: function() {
      return true;
    }
  },

  watch: {
    selected: function() {
    },
  },

  mounted() {
    /*if (this.modelValue) {
      this.loadOptions();
    }*/
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

.os-dropdown :deep(.p-multiselect-label) {
  white-space: break-spaces;
  /*padding: 0;*/
}
</style>
