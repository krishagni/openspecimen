<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm" :showDefaultValue="true">
      <div class="p-fluid p-grid">
        <div class="p-field p-col-12">
          <label> Rows </label>
          <InputText type="text" v-model="fm.noOfRows" />
        </div>
      </div>
    </CommonFieldProps>
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <Textarea
        :rows="fm.noOfRows"
        v-model="fm.$unused"
        v-tooltip.bottom="fm.toolTip"
      />
    </div>
  </div>
</template>

<script>
import { reactive } from "vue";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import CommonFieldProps from "./CommonFieldProps.vue";

export default {
  name: "TextAreaField",

  components: {
    InputText,
    Textarea,
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