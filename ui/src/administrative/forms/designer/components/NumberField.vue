<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm" :showDefaultValue="true">
      <div class="p-fluid p-grid">
        <div class="p-field p-col-12">
          <label> Number of Fraction Digits </label>
          <InputNumber type="text" v-model="fm.noOfDigitsAfterDecimal" />
        </div>

        <div class="p-field p-col-12">
          <label> Range </label>
          <div class="p-formgroup-inline">
            <div class="p-field">
              <label class="p-sr-only">Min.</label>
              <InputNumber
                type="text"
                placeholder="Min."
                v-model="fm.minValue"
              />
            </div>
            <div class="p-field">
              <label class="p-sr-only">Max.</label>
              <InputNumber
                type="text"
                placeholder="Max."
                v-model="fm.maxValue"
              />
            </div>
          </div>
        </div>
      </div>
    </CommonFieldProps>
  </div>

  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <InputNumber
        type="text"
        :min="fm.minValue"
        :max="fm.maxValue"
        :minFractionDigits="fm.noOfDigitsAfterDecimal"
        :maxFractionDigits="fm.noOfDigitsAfterDecimal"
        v-tooltip.bottom="fm.toolTip"
        v-model="fm.$unused"
      />
    </div>
  </div>
</template>

<script>
import { reactive } from "vue";
import InputNumber from "primevue/inputnumber";
import CommonFieldProps from "./CommonFieldProps.vue";

export default {
  name: "NumberField",

  components: {
    InputNumber,
    CommonFieldProps,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    if (fm.defaultValue != undefined && fm.defaultValue != null && fm.defaultValue != '') {
      fm["$unused"] = +fm.defaultValue;
    } else {
      fm["$unused"] = undefined;
    }

    return {
      fm,
    };
  },
};
</script>