<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <div class="p-fluid p-grid">
        <div class="p-field p-col-12">
          <label> Dropdown </label>
          <div class="dropdown-selector">
            <Dropdown v-model="fm.attribute"
              :options="pvAttrsList" option-label="name" option-value="attribute"
              :filter="true" @filter="searchAttributes" @change="selectAttribute" />
            <Button icon="pi pi-plus" aria-label="Add Dropdown" class="p-button-text"
              v-tooltip.bottom="'Add Dropdown'" @click="showAddDropdown" />
            <Button v-if="selectedAttribute && selectedAttribute.formId"
              icon="pi pi-cog" aria-label="Manage Options" class="p-button-text"
              v-tooltip.bottom="'Manage Options'" @click="showManageDropdown" />
          </div>
        </div>

        <div class="p-field p-col-4">
          <label> Allow Multiple Values </label>
          <br />
          <InputSwitch v-model="fm.multiple" :disabled="fm.$saved" />
          <br />
          <small v-if="fm.$saved"> This setting cannot be changed after creation. </small>
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

    <Dialog header="Add Dropdown" v-model:visible="dropdownDialog.visible" :modal="true" :style="{width: '36rem'}">
      <div class="p-fluid">
        <div class="p-field">
          <label>Caption</label>
          <InputText v-model="dropdownDialog.caption" />
        </div>
        <div class="p-field">
          <label>Options (one per line, optional)</label>
          <Textarea v-model="dropdownDialog.options" rows="8" />
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" class="p-button-text" @click="dropdownDialog.visible = false" />
        <Button label="Create" :loading="dropdownDialog.saving" @click="createDropdown" />
      </template>
    </Dialog>

    <Dialog header="Manage Dropdown Options" v-model:visible="manageDialog.visible" :modal="true"
      :style="{width: '40rem'}" :content-style="{overflow: 'hidden'}">
      <div class="p-fluid manage-options">
        <div class="p-field option-search">
          <InputText v-model="manageDialog.query" placeholder="Search options"
            @input="searchManagedOptions" />
        </div>
        <div class="options-list">
          <div class="p-field" v-for="pv in manageDialog.pvs" :key="pv.id">
            <div class="option-editor">
              <InputText v-model="pv.value" />
              <Button icon="pi pi-check" class="p-button-text" @click="updateOption(pv)" />
              <Button icon="pi pi-trash" class="p-button-text p-button-danger" @click="deleteOption(pv)" />
            </div>
          </div>
        </div>
        <div class="p-field new-options">
          <label>New options (one per line)</label>
          <Textarea v-model="manageDialog.newOptions" rows="5" />
          <Button label="Add Options" @click="addManagedOptions" />
        </div>
        <Paginator v-if="manageDialog.totalRecords > manageDialog.rows"
          :first="manageDialog.first" :rows="manageDialog.rows"
          :total-records="manageDialog.totalRecords" @page="pageManagedOptions" />
      </div>
    </Dialog>
  </div>

  <div class="p-fluid p-grid" v-else>
    <div class="p-field p-col-12">
      <label v-if="!noLabel"> {{ fm.caption }} </label>
      <AutoComplete
        v-model="fm.$unused"
        field="value"
        :suggestions="pvs"
        :dropdown="true"
        :multiple="fm.multiple"
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
import InputSwitch from "primevue/inputswitch";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import Button from "primevue/button";
import Dialog from "primevue/dialog";
import Paginator from "primevue/paginator";
import { useToast } from "primevue/usetoast";
import CommonFieldProps from "./CommonFieldProps.vue";

import http from "@/common/services/HttpClient.js";

