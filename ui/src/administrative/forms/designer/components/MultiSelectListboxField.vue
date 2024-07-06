<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <SelectFieldProps :field="fm" />
    </CommonFieldProps>
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <MultiSelect
        v-model="fm.$unused"
        :options="pvs"
        optionLabel="value"
        optionValue="value"
        appendTo="body"
        v-tooltip.bottom="fm.toolTip"
      />
    </div>
  </div>
</template>

<script>
import { computed, reactive } from "vue";
import MultiSelect from "primevue/multiselect";
import CommonFieldProps from "./CommonFieldProps.vue";
import SelectFieldProps from "./SelectFieldProps.vue";

export default {
  name: "MultiSelectListboxField",

  components: {
    CommonFieldProps,
    SelectFieldProps,
    MultiSelect,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    fm['$unused'] = fm.defaultValue && fm.defaultValue.value && [fm.defaultValue.value];

    let pvs = computed(function () {
      if (!fm.pvs) {
        return [];
      }

      return fm.pvs.filter((pv) => !!pv.value);
    });

    return {
      fm,
      pvs,
    };
  },
};
</script>

