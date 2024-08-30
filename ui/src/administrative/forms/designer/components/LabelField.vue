<template>
  <div class="p-fluid p-grid" v-if="!preview">
    <div class="p-field p-col-12">
      <label> Type </label>
      <InputText type="text" disabled v-model="fieldTitle" />
    </div>

    <div class="p-field p-col-12">
      <label> Note </label>
      <Textarea rows="2" v-model="fm.caption" />
    </div>

    <div class="p-field p-col-12">
      <label> Name </label>
      <InputText type="text" v-model="fm.udn" disabled v-if="fm.$saved" />
      <InputText type="text" v-model="fm.udn" v-else />
    </div>
  </div>
  <div v-else>
    <os-html :content="fm.caption" />
  </div>
</template>

<script>
import { computed, reactive, watch } from "vue";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import fieldsRegistry from "../services/FieldsRegistry.js";

export default {
  name: "NoteField",

  components: {
    Textarea,
    InputText,
  },

  props: {
    field: Object,
    preview: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    fm.note = true;

    let fieldTitle = computed(() => fieldsRegistry.getField(fm.type).label);
    watch(
      () => fm.udn,
      () => (fm.name = fm.udn)
    );

    return {
      fm,
      fieldTitle,
    };
  },
};
</script>
