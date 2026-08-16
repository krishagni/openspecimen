<template>
  <div v-if="!preview">
    <CommonFieldProps :field="fm">
      <div class="p-fluid p-grid">
        <div class="p-field p-col-12">
          <label> Options </label>
          <div class="dropdown-selector">
            <PvAttributeDropdown v-model="fm.attribute" :form-id="main.id" @select="selectAttribute" />
            <Button icon="pi pi-plus" aria-label="Add Dropdown" class="p-button-text"
              v-tooltip.bottom="'Add Dropdown'" @click="showAddDropdown" />
            <Button v-if="selectedAttribute && selectedAttribute.formId"
              icon="pi pi-cog" aria-label="Manage Options" class="p-button-text"
              v-tooltip.bottom="'Manage Options'" @click="showManageDropdown" />
          </div>
        </div>

        <div class="p-field p-col-4" v-if="displayType == 'dropdown'">
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

        <div class="p-field p-col-4" v-if="displayType != 'dropdown'">
          <label> Options Per Row </label>
          <InputNumber v-model="fm.optionsPerRow" :min="1" />
        </div>

        <div class="p-field p-col-12">
          <label> Default Value </label>
          <AutoComplete v-model="defaultPv" field="value" :suggestions="pvs"
            :dropdown="true" @complete="searchPv($event)" />
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
      <AutoComplete v-if="displayType == 'dropdown'"
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
      <div v-else v-tooltip.bottom="fm.toolTip">
        <div class="pv-options-row" v-for="(row, idx) in optionRows" :key="idx">
          <div class="pv-option" :style="{width: optionWidth + '%'}" v-for="(option, jdx) in row" :key="jdx">
            <Checkbox v-if="displayType == 'checkbox'" :name="fm.udn" :value="option.value" v-model="fm.$unused" />
            <RadioButton v-else :name="fm.udn" :value="option.value" v-model="fm.$unused" />
            <label>{{ option.value }}</label>
          </div>
        </div>
        <os-message type="warn" v-if="hasMorePvs">
          <span v-t="'forms.pv_options_limit'"></span>
        </os-message>
      </div>
    </div>
  </div>
</template>

<script>
import { computed, reactive, ref, watch } from "vue";
import AutoComplete from "primevue/autocomplete";
import Checkbox from "primevue/checkbox";
import InputNumber from "primevue/inputnumber";
import InputSwitch from "primevue/inputswitch";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import Button from "primevue/button";
import Dialog from "primevue/dialog";
import Paginator from "primevue/paginator";
import RadioButton from "primevue/radiobutton";
import { useToast } from "primevue/usetoast";
import CommonFieldProps from "./CommonFieldProps.vue";
import PvAttributeDropdown from "./PvAttributeDropdown.vue";

import http from "@/common/services/HttpClient.js";
import utility from "../services/Utility.js";

export default {
  name: "PvField",

  components: {
    CommonFieldProps,
    AutoComplete,
    Checkbox,
    InputNumber,
    InputSwitch,
    InputText,
    Textarea,
    Button,
    Dialog,
    Paginator,
    RadioButton,
    PvAttributeDropdown,
  },

  props: {
    field: Object,
    preview: Boolean,
    noLabel: Boolean,
    main: Object,
    subForm: Object,
    displayType: {
      type: String,
      default: 'dropdown'
    }
  },

  emits: ['save-requested'],

  setup(props) {
    const toast = useToast();
    let fm = reactive(props.field);
    if (props.displayType == 'checkbox') {
      fm.multiple = true;
      fm["$unused"] = fm.defaultValue ? [fm.defaultValue] : [];
    } else {
      if (props.displayType == 'radio') {
        fm.multiple = false;
      }

      if (props.displayType == 'dropdown') {
        let defaultPv = fm.defaultValue ? {value: fm.defaultValue} : null;
        fm["$unused"] = fm.multiple ? (defaultPv ? [defaultPv] : []) : defaultPv;
      } else {
        fm["$unused"] = fm.defaultValue;
      }
    }

    if (props.displayType != 'dropdown' && !fm.optionsPerRow) {
      fm.optionsPerRow = 1;
    }

    let pvs         = ref([]);
    let hasMorePvs  = ref(false);
    let cachedPvs   = {};
    let defaultPv   = computed({
      get: () => fm.defaultValue ? {value: fm.defaultValue} : null,
      set: (pv) => fm.defaultValue = pv?.value || null
    });
    let optionRows  = computed(() => utility.getOptionRows(pvs.value, fm.optionsPerRow));
    let optionWidth = computed(() => utility.getOptionWidth(fm.optionsPerRow));

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

    let selectedAttribute = ref(null);

    //
    // Loads up to 26 active options when the field is initialised, its attribute changes, or its
    // managed options change. The first 25 are previewed; the extra result determines whether to
    // warn the designer that the PV list is unsuitable for checkbox or radio-button rendering.
    //
    let _loadPreviewPvs = async () => {
      if (props.displayType == 'dropdown') {
        return;
      }

      if (!fm.attribute) {
        pvs.value = [];
        hasMorePvs.value = false;
        return;
      }

      let result = await http.get('permissible-values/v', {
        attribute: fm.attribute,
        includeOnlyLeafValue: fm.leafValue,
        maxResults: 26
      });
      hasMorePvs.value = result.length > 25;
      pvs.value = result.slice(0, 25);
    };

    watch(() => fm.leafValue, _loadPreviewPvs);

    let currentAttribute = fm.attribute;

    //
    // Clears an invalid default when the attribute changes and retains the selected attribute
    // detail used to identify form-scoped attributes.
    //
    let selectAttribute = (attribute) => {
      let newAttribute = attribute?.attribute;

      // Ignore a stale selection emitted by an earlier asynchronous attribute load.
      if (newAttribute != fm.attribute) {
        return;
      }

      if (currentAttribute != newAttribute) {
        fm.defaultValue = null;
        currentAttribute = newAttribute;
      }

      selectedAttribute.value = attribute;
      _loadPreviewPvs();
    };

    _loadPreviewPvs();

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
        selectAttribute(attr);
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
      let [managedPvs, count] = await Promise.all([
        http.get('permissible-values', params),
        http.get('permissible-values/count', params)
      ]);
      manageDialog.pvs = managedPvs;
      manageDialog.totalRecords = count.count;
    };

    //
    // Refreshes the management dialog and its independently filtered checkbox/radio preview after
    // an option is added, updated, or deleted.
    //
    let _refreshManagedOptions = async () => {
      await _loadManagedOptions();
      await _loadPreviewPvs();
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
      await _refreshManagedOptions();
    };

    //
    // Persists edits to an existing option and refreshes the current page.
    //
    let updateOption = async (pv) => {
      await http.put('permissible-values/' + pv.id, pv);
      await _refreshManagedOptions();
    };

    //
    // Hard-deletes an option through the PV API and refreshes the current page.
    //
    let deleteOption = async (pv) => {
      await http.delete('permissible-values/' + pv.id);
      await _refreshManagedOptions();
    };

    return {
      fm,
      defaultPv,
      pvs,
      searchPv,
      optionRows,
      optionWidth,
      hasMorePvs,
      selectedAttribute,
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

.dropdown-selector .p-autocomplete {
  flex: 1;
  min-width: 0;
}

.dropdown-selector .p-button {
  flex: none;
  width: 2.5rem;
}

.pv-options-row {
  display: flex;
  flex-wrap: wrap;
}

.pv-option {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.75rem;
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
