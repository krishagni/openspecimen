<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm" :showDefaultValue="true" />
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <InputText
        type="text"
        v-model="fm.$unused"
        v-tooltip.bottom="fm.toolTip"
      />
    </div>
  </div>
</template>

<script>
import { reactive } from "vue";
import InputText from "primevue/inputtext";
import CommonFieldProps from "./CommonFieldProps.vue";

export default {
  name: "StringTextField",

  components: {
    InputText,
    CommonFieldProps,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    fm["$unused"] = fm.defaultValue;

    return {
      fm,
    };
  },
};
</script>