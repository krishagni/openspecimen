<template>
  <div
    class="form-canvas p-grid p-flex-column"
    v-if="!ctx.subForm"
  >
    <Breadcrumb
      v-if="hierarchy.main"
      :home="hierarchy.main"
      :model="hierarchy.subForm"
    />

    <Card class="details-card p-col p-mb-4">
      <template #content>
        <div class="p-fluid p-grid" v-if="ctx.selectedCard == form">
          <div class="p-field p-col-12">
            <label> Title </label>
            <InputText type="text" v-model="form.caption" ref="titleRef" />
          </div>

          <div class="p-field p-col-12">
            <label> Name </label>
            <InputText
              type="text"
              v-model="form.name"
              disabled
              v-if="form.$saved"
            />
            <InputText type="text" v-model="form.name" v-else />
          </div>
        </div>

        <div class="p-fluid" v-else>
          <h4>{{ form.caption }}</h4>
        </div>

        <div class="buttons">
          <div class="right" v-if="ctx.selectedCard == null">
            <Button
              icon="pi pi-pencil"
              class="p-button-text p-button-plain"
              v-tooltip.bottom="'Edit'"
              @click="edit(form)"
            />
          </div>

          <div v-else-if="ctx.selectedCard == form">
            <Button icon="pi pi-check" label="Save" @click="save" />
            <Button
              icon="pi pi-times"
              label="Cancel"
              class="p-button-secondary"
              @click="cancel(form)"
              v-if="form['$saved'] != false"
            />
          </div>
        </div>
      </template>
    </Card>

    <draggable :list="fields" handle=".grip" @change="onMoveField">
      <Card
        class="p-col p-mb-4 draggable-item"
        v-for="(field, idx) in fields"
        :key="idx"
      >
        <template #content>
          <div class="grip" v-if="ctx.selectedCard == null" v-tooltip.bottom="'Press and hold to move the field'">
            <span>:::</span>
          </div>

          <component
            :is="fieldMetadata(field)"
            v-bind="{ field: field, preview: ctx.selectedCard != field }"
          />

          <div class="buttons">
            <div class="right" v-if="ctx.selectedCard == null">
              <ChangeFieldTypeMenu
                :field="field"
                @changeTo="changeFieldType(field, $event)"
              />
              <Button
                icon="pi pi-pencil"
                class="p-button-text p-button-plain"
                v-tooltip.bottom="'Edit'"
                @click="edit(field)"
              />
              <Button
                icon="pi pi-copy"
                class="p-button-text p-button-plain"
                v-tooltip.bottom="'Clone'"
                @click="copyField(field, idx)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-text p-button-plain"
                v-tooltip.bottom="'Delete'"
                @click="remove(field)"
              />
            </div>

            <div v-else-if="ctx.selectedCard == field">
              <Button icon="pi pi-check" label="Save" @click="save()" />
              <Button
                icon="pi pi-times"
                label="Cancel"
                class="p-button-secondary"
                @click="cancel(field)"
              />
            </div>
          </div>
        </template>
      </Card>
    </draggable>

    <div class="p-col p-mb-4" v-if="ctx.selectedCard == null">
      <span class="add-field">
        <Button
          label="Add Field"
          icon="pi pi-plus"
          @click="toggleAddFieldMenu"
        />
        <Menu class="add-field-menu" ref="addFieldMenu" :model="fieldTypes" :popup="true"
          style="height: 300px; width: 220px; overflow-y: scroll;" />
      </span>

      <Button
        v-if="form.type == 'subForm'"
        icon="pi pi-arrow-left"
        label="Go Back"
        class="p-button-secondary"
        style="margin-left: 0.5em"
        @click="goBackToMainForm()"
      />
    </div>
  </div>
  <div v-else>
    <FormCanvas
      :main="form"
      :subForm="ctx.selectedCard"
      @return-to-main="onReturnToMainForm"
      @save="onSubFormSave"
    />
  </div>
</template>

