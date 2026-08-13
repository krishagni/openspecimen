<template>
  <div>
    <component ref="optionsControl" :is="control" v-model="inputValue"
      :name="name" :options="options" :options-per-row="optionsPerRow"
      :tab-order="tabOrder" :disabled="disabled" :context="context" />

    <os-message type="warn" v-if="hasMoreOptions">
      <span v-t="'forms.pv_options_limit'"></span>
    </os-message>
  </div>
</template>

<script>
import Checkbox from './Checkbox.vue';
import RadioButton from './RadioButton.vue';

import pvSvc from '@/common/services/PermissibleValue.js';

export default {
  props: [
    'name', 'modelValue', 'attribute', 'leafValue', 'rootValue',
    'optionsPerRow', 'tabOrder', 'disabled', 'multiple', 'context'
  ],

  emits: ['change', 'update:modelValue'],

  components: {
    Checkbox,
    RadioButton
  },

  data() {
    return { options: [], hasMoreOptions: false };
  },

  computed: {
    control() {
      return this.multiple ? Checkbox : RadioButton;
    },

    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
        this.$emit('change', value);
      }
    }
  },

  watch: {
    attribute: {
      immediate: true,
      handler() {
        this._loadOptions();
      }
    },

    leafValue() {
      this._loadOptions();
    },

    rootValue() {
      this._loadOptions();
    }
  },

  methods: {
    getDisplayValue() {
      return this.$refs.optionsControl && this.$refs.optionsControl.getDisplayValue();
    },

    async _loadOptions() {
      if (!this.attribute) {
        this.options = [];
        this.hasMoreOptions = false;
        return;
      }

      const pvs = await pvSvc.getPvs(this.attribute, '', {
        includeOnlyLeafValue: this.leafValue == true,
        includeOnlyRootValue: this.rootValue == true,
        maxResults: 26
      });

      //
      // 1. create a set of visible PV values for fast lookup
      // 2. iterate through the selected values and check whether all of them
      //    are present in the loaded PVs
      // 3. If yes, no additional work needs to be done
      // 4. Otherwise, explicitly load the selected but invisible PVs and merge
      //    them with the PVs used to render the options
      //
      const visiblePvs    = pvs.slice(0, 25);
      const visibleValues = new Set(visiblePvs.map(pv => pv.value));

      //
      // 2. find selected but invisible options
      //
      const invisibleValues = (this.modelValue instanceof Array ? this.modelValue : [this.modelValue])
        .filter(value => value != null)
        .map(value => typeof value == 'object' ? value.value : value)
        .filter(value => !visibleValues.has(value));

      //
      // 3. if one or more selected options not visible then
      //
      if (invisibleValues.length > 0) {
        //
        // 4. load the invisible options
        //
        const invisiblePvs = await pvSvc.getPvs(this.attribute, invisibleValues, {activityStatus: 'all'});

        //
        // 4. merge them with the list used to render the options
        //
        visiblePvs.push(...invisiblePvs.filter(pv => !visibleValues.has(pv.value)));
      }

      this.options = visiblePvs.map(pv => ({caption: pv.value, value: pv.value}));
      this.hasMoreOptions = pvs.length == 26;
    }
  }
};
</script>
