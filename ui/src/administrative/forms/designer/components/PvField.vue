<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <div class="p-fluid p-grid">
        <div class="p-field p-col-12">
          <label> Dropdown </label>
          <Dropdown v-model="fm.attribute"
            :options="pvAttrsList" option-label="name" option-value="attribute"
            :filter="true" />
        </div>

        <div class="p-field p-col-4">
          <label> Only Leaf Values </label>
          <br />
          <InputSwitch v-model="fm.leafValue" />
        </div>

        <div class="p-field p-col-4">
          <label> Numeric Values </label>
          <br />
          <InputSwitch v-model="fm.numericValues" />
        </div>
      </div>
    </CommonFieldProps>
  </div>

  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <AutoComplete
        v-model="fm.$unused"
        field="value"
        :suggestions="pvs"
        :dropdown="true"
        @complete="searchPv($event)"
        appendTo="body"
        v-tooltip.bottom="fm.toolTip"
      >
        <template #item="slotProps">
          <span>
            {{ slotProps.item.value }}
          </span>
        </template>
      </AutoComplete>
    </div>
  </div>
</template>

<script>
import { reactive, ref } from "vue";
import AutoComplete from "primevue/autocomplete";
import Dropdown from "primevue/dropdown";
// import InputText from "primevue/inputtext";
import InputSwitch from "primevue/inputswitch";
import CommonFieldProps from "./CommonFieldProps.vue";

import http from "@/common/services/HttpClient.js";

export default {
  name: "PvField",

  components: {
    CommonFieldProps,
    AutoComplete,
    Dropdown,
//    InputText,
    InputSwitch,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    fm["$unused"] = fm.defaultValue;

    let pvs = ref([]);
    let cachedPvs = {};
    let searchPv = (event) => {
      let searchTerm = event.query.trim();

      let attrPvs = cachedPvs[fm.attribute || "$$all"];
      if (searchTerm.length == 0 && attrPvs && attrPvs.length > 0) {
        pvs.value = [...attrPvs];
        return;
      }

      let params = {
        searchString: searchTerm,
        includeOnlyLeafValue: fm.leafValue,
        attribute: fm.attribute,
      };

      http.get("permissible-values/v", params).then((ret) => {
        if (searchTerm.length == 0) {
          cachedPvs[fm.attribute || "$$all"] = ret;
        }

        pvs.value = ret;
      });
    };

    let pvAttrsList = ref([]);
    http.get("permissible-values/attributes").then((list)  => pvAttrsList.value = list);

    return {
      fm,
      pvs,
      searchPv,
      pvAttrsList
    };
  },
};
</script>