<script>
import { computed, defineAsyncComponent, reactive, ref, watch } from "vue";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import Button from "primevue/button";
import Divider from "primevue/divider";
import InlineMessage from "primevue/inlinemessage";
import Menu from "primevue/menu";
import Breadcrumb from "primevue/breadcrumb";

import { useToast } from "primevue/usetoast";
import { useConfirm } from "primevue/useconfirm";
import { VueDraggableNext } from "vue-draggable-next";

import ChangeFieldTypeMenu from "./ChangeFieldTypeMenu.vue"
import fieldsRegistry from "../services/FieldsRegistry.js";
import utility from "../services/Utility.js";

export default {
  name: "FormCanvas",

  props: {
    main: Object,
    subForm: Object,
  },

  components: {
    Card,
    Divider,
    InlineMessage,
    InputText,
    Button,
    Menu,
    Breadcrumb,
    ChangeFieldTypeMenu,
    draggable: VueDraggableNext,
  },

  setup(props, { emit }) {
    const toast = useToast();
    const confirm = useConfirm();

    let form = reactive(
      props.subForm ||
      props.main || {
        $saved: false,
        $errors: {},
        name: "",
        caption: "",
        rows: [],
      }
    );

    let fields = computed(() => {
      let result = [];
      let rowIdx = -1;
      for (let row of form.rows) {
        let sameRowAsLastField = false;
        ++rowIdx;

        for (let field of row) {
          field.$rowIdx = rowIdx;
          field.$sameRowAsLastField = sameRowAsLastField;
          field.$sfField = props.subForm != null;
          sameRowAsLastField = true;
          result.push(field);
        }
      }

      return result;
    });

    let copy = {};
    let ctx = reactive({
      selectedCard: form["$saved"] == false ? form : null,
      saving: false,
      hierarchy: {},
    });

    let hierarchy;
    if (props.subForm) {
      watch(
        () => form.name,
        () => (form.udn = form.name)
      );

      if (form.rows && form.rows.length > 0) {
        let rows = [];
        for (let row of form.rows) {
          for (let field of row) {
            rows.push([field]);
          }
        }

        form.rows = rows;
      }

      hierarchy = computed(() => {
        return {
          main: { label: props.main.caption },
          subForm: [{ label: form.caption || "New Subform" }],
        };
      });
    } else {
      hierarchy = computed(() => {
        return {};
      });
    }

    let formNameWatcher;
    if (!form.$saved) {
      formNameWatcher = watch(
        () => form.caption,
        () => {
          form.name = utility.toSnakeCase(form.caption).substring(0, 64);
          if (props.subForm) {
            form.udn = form.name;
          }
        }
      );
    }

    let fieldTypes = computed(function () {
      let result = [];
      let types = fieldsRegistry.getTypes();
      for (let type in types) {
        if (props.subForm && types[type].allowedInSubForm == false) {
          continue;
        }

        let field = { ...types[type] };
        field["command"] = () => addField(field.type);
        result.push(field);
      }

      return result;
    });

    function addField(type) {
      let field = { 
        $saved: false,
        type: type,
        $rowIdx: form.rows.length,
        $sfField: props.subForm != null
      };

      if (type == "subForm") {
        field.rows = [];
      }

      let newRow = [field];
      form.rows.push(newRow);
      edit(field);
    }

    let addFieldMenu = ref(null);
    function toggleAddFieldMenu(event) {
      addFieldMenu.value.toggle(event);
    }

    let onMoveField = function (evt) {
      if (!evt.moved) {
        return;
      }

      let field = evt.moved.element;
      let oldRow = form.rows[field.$rowIdx];

      let newIndex = evt.moved.newIndex;
      if (newIndex > evt.moved.oldIndex) {
        // +1 for the slot vacated by the field
        // newIndex will be one less than the actual
        ++newIndex;
      }

      let fieldIdx = -1;
      let rowIdx = -1;
      let found = false;
      for (let row of form.rows) {
        ++rowIdx;

        for (let colIdx = 0; colIdx < row.length; ++colIdx) {
          ++fieldIdx;
          if (newIndex == fieldIdx) {
            if (colIdx == 0) {
              form.rows.splice(rowIdx, 0, [field]);
            } else {
              row.splice(colIdx, 0, field);
            }

            found = true;
            break;
          }
        }

        if (found) {
          break;
        }
      }

      if (!found) {
        form.rows.push([field]);
      }

      if (oldRow.length == 1) {
        let oldRowIdx = form.rows.indexOf(oldRow);
        form.rows.splice(oldRowIdx, 1);
      } else if (evt.moved.newIndex > evt.moved.oldIndex) {
        oldRow.splice(oldRow.indexOf(field), 1);
      } else {
        oldRow.splice(oldRow.lastIndexOf(field), 1);
      }

      save();
    };

    function addError(msg) {
      toast.add({ severity: "error", detail: msg, life: 5000 });
    }

    function validate() {
      if (!ctx.selectedCard) {
        return true;
      }

      let valid = true;
      if (ctx.selectedCard == form) {
        if (!form.caption || form.caption.trim().length == 0) {
          addError("Form title is required");
          valid = false;
        }

        if (!form.name || form.name.trim().length == 0) {
          addError("Form name is required");
          valid = false;
        }
      } else {
        let field = ctx.selectedCard;
        if (!field.caption || field.caption.trim().length == 0) {
          addError("Field label is required");
          valid = false;
        }

        if (!field.udn || field.udn.trim().length == 0) {
          addError("Field name is required");
          valid = false;
        }

        let fmd = fieldsRegistry.getField(field.type);
        if (!fmd) {
          addError("Unknown field type: " + field.type);
          valid = false;
        } else if (typeof fmd.validate == "function") {
          let result = fmd.validate(field);
          if (!result.status) {
            addError(result.error);
            valid = false;
          }
        }
      }

      return valid;
    }

    let save = function (cb) {
      if (!validate()) {
        return;
      }

      if (ctx.selectedCard && ctx.selectedCard != form) {
        let field = ctx.selectedCard;
        let row = form.rows[field.$rowIdx];

        if (field.$sameRowAsLastField) {
          if (row[0] == field && field.$rowIdx > 0) {
            let prevRow = form.rows[field.$rowIdx - 1];
            row.forEach(col => prevRow.push(col));
            form.rows.splice(field.$rowIdx, 1);
          }
        } else if (row[0] != field) {
          let newRow = [];
          let colIdx = row.indexOf(field);
          for (let idx = colIdx; idx < row.length; ++idx) {
            newRow.push(row[idx]);
          }

          row.splice(colIdx, row.length - colIdx);
          form.rows.splice(field.$rowIdx + 1, 0, newRow);
        }
      }

      ctx.saving = true;
      emit("save", {
        onSave: function (result) {
          ctx.saving = false;

          if (result.status != true) {
            if (typeof cb == 'function') {
              cb(false);
            }

            return;
          }

          if (ctx.selectedCard) {
            ctx.selectedCard.$saved = true;
          }

          if (ctx.selectedCard == form) {
            formNameWatcher && formNameWatcher();
          }

          copy = "";
          ctx.selectedCard = null;
          ctx.subForm = false;
          if (typeof cb == 'function') {
            cb(true);
          }
        },
      });
    };

    let edit = function (card) {
      copy = JSON.stringify(card);
      ctx.selectedCard = card;
      ctx.subForm = (form != card && card.type == 'subForm');
    };

    let copyField = function (field, idx) {
      let newField = JSON.parse(JSON.stringify(field));
      newField["$saved"] = false;
      newField["name"] = newField["udn"] = newField["caption"] = null;

      let newRow = [newField];
      form.rows.splice(idx + 1, 0, newRow);
      edit(newField);
    };

    let remove = function (field) {
      confirm.require({
        message: "Are you sure you want to delete the field?",
        header: "Confirm Delete",
        icon: "pi pi-exclamation-triangle",
        acceptClass: "p-button-danger",
        accept: () => {
          let rowIdx = field.$rowIdx;
          let row = form.rows[rowIdx];

          let colIdx = row.indexOf(field);
          row.splice(colIdx, 1);
          if (row.length == 0) {
            form.rows.splice(rowIdx, 1);
          }

          ctx.saving = true;
          emit("save", {
            onSave: function (result) {
              ctx.saving = false;
              if (result.status != true) {
                alert("Failed to save. Reload the form");
              }
            },
          });
        },
        reject: () => { },
      });
    };

    let cancel = function (field) {
      if (field.$saved != false) {
        Object.assign(field, JSON.parse(copy));
      } else {
        let row = form.rows[field.$rowIdx];
        if (row.length == 1) {
          form.rows.splice(field.$rowIdx, 1);
        } else {
          row.splice(row.indexOf(field), 1);
        }
      }

      copy = "";
      ctx.selectedCard = null;
      ctx.subForm = false;
    };

    let goBackToMainForm = function () {
      emit("return-to-main");
    };

    let onReturnToMainForm = function () {
      copy = "";
      ctx.selectedCard = null;
      ctx.subForm = false;
    };

    let onSubFormSave = function (data) {
      emit("save", data);
    };

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

    let titleRef = ref(null);
    watch(titleRef, () => {
      if (titleRef.value == null) {
        return;
      }

      setTimeout(() => titleRef.value.$el.focus(), 100);
    });


    let changeFieldType = function (field, type) {
      let oldType = field.type;
      let oldDefValue = field.defaultValue;

      field.type = type.name
      if (field.type == 'stringTextField') {
        field.defaultValue = field.defaultValue && field.defaultValue.value;
      }

      save(
        status => {
          if (status) {
            ctx.selectedCard = field;
            setTimeout(() => ctx.selectedCard = null, 100);
          } else {
            field.type = oldType;
            field.defaultValue = oldDefValue;
          }
        }
      );
    }

    return {
      form,
      fields,
      ctx,
      fieldTypes,
      addField,
      addFieldMenu,
      toggleAddFieldMenu,
      edit,
      copyField,
      save,
      remove,
      cancel,
      fieldMetadata,
      hierarchy,
      goBackToMainForm,
      onReturnToMainForm,
      onSubFormSave,
      onMoveField,
      titleRef,
      changeFieldType
    };
  },
};
</script>

