<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <div class="p-fluid p-grid">
        <div class="p-field p-col-12">
          <label> Allow Multiple Values </label><br />
          <InputSwitch v-model="fm.multiple" :disabled="fm.$saved" />
          <br />
          <small v-if="fm.$saved"> This setting cannot be changed after creation. </small>
        </div>
      </div>
    </CommonFieldProps>
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <AutoComplete
        v-model="fm.$unused"
        field="name"
        :suggestions="containers"
        :dropdown="true"
        :multiple="fm.multiple"
        @complete="searchContainer($event)"
        appendTo="body"
        v-tooltip.bottom="fm.toolTip"
      >
        <template #item="slotProps">
          <span>
            {{ slotProps.item.name }}
          </span>
        </template>
      </AutoComplete>
    </div>
  </div>
</template>

<script>
import { reactive, ref } from "vue";
import AutoComplete from "primevue/autocomplete";
import InputSwitch from "primevue/inputswitch";
import CommonFieldProps from "./CommonFieldProps.vue";

import http from "@/common/services/HttpClient.js";

export default {
  name: "StorageContainerField",

  components: {
    CommonFieldProps,
    AutoComplete,
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

    let containers = ref([]);
    let cachedContainers = [];
    let searchContainer = (event) => {
      let searchTerm = event.query.trim();
      if (searchTerm.length == 0 && cachedContainers.length > 0) {
        containers.value = [...cachedContainers];
        return;
      }

      http.get("storage-containers", { name: searchTerm }).then((ret) => {
        if (searchTerm.length == 0) {
          cachedContainers = ret;
        }

        containers.value = ret;
      });
    };

    return {
      fm,
      containers,
      searchContainer,
    };
  },
};
</script>
