<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <div class="p-field-checkbox">
        <Checkbox v-model="fm.checked" :binary="true" />
        <label> Default Checked </label>
      </div>
    </CommonFieldProps>
  </div>

  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <div class="p-field-checkbox">
        <Checkbox
          v-model="fm.$unused"
          :binary="true"
          v-tooltip.bottom="fm.toolTip"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { reactive } from "vue";
import Checkbox from "primevue/checkbox";
import CommonFieldProps from "./CommonFieldProps.vue";

export default {
  name: "BooleanCheckboxField",

  components: {
    Checkbox,
    CommonFieldProps,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    if (fm.checked == null || fm.checked == undefined) {
      fm.checked = fm.defaultChecked;
    }

    fm["$unused"] = fm.checked;
    return {
      fm,
    };
  },
};
</script>
