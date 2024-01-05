<template>
  <div class="os-dropdown">
    <div class="p-float-label" v-if="$attrs['md-type']" :class="!$attrs.placeholder && 'no-label'">
      <Dropdown
        ref="selectWidget"
        v-model="selected"
        :options="options"
        option-label="type"
        option-value="type"
        option-group-label="specimenClass"
        option-group-children="types"
        :filter="true"
        :show-clear="true"
        :disabled="disabled"
        :tabindex="tabOrder"
      />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <Dropdown
        ref="selectWidget"
        v-model="selected"
        :options="options"
        option-label="type"
        option-value="type"
        option-group-label="specimenClass"
        option-group-children="types"
        :filter="true"
        :show-clear="true"
        :disabled="disabled"
        :tabindex="tabOrder"
      />
    </div>
  </div>
</template>

<script>

import Dropdown from 'primevue/dropdown';

import exprUtil from '@/common/services/ExpressionUtil.js';
import util     from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'form', 'disabled', 'context', 'tabOrder', 'entity'],

  emits: ['update:modelValue'],

  components: {
    Dropdown
  },

  data() {
    return { 
      options: []
    };
  },

  computed: {
    selected: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  created() {
    const map = {};
    for (let type of util.getSpecimenTypes()) {
      const types = map[type.specimenClass] = map[type.specimenClass] || {specimenClass: type.specimenClass, types: []};
      types.types.push(type);
    }

    let classes = Object.values(map);
    classes.sort((c1, c2) => c1.specimenClass < c2.specimenClass ? -1 : (c1.specimenClass > c2.specimenClass ? 1 : 0));
    for (let sc of classes) {
      sc.types.sort((t1, t2) => t1.type < t2.type ? -1 : (t1.type > t2.type ? 1 : 0));
    }

    this.options = classes;
    this._updateValue(this.selected);
  },

  watch: {
    selected: function (newValue, oldValue) {
      if (newValue != oldValue) {
        this._updateValue(newValue);
      }
    }
  },

  methods: {
    getDisplayValue: function() {
      if (!this.selected) {
        return null;
      }

      const option = this._getSelectedOption(this.selected);
      return option.type + ' (' + option.specimenClass + ')';
    },

    _updateValue(value) {
      const entity = this.entity || 'specimen';
      const option = this.selectedOption = this._getSelectedOption(value);

      const formData  = (this.form && this.form.formData) || this.form || this.context || {};
      const entityObj = exprUtil.getValue(formData, entity) || {};

      entityObj.specimenClass = option && option.specimenClass || null;
      entityObj.type          = option && option.type || null;
    },

    _getSelectedOption(value) {
      let option = {};
      for (const sc of this.options) {
        for (const type of sc.types) {
          if (type.type == value) {
            option = type;
            break;
          }
        }
      }

      return option;
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
