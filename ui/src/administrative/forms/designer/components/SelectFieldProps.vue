<template>
  <div class="p-fluid p-grid">
    <div class="p-field p-col-12">
      <label> Options </label>
      <draggable :list="fm.pvs" handle=".grip">
        <div v-for="(pv, idx) in fm.pvs" :key="idx">
          <div class="sortable-option">
            <div class="grip" v-if="fctx.editIdx != idx">
              <span>:::</span>
            </div>
            <div v-if="fctx.editIdx == idx">
              <InputText
                type="text"
                v-model="pv.value"
                @keyup="handleEnter"
                @focus="$event.target.select()"
              />
            </div>
            <div class="option-container" v-else>
              <div class="value" @click="editOption(idx)">
                {{ pv.value || "Empty Option" }}
              </div>
              <Button
                icon="pi pi-times"
                class="button p-button-rounded p-button-text p-button-plain"
                @click="removeOption(idx)"
              />
            </div>
          </div>
        </div>
      </draggable>
    </div>
    <div class="p-field p-col-3">
      <Button
        class="p-button-text opt-add"
        label="Add Option"
        @click="addOption"
      />
    </div>
    <div class="p-field p-col-5">
      <FileUpload
        v-if="!fctx.processingOptions"
        class="opt-uploader"
        mode="basic"
        :customUpload="true"
        :auto="true"
        chooseLabel="Upload Options"
        accept="text/plain"
        @uploader="optionsFileReader"
      />
    </div>
    <div class="p-field p-col-4">
      <Dropdown
        v-model="fm.pvOrdering"
        :options="fctx.sortOptions"
        optionLabel="value"
        optionValue="name"
        @change="sortOptions"
      />
    </div>

    <div class="p-field p-col-12">
      <label> Default Option </label>
      <Dropdown
        v-model="fm.defaultValue.value"
        :options="fm.pvs"
        optionLabel="value"
        optionValue="value"
        showClear="true"
      />
    </div>

    <div class="p-field p-col-12" v-if="showOptionsLayout">
      <label> Options Per Row </label>
      <InputNumber v-model="fm.optionsPerRow" />
    </div>
  </div>
</template>

<script>
import { reactive } from "vue";
import InputText from "primevue/inputtext";
import InputNumber from "primevue/inputnumber";
import Dropdown from "primevue/dropdown";
import Button from "primevue/button";
import FileUpload from "primevue/fileupload";
import { useToast } from "primevue/usetoast";
import { VueDraggableNext } from "vue-draggable-next";

export default {
  name: "SelectFieldProps",

  components: {
    InputText,
    InputNumber,
    Dropdown,
    Button,
    FileUpload,
    draggable: VueDraggableNext,
  },

  props: {
    field: Object,
    preview: Boolean,
    showOptionsLayout: Boolean,
  },

  setup(props) {
    const toast = useToast();

    let fm = reactive(props.field);
    if (!fm.pvs || fm.pvs.length == 0) {
      fm.pvs = [{ value: "" }];
    } else if (fm.pvs[fm.pvs.length - 1].value) {
      fm.pvs.push({ value: "" });
    }

    if (!fm.defaultValue) {
      fm.defaultValue = {};
    }

    let fctx = reactive({
      editIdx: fm.pvs.length - 1,
      processingOptions: false,
      sortOptions: [
        { name: "ASC", value: "Ascending" },
        { name: "DESC", value: "Descending" },
        { name: "NONE", value: "None" },
      ],
    });

    let handleEnter = function (e) {
      if (e.keyCode == 13) {
        fm.pvs.splice(fctx.editIdx + 1, 0, { value: "" });
        ++fctx.editIdx;
      }
    };

    let addOption = function () {
      fm.pvs.push({ value: "" });
      fctx.editIdx = fm.pvs.length - 1;
    };

    let editOption = function (idx) {
      fctx.editIdx = idx;
    };

    let removeOption = function (idx) {
      let rmOpts = fm.pvs.splice(idx, 1);
      if (rmOpts.length > 0 && rmOpts[0].value == fm.defaultValue.value) {
        fm.defaultValue = {};
      }

      if (idx <= fctx.editIdx) {
        --fctx.editIdx;
      }

      if (fm.pvs.length == 0) {
        fm.pvs.push({ value: "" });
      }
    };

    let optionsFileReader = function (event) {
      fctx.processingOptions = true;

      let file = event.files[0];
      if (file.type != 'text/plain' && file.type != 'text/csv') {
        toast.add({severity: 'error', detail: 'Input file should be either in plain text or CSV format', life: 5000});
        setTimeout(() => fctx.processingOptions = false, 100);
        return;
      }

      let reader = new FileReader();
      reader.readAsText(file, "UTF-8");
      reader.onload = (evt) => {
        let options = evt.target.result.split(/\r?\n/);
        if (options.length > 0) {
          let idx = fm.pvs.length - 1;
          while (idx >= 0 && !fm.pvs[idx].value) {
            fm.pvs.splice(idx, 1);
            idx--;
          }
        }

        for (let option of options) {
          if (!option) {
            continue;
          }

          fm.pvs.push({ value: option });
        }

        fctx.processingOptions = false;
        fctx.editIdx = -1;
      };
      reader.onerror = (evt) => {
        console.error(evt);
        fctx.processingOptions = false;
      };
    };

    let sortOptions = function (event) {
      if (!event.value || event.value == 'NONE') {
        return;
      }

      if (event.value == 'ASC') {
        fm.pvs.sort((pv1, pv2) => pv1.value < pv2.value ? -1 : (pv1.value > pv2.value ? 1 : 0));
      } else if (event.value == 'DESC') {
        fm.pvs.sort((pv1, pv2) => pv1.value > pv2.value ? -1 : (pv1.value < pv2.value ? 1 : 0));
      }

      //
      // empty option is also sorted. remove it
      //
      fm.pvs = fm.pvs.filter((pv) => !!pv.value);
      fm.pvs.push({value: ''});
    }

    return {
      fm,
      fctx,
      handleEnter,
      addOption,
      editOption,
      removeOption,
      optionsFileReader,
      sortOptions,
    };
  },
};
</script>

<style scoped>
.option-container {
  margin-left: -1rem;
  padding-left: 1rem;
}

.option-container:after {
  content: "";
  display: table;
  clear: both;
  overflow: auto;
}

.option-container .value {
  padding: 0.5rem 0;
  float: left;
  width: calc(100% - 40px);
}

.option-container .button {
  float: right;
}

.opt-add >>> .p-button-label {
  text-align: left;
}

.opt-uploader >>> .p-button {
  background: transparent;
  border: transparent;
  color: #2196f3;
}

.opt-uploader >>> .p-button:hover {
  background: rgba(33, 150, 243, 0.04);
  color: #2196f3;
  border-color: transparent;
}

.sortable-option {
  position: relative;
}

.sortable-option .grip {
  display: none;
  position: absolute;
  top: 0.5rem;
  left: -1rem;
  cursor: grab;
  transform: rotate(90deg);
  font-weight: 700;
}

.sortable-option:hover > .grip {
  display: initial;
}
</style>
