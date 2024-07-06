<template>
  <BlockUI :blocked="saving">
    <Toast />
    <ConfirmDialog />
    <div class="spinner" v-if="loading">
      <ProgressSpinner />
      <div class="hint">Loading...</div>
    </div>
    <div class="designer" v-else>
      <form-canvas :main="form" @save="onSave" @done="onDone"/>
    </div>
  </BlockUI>
</template>

<script>

import { reactive, ref } from "vue";
import BlockUI from "primevue/blockui";
import ProgressSpinner from "primevue/progressspinner";
import Toast from "primevue/toast";
import ConfirmDialog from 'primevue/confirmdialog';
import { useToast } from "primevue/usetoast";
import FormCanvas from "./components/FormCanvas.vue";

import http from "@/common/services/HttpClient.js";
import util from '@/common/services/Util.js';

export default {
  props: ['form-def'],

  components: {
    BlockUI,
    ProgressSpinner,
    Toast,
    ConfirmDialog,
    "form-canvas": FormCanvas,
  },

  setup(props, {emit}) {
    let form = reactive({
      $saved: false,
      rows: [],
    });

    let loading = ref(false);
    loading.value = true;

    let formDef = util.clone(props.formDef);
    for (let row of (formDef.rows || [])) {
      for (let field of row) {
        field.$saved = true;
        if (field.type == 'subForm') {
          for (let sfRow of (field.rows || [])) {
            for (let sfField of sfRow) {
              sfField.$saved = true;
            }
          }
        }
      }
    }

    Object.assign(form, formDef);
    form.$saved = true;
    loading.value = false;

    let saving = ref(false);
    let onSave = function (data) {
      saving.value = true;
      let formObj = JSON.parse(JSON.stringify(form));
      let url = "forms/" + (formObj.id > 0 ? formObj.id : -1);
      http.put(url, form)
        .then(function (resp) {
          setTimeout(function() { saving.value = false; }, 250);
          formObj.id = form.id = resp.id;
          data.onSave({ status: true, id: form.id });
          emit('form-saved', {form: formObj});
        })
        .catch((error) => {
          saving.value = false;
          data.onSave({ status: false });
          showError(error.response);
        });
    };

    let onDone = function () {
      if (window != window.top) {
        window.parent.postMessage('done');
      }
    }

    let showError = function (error) {
      if (error.data) {
        if (error.data instanceof Array) {
          error.data.forEach((err) => addError(err.message));
        } else if (typeof error.data == "object") {
          addError(JSON.stringify(error.data));
        } else {
          addError(error.data);
        }
      } else {
        addError(error.statusText);
      }
    };

    const toast = useToast();
    let addError = (msg) =>
      toast.add({ severity: "error", detail: msg, life: 5000 });

    return {
      loading,
      form,
      onSave,
      onDone,
      saving,
    };
  },
};
</script>
    

<style>
.spinner {
  max-width: 100px;
  margin: auto;
  margin-top: 100px;
}

.spinner .hint {
  text-align: center;
}
</style>

<style scoped>
.designer :deep(.p-card) {
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 2px 6px 2px rgba(60,64,67,.15);
}
</style>
