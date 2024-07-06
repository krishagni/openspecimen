<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm" />
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <AutoComplete
        v-model="fm.$unused"
        field="name"
        :suggestions="sites"
        :dropdown="true"
        @complete="searchSite($event)"
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
import CommonFieldProps from "./CommonFieldProps.vue";

import http from "@/common/services/HttpClient.js";

export default {
  name: "SiteField",

  components: {
    CommonFieldProps,
    AutoComplete,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    fm["$unused"] = fm.defaultValue;

    let sites = ref([]);
    let cachedSites = [];
    let searchSite = (event) => {
      let searchTerm = event.query.trim();
      if (searchTerm.length == 0 && cachedSites.length > 0) {
        sites.value = [...cachedSites];
        return;
      }

      http.get("sites", { name: searchTerm }).then((ret) => {
        if (searchTerm.length == 0) {
          cachedSites = ret;
        }

        sites.value = ret;
      });
    };

    return {
      fm,
      sites,
      searchSite,
    };
  },
};
</script>

