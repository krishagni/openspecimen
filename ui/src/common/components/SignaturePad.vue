
<template>
  <div class="os-signature" v-show="editMode || imageData || imageSrc">
    <canvas ref="canvasEl" v-show="editMode"></canvas>

    <div class="read" v-if="!editMode">
      <img :src="imageData" v-if="imageData" />
      <img :src="imageSrc"  v-else-if="imageSrc" />
    </div>

    <div class="actions" v-if="editMode">
      <input type="button" value="Save"   @click="save" />
      <input type="button" value="Clear"  @click="clear" />
      <input type="button" value="Cancel" @click="cancel" />
    </div>
    <div class="actions" v-else>
      <input type="button" value="Edit" @click="edit" />
      <input type="button" value="Remove" @click="remove"/>
    </div>
  </div>
  <div class="os-signature-add" v-show="!editMode && !imageData && !imageSrc" >
    <Button left-icon="plus" label="Add" @click="edit" /> 
  </div>
</template>

<script>
import SignaturePad from 'signature_pad';

import Button from '@/common/components/Button.vue';

import alertSvc from '@/common/services/Alerts.js';

export default {
  props: ['name', 'modelValue', 'headers', 'uploader', 'imageUrl'],

  components: {
    Button
  },

  data() {
    return {
      editMode: false,

      imageData: ''
    }
  },

  mounted() {
    this.signaturePad = new SignaturePad(this.$refs.canvasEl, {backgroundColor: 'rgb(255, 255, 255)'});
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    imageSrc: function() {
      if (this.inputValue && this.imageUrl) {
        return this.imageUrl(this.inputValue);
      }

      return null;
    }
  },

  methods: {
    edit: function() {
      this.signaturePad.clear();
      this.editMode = true;
    },

    remove: function() {
      this.signaturePad.clear();
      this.imageData = null;
      this.$emit('update:modelValue', null);
    },

    save: async function() {
      if (this.signaturePad.isEmpty()) {
        alertSvc.error('No signature was detected');
        return;
      }

      let data = this.signaturePad.toDataURL('image/jpeg');
      if (this.uploader) {
        let value = await this.uploader(data);
        this.$emit('update:modelValue', value);
        this.imageData = data;
        alertSvc.success('Signature saved');
      }

      this.editMode = false;
    },

    clear: function() {
      this.signaturePad.clear();
    },

    cancel: function() {
      this.editMode = false;
    }
  }
}
</script>

<style scoped>

.os-signature {
  padding: 0.5rem 0.75rem;
  border: 1px solid;
  border-color: #ced4da;
  border-radius: 4px;
  width: fit-content;
}

.os-signature .actions {
  padding-top: 0.5rem;
}

.os-signature .actions:before {
  content: '';
  border-top: 1px solid;
  border-color: #ced4da;
  display: block;
  margin: 0rem -0.75rem 0rem -0.75rem;
  padding-bottom: 0.5rem;
}

.os-signature .actions input {
  margin-right: 0.25rem;
  border: 1px solid;
  border-color: #ced4da;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  background: #fff;
}

.os-signature .actions input:hover,
.os-signature-add button.btn:hover {
  background: #0069d9;
  color: #ffffff;
  border-color: #0069d9;
  cursor: pointer;
}

.os-signature-add button.btn {
  background: #fff;
  padding: 0.5rem 0.75rem;
  border: 1px solid;
  border-color: #007bff;
  border-radius: 4px
}

</style>
