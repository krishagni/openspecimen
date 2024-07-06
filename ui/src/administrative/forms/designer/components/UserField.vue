<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm" />
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <AutoComplete
        v-model="fm.$unused"
        field="formattedName"
        :suggestions="users"
        :dropdown="true"
        @complete="searchUser($event)"
        appendTo="body"
        v-tooltip.bottom="fm.toolTip"
      >
        <template #item="slotProps">
          <span>
            {{ slotProps.item.firstName }} {{ slotProps.item.lastName }}
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
  name: "UserField",

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

    let users = ref([]);
    let cachedUsers = [];
    let searchUser = (event) => {
      let searchTerm = event.query.trim();
      if (searchTerm.length == 0 && cachedUsers.length > 0) {
        users.value = [...cachedUsers];
        return;
      }

      http.get("users", { searchString: searchTerm }).then((ret) => {
        if (searchTerm.length == 0) {
          cachedUsers = ret;
        }

        users.value = ret;
        for (let user of ret) {
          user.formattedName = '';
          if (user.firstName) {
            user.formattedName = user.firstName;
          }

          if (user.lastName) {
            user.formattedName += ' ' + user.lastName;
          }

          user.formattedName = user.formattedName.trim();
        }
      });
    };

    return {
      fm,
      users,
      searchUser,
    };
  },
};
</script>

