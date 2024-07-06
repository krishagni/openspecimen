<template>
  <CommonFieldProps :field="fm" v-if="!preview" />

  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <FileUpload
        mode="basic"
        name="demo[]"
        url="./upload"
        v-tooltip.bottom="fm.toolTip"
      />
    </div>
  </div>
</template>

<script>
import { reactive } from "vue";
import FileUpload from "primevue/fileupload";
import CommonFieldProps from "./CommonFieldProps.vue";

export default {
  name: "FileUploadField",

  components: {
    FileUpload,
    CommonFieldProps,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    return {
      fm,
    };
  },
};
</script>