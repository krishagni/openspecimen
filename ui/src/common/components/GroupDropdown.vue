<template>
  <div class="os-dropdown">
    <div class="p-float-label" v-if="$attrs['md-type']">
      <Dropdown
        ref="selectWidget"
        v-model="selected"
        :options="options"
        :option-label="listSource.displayProp"
        :option-value="listSource.selectProp"
        :option-group-label="listSource.groupNameProp"
        :option-group-children="listSource.groupItemsProp"
        :filter="true"
        :show-clear="showClear"
        :disabled="disabled"
        :tabindex="tabOrder"
        @show="loadOptions"
        @filter="filterOptions($event)">
        <template #value> {{selectedString}} </template>
      </Dropdown>

      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <Dropdown
        ref="selectWidget"
        v-model="selected"
        :options="options"
        :option-label="listSource.displayProp"
        :option-value="listSource.selectProp"
        :option-group-label="listSource.groupNameProp"
        :option-group-children="listSource.groupItemsProp"
        :filter="true"
        :show-clear="showClear"
        :disabled="disabled"
        :tabindex="tabOrder"
        @show="loadOptions"
        @filter="filterOptions($event)">
        <template #value> {{selectedString}} </template>
      </Dropdown>
    </div>
  </div>
</template>

<script>
import Dropdown from 'primevue/dropdown';

export default {
  props: ['modelValue', 'listSource', 'placeholder', 'form', 'disabled', 'context', 'tabOrder'],

  emits: ['update:modelValue'],

  components: {
    Dropdown
  },

  data() {
    return { 
      options: [],
    };
  },

  methods: {
    async filterOptions({value}) {
      if (this.filterTimer) {
        clearTimeout(this.filterTimer);
        this.filterTimer = null;
      }

      const self = this;
      this.filterTimer = setTimeout(
        () => {
          if (!self.allOptions || !value) {
            self.options = self.allOptions || [];
            return;
          }

          const result = [];
          value = value.toLowerCase();
          for (let group of self.allOptions) {
            const matchedGroup = { }
            matchedGroup[self.listSource.groupNameProp] = group[self.listSource.groupNameProp];

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
        this.listSource.loadFn({context: this.context}).then(options => this.allOptions = this.options = options);
      }
    },

    getSelectedOption: function() {
      if (!this.modelValue) {
        return null
      }

      return this.selected && typeof this.selected == 'object' ? this.selected[this.selectProp || 'id'] : this.selected;
    },

    getDisplayValue() {
      if (this.selected && typeof this.selected == 'object') {
        return this.selected[this.listSource.displayProp];
      }

      return this.selected;
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
      if (this.selected && typeof this.selected == 'object') {
        return this.selected[this.listSource.displayProp];
      } else {
        return this.placeholder;
      }
    },

    showClear: function() {
      return true;
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
  position: relative;
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

.os-dropdown :deep(.p-dropdown-label) {
  white-space: initial;
  overflow: initial;
  height: 2.25rem;
}
</style>
