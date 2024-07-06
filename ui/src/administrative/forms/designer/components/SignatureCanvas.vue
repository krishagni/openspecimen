<template>
  <canvas ref="canvasEl" v-show="editMode"></canvas>
  <img :src="data" v-show="!editMode" />
  <div v-if="editMode">
    <input type="button" value="Save" @click="save" />
    <input type="button" value="Clear" @click="clear" />
    <input type="button" value="Cancel" @click="cancel" />
  </div>
  <div v-else>
    <input type="button" value="Edit" @click="edit" />
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import SignaturePad from "signature_pad";

export default {
  name: "SignatureCanvas",

  props: {
    data: String,
  },

  emits: ["signatureSaved"],

  setup(props, { emit }) {
    let canvasEl = ref(null);
    let editMode = ref(!props.data);

    let signaturePad = null;
    onMounted(function () {
      signaturePad = new SignaturePad(canvasEl.value);
      if (props.data) {
        signaturePad.fromDataURL(props.data);
      }
    });

    let edit = function () {
      editMode.value = true;
    };

    let save = function () {
      emit("signatureSaved", { data: signaturePad.toDataURL() });
      editMode.value = false;
    };

    let clear = () => {
      signaturePad.clear();
    };

    let cancel = () => {
      editMode.value = false;
    };

    return {
      canvasEl,
      editMode,
      edit,
      save,
      clear,
      cancel,
    };
  },
};
</script>

<style scoped>
canvas,
img {
  height: 150px;
  width: 300px;
  border: 1px solid #ddd;
}

input[type="button"] {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 5px 10px;
  margin-right: 0.5rem;
}
</style>