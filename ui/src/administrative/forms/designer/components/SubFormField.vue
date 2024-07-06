<template>
  <div class="p-fluid p-grid" v-if="!preview">
    <div class="p-field p-col-12">
      <label> Label </label>
      <InputText type="text" v-model="fm.caption" />
    </div>

    <div class="p-field p-col-12">
      <label> Name </label>
      <InputText type="text" v-model="fm.udn" />
    </div>
  </div>
  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th v-for="(field, idx) in fields" :key="idx">
                <span>{{ field.caption }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td v-for="(field, idx) in fields" :key="idx">
                <component
                  :is="fieldMetadata(field)"
                  v-bind="{ field: field, preview: true, noLabel: true }"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, defineAsyncComponent, reactive } from "vue";
import InputText from "primevue/inputtext";

export default {
  name: "SubFormField",

  components: {
    InputText,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
  },

  setup(props) {
    let fm = reactive(props.field);
    let fields = computed(function () {
      var result = [];
      for (let row of fm.rows) {
        for (let col of row) {
          result.push(col);
        }
      }

      return result;
    });

    let capitalise = (s) => s.charAt(0).toUpperCase() + s.slice(1);

    let fieldMetadata = computed(() => (field) =>
      defineAsyncComponent(function () {
        let name = field.type;
        if (!name.endsWith("Field")) {
          name = name + "Field";
        }

        return import(`./${capitalise(name)}.vue`);
      })
    );

    return {
      fm,
      fields,
      fieldMetadata,
    };
  },
};
</script>

<style scoped>
.table-wrapper {
  width: 100%;
  display: block;
  overflow-x: auto;
  white-space: nowrap;
}

table {
  width: 100%;
  margin-top: 10px;
  margin-bottom: 20px;
  display: table;
  border-collapse: collapse;
}

table tr {
  margin-right: 0px;
  margin-left: 0px;
}

table thead tr th,
table tbody tr td {
  padding: 8px;
  line-height: 1.42857143;
  vertical-align: top;
  border-top: 1px solid #ddd;
  word-break: break-word;
  min-width: 150px;
}

table thead tr th {
  vertical-align: bottom;
  border-top: 0px;
  border-bottom: 1px solid #ddd;
  font-weight: bold;
  color: #707070;
  font-size: 12px;
  border-bottom: 0px;
  background: #f5f5f5;
}
</style>
