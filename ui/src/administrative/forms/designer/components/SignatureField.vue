<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm" />
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <div>
        <SignatureCanvas
          :data="fm.$unused"
          @signatureSaved="onSignSave"
          v-tooltip.bottom="fm.toolTip"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { reactive } from "vue";
import CommonFieldProps from "./CommonFieldProps.vue";
import SignatureCanvas from "./SignatureCanvas.vue";

export default {
  name: "SignatureField",

  components: {
    CommonFieldProps,
    SignatureCanvas,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    fm["$unused"] = "";

    let onSignSave = function (eventData) {
      fm["$unused"] = eventData.data;
    };

    return {
      fm,
      onSignSave,
    };
  },
};
</script>