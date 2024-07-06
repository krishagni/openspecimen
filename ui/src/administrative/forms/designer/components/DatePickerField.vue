<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <div class="p-fluid p-grid">
      <div class="p-field p-col-12">
        <label> Format </label>
        <Dropdown
          v-model="fm.format"
          :options="formats"
          optionLabel="label"
          optionValue="value"
        />
      </div>
      <div class="p-field p-col-12">
        <label> Default Date </label>
        <div class="p-grid">
          <div class="p-field p-col-3 ">
            <div class="p-field-radiobutton">
            <label>
              <RadioButton
                name="defaultDate"
                :value="'NONE'"
                v-model="fm.defaultType"
              />
              <span> None </span>
            </label>
            </div>
          </div>
          <div class="p-field p-col-3 ">
            <div class="p-field-radiobutton">
            <label>
              <RadioButton
                name="defaultDate"
                :value="'CURRENT_DATE'"
                v-model="fm.defaultType"
              />
              <span> Current Date </span>
            </label>
            </div>
          </div>
        </div>
      </div>
      </div>
    </CommonFieldProps>
  </div>

  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <Calendar
        v-model="fm.$unused"
        :showTime="showTime"
        :touchUI="true"
        :showIcon="true"
        v-tooltip.bottom="fm.toolTip"
      />
    </div>
  </div>
</template>

<script>
import { computed, reactive } from "vue";
import Calendar from "primevue/calendar";
import Dropdown from "primevue/dropdown";
import RadioButton from "primevue/radiobutton";
import CommonFieldProps from "./CommonFieldProps.vue";

export default {
  name: "DatePickerField",

  components: {
    Calendar,
    Dropdown,
    RadioButton,
    CommonFieldProps,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    fm.format = fm.format || "dd-MM-yyyy";
    if (fm.defaultType == "CURRENT_DATE") {
      fm["$unused"] = new Date();
    } else {
      fm["$unused"] = null;
    }

    let formats = [
      {
        label: "Date Only",
        value: "dd-MM-yyyy",
      },
      {
        label: "Date and Time",
        value: "dd-MM-yyyy HH:mm",
      },
    ];

    let showTime = computed(
      () =>
        fm.format &&
        (fm.format.indexOf("HH:mm") >= 0 || fm.format.indexOf("HH24:mm") >= 0)
    );

    return {
      fm,
      formats,
      showTime,
    };
  },
};
</script>

<style scoped>
.p-field-radiobutton label {
  line-height: 1.4;
}
</style>