export default {
  name: "PvField",

  components: {
    CommonFieldProps,
    AutoComplete,
    Dropdown,
    InputSwitch,
    InputText,
    Textarea,
    Button,
    Dialog,
    Paginator,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
    main: Object,
    subForm: Object,
  },

  emits: ['save-requested'],

  setup(props) {
    const toast = useToast();
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
    let selectedAttribute = ref(null);

    //
    // Merges server results while retaining the currently selected attribute for display.
    //
    let _mergeAttributes = (lists) => {
      let attributes = new Map();
      if (selectedAttribute.value) {
        attributes.set(selectedAttribute.value.attribute, selectedAttribute.value);
      }

      lists.flat().forEach(attr => attributes.set(attr.attribute, attr));
      pvAttrsList.value = Array.from(attributes.values());
    };

    //
    // Loads the first attribute page or searches attributes by both caption and internal name.
    //
    let _loadAttributes = async (query) => {
      let params = {formId: props.main.id, maxResults: 100};
      if (!query) {
        _mergeAttributes([await http.get("permissible-values/attributes", params)]);
        return;
      }

      let [byCaption, byAttribute] = await Promise.all([
        http.get("permissible-values/attributes", {...params, name: query}),
        http.get("permissible-values/attributes", {...params, attribute: query})
      ]);
      _mergeAttributes([byCaption, byAttribute]);
    };

    //
    // Loads the selected attribute explicitly because it might not be present in the first page.
    //
    let _loadSelectedAttribute = async () => {
      if (!fm.attribute) {
        return;
      }

      let list = await http.get("permissible-values/attributes", {
        formId: props.main.id, attribute: fm.attribute, maxResults: 100
      });
      selectedAttribute.value = list.find(attr => attr.attribute == fm.attribute) || null;
      _mergeAttributes([list]);
    };

    //
    // Handles server-side filtering of the potentially large attribute list.
    //
    let searchAttributes = (event) => _loadAttributes(event.value.trim());

    //
    // Retains the complete selected attribute detail used to identify form-scoped attributes.
    //
    let selectAttribute = (event) => {
      selectedAttribute.value = pvAttrsList.value.find(attr => attr.attribute == event.value) || null;
    };

    _loadAttributes();
    _loadSelectedAttribute();

    let dropdownDialog = reactive({visible: false, caption: '', options: '', saving: false});
    let manageDialog = reactive({
      visible: false, pvs: [], newOptions: '', query: '', first: 0, rows: 100, totalRecords: 0
    });
    let manageSearchTimer;

    //
    // Opens the form-scoped dropdown creation dialog with the field caption as its default caption.
    //
    let showAddDropdown = () => {
      if (!fm.udn) {
        toast.add({severity: 'error', detail: 'Enter the field name before adding a dropdown.', life: 5000});
        return;
      }

      dropdownDialog.caption = fm.caption || '';
      dropdownDialog.options = '';
      dropdownDialog.visible = true;
    };

    //
    // Creates a form-scoped attribute and optionally creates the initial multiline option set.
    //
    let createDropdown = async () => {
      let pvs = dropdownDialog.options.split(/\r?\n/).map(value => value.trim()).filter(value => value);
      if (!dropdownDialog.caption.trim()) {
        toast.add({severity: 'error', detail: 'Caption is required.', life: 5000});
        return;
      }

      dropdownDialog.saving = true;
      try {
        let attr = await http.post(
          'permissible-values/attributes',
          {
            formId: props.main.id, name: dropdownDialog.caption,
            pvs: pvs.map(value => ({value}))
          },
          null,
          null,
          (resolve, error) => {
            http.handleError(error);
            resolve(null);
          }
        );
        if (!attr) {
          return;
        }

        fm.attribute = attr.attribute;
        selectedAttribute.value = attr;
        _mergeAttributes([[attr]]);
        dropdownDialog.visible = false;
      } finally {
        dropdownDialog.saving = false;
      }
    };

    //
    // Loads one page of options and the matching total using the current server-side search.
    //
    let _loadManagedOptions = async () => {
      let params = {
        attribute: fm.attribute,
        searchString: manageDialog.query.trim(),
        activityStatus: 'all',
        startAt: manageDialog.first,
        maxResults: manageDialog.rows
      };
      let [pvs, count] = await Promise.all([
        http.get('permissible-values', params),
        http.get('permissible-values/count', params)
      ]);
      manageDialog.pvs = pvs;
      manageDialog.totalRecords = count.count;
    };

    //
    // Opens option management at the first unfiltered page for the selected attribute.
    //
    let showManageDropdown = async () => {
      clearTimeout(manageSearchTimer);
      manageDialog.query = '';
      manageDialog.first = 0;
      await _loadManagedOptions();
      manageDialog.visible = true;
    };

    //
    // Waits until typing pauses for 650 ms, then searches from the first result page.
    //
    let searchManagedOptions = () => {
      clearTimeout(manageSearchTimer);
      manageSearchTimer = setTimeout(async () => {
        manageDialog.first = 0;
        await _loadManagedOptions();
      }, 650);
    };

    //
    // Loads the page selected in the options paginator.
    //
    let pageManagedOptions = async (event) => { manageDialog.first = event.first; await _loadManagedOptions(); };

    //
    // Creates each nonblank line as a new option through the PV validation API.
    //
    let addManagedOptions = async () => {
      let values = manageDialog.newOptions.split(/\r?\n/)
        .map(value => value.trim()).filter(value => value);
      if (values.length == 0) {
        return;
      }

      for (let value of values) {
        await http.post('permissible-values', {attribute: fm.attribute, value});
      }

      manageDialog.newOptions = '';
      await _loadManagedOptions();
    };

    //
    // Persists edits to an existing option and refreshes the current page.
    //
    let updateOption = async (pv) => {
      await http.put('permissible-values/' + pv.id, pv);
      await _loadManagedOptions();
    };

    //
    // Hard-deletes an option through the PV API and refreshes the current page.
    //
    let deleteOption = async (pv) => {
      await http.delete('permissible-values/' + pv.id);
      await _loadManagedOptions();
    };

    return {
      fm,
      pvs,
      searchPv,
      pvAttrsList,
      selectedAttribute,
      searchAttributes,
      selectAttribute,
      dropdownDialog,
      manageDialog,
      showAddDropdown,
      createDropdown,
      showManageDropdown,
      searchManagedOptions,
      pageManagedOptions,
      addManagedOptions,
      updateOption,
      deleteOption
    };
  },
};
</script>

<style scoped>
.dropdown-selector,
.option-editor {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.dropdown-selector .p-dropdown {
  flex: 1;
  min-width: 0;
}

.dropdown-selector .p-button {
  flex: none;
  width: 2.5rem;
}

.option-editor .p-inputtext {
  flex: 1;
}

.manage-options {
  display: flex;
  flex-direction: column;
  height: min(70vh, 44rem);
  min-height: 24rem;
}

.option-search, .new-options, .p-paginator {
  flex: none;
}

.option-search .p-inputtext {
  width: 100%;
  min-width: 0;
}

.options-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 0.25rem;
}
</style>
