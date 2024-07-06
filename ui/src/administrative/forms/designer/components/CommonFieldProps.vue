<template>
  <div class="p-fluid p-grid">
    <div class="p-field p-col-12">
      <label> Type </label>
      <InputText type="text" disabled v-model="fieldTitle" />
    </div>

    <div class="p-field p-col-12">
      <label> Display Label </label>
      <InputText type="text" v-model="fm.caption" ref="labelRef" />
    </div>

    <div class="p-field p-col-12">
      <label> Variable Name </label>
      <InputText type="text" v-model="fm.udn" disabled v-if="fm.$saved" />
      <InputText type="text" v-model="fm.udn" v-else />
      <span class="hint" v-if="!fm.$saved"> Ensure variable name is unique within the form. </span>
    </div>

    <div class="p-field p-col-12">
      <label> Tooltip </label>
      <InputText type="text" v-model="fm.toolTip" />
    </div>

    <div class="p-field p-col-4">
      <label> PHI </label>
      <br />
      <InputSwitch v-model="fm.phi" />
    </div>
    <div class="p-field p-col-4">
      <label> Required </label>
      <br />
      <InputSwitch v-model="fm.mandatory" />
    </div>
    <div class="p-field p-col-4">
      <label> Show in Grid </label>
      <br />
      <InputSwitch v-model="fm.showInGrid" />
    </div>

    <div class="p-field p-col-12" v-if="showDefaultValue">
      <label> Default Value </label>
      <InputText type="text" v-model="fm.defaultValue" />
    </div>
  </div>

  <slot></slot>

  <div class="p-fluid p-grid" v-show="!fm.$sfField">
    <div class="p-field p-col-12">
      <div class="p-field-checkbox">
        <Checkbox
          v-model="fm.$sameRowAsLastField"
          :binary="true"
        />
        <label> Display on the same row as the last field </label>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, onMounted, reactive, ref, watch } from "vue";
import InputText from "primevue/inputtext";
import InputSwitch from "primevue/inputswitch";
import Checkbox from "primevue/checkbox";
import utility from "../services/Utility.js";
import fieldsRegistry from "../services/FieldsRegistry.js";

export default {
  name: "CommonFieldProps",

  components: {
    InputText,
    InputSwitch,
    Checkbox
  },

  props: {
    field: Object,
    showDefaultValue: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    let fieldTitle = computed(() => fieldsRegistry.getField(fm.type).label);
    if (!fm.$saved) {
      watch(
        () => fm.caption,
        () => {
          fm.udn = fm.name = utility.toSnakeCase(fm.caption).substring(0, 64);
        }
      );

      watch(
        () => fm.udn,
        () => {
          fm.name = fm.udn = fm.udn.substring(0, 64);
        }
      );
    }

    let labelRef = ref(null);
    onMounted(() => {
      setTimeout(() => labelRef.value.$el.focus(), 100);
    });

    return {
      fm,
      fieldTitle,
      labelRef,
    };
  },
};
</script>

<style scoped>
.hint {
  font-size: 14px;
  font-style: italic;
}
</style>
