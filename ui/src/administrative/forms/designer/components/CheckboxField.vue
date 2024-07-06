<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <SelectFieldProps :field="fm" :showOptionsLayout="true"/>
    </CommonFieldProps>
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <div v-tooltip.bottom="fm.toolTip">
        <div class="checkbox-row" v-for="(row, idx) in optionRows" :key="idx">
          <div
            class="p-field-checkbox"
            :style="{ width: optionWidth + '%' }"
            v-for="(option, jdx) in row"
            :key="jdx"
          >
            <Checkbox
              :name="fm.udn"
              :value="option.value"
              v-model="fm.$unused"
            />
            <label> {{ option.value }} </label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, reactive } from "vue";
import Checkbox from "primevue/checkbox";
import CommonFieldProps from "./CommonFieldProps.vue";
import SelectFieldProps from "./SelectFieldProps.vue";
import utility from "../services/Utility.js";

export default {
  name: "CheckboxField",

  components: {
    CommonFieldProps,
    SelectFieldProps,
    Checkbox,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    if (fm.defaultValue && fm.defaultValue.value) {
      fm["$unused"] = [fm.defaultValue.value];
    } else {
      delete fm["$unused"]
    }

    let optionRows = computed(() =>
      utility.getOptionRows(fm.pvs, fm.optionsPerRow)
    );

    let optionWidth = computed(() => utility.getOptionWidth(fm.optionsPerRow));

    return {
      fm,
      optionRows,
      optionWidth,
    };
  },
};
</script>

<style scoped>
.checkbox-row::after {
  content: " ";
  display: table;
  clear: both;
}

.checkbox-row .p-field-checkbox {
  float: left;
}
</style>