<style scoped>
.form-canvas {
  max-width: 640px;
  margin: auto;
  padding: 15px 0px;
}

.form-canvas > div {
  width: 100%;
}

.form-canvas .buttons button {
  margin-right: 0.5em;
}

.form-canvas .buttons:before {
  content: " ";
  display: block;
  border-top: 1px solid #dee2e6;
  margin: 1rem 0;
}

.form-canvas .buttons:after {
  content: "";
  display: table;
  clear: both;
  overflow: auto;
}

.form-canvas .buttons .right {
  float: right;
}

.form-canvas .details-card .p-card-content {
  padding: 0 !important;
}

.form-canvas .draggable-item {
  position: relative;
}

.form-canvas .draggable-item .grip {
  display: none;
  position: absolute;
  top: 0px;
  left: calc(50% - 9px);
  cursor: grab;
  font-weight: 700;
}

.form-canvas .draggable-item:hover .grip {
  display: initial;
}

.form-canvas >>> .p-inline-message {
  margin-top: 0.5rem;
  width: max-content;
}

.form-canvas >>> .p-field > label {
  display: inline-block;
  max-width: 100%;
  font-weight: 700;
}

.form-canvas .p-field-checkbox label,
.form-canvas .p-field-radiobutton label {
  font-weight: 400;
}

.form-canvas .add-field :deep(.p-menu) {
  height: 300px;
  width: 220px;
  overflow-y: scroll;
}

:deep(.add-field-menu) {
  height: 300px;
  width: 220px;
  overflow-y: scroll;
}
</style>
