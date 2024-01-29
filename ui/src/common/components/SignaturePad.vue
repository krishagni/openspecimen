
<template>
  <div>
    <div class="os-signature" v-show="editMode || imageData || imageSrc">
      <canvas ref="canvasEl" :tabindex="tabOrder" v-show="editMode"></canvas>

      <div class="read" v-if="!editMode">
        <img :src="imageData" :tabindex="tabOrder" v-if="imageData" />
        <img :src="imageSrc"  :tabindex="tabOrder" v-else-if="imageSrc" />
      </div>

      <div class="actions" v-if="editMode">
        <input type="button" :value="$t('common.buttons.save')"   @click="save" />
        <input type="button" :value="$t('common.buttons.clear')"  @click="clear" />
        <input type="button" :value="$t('common.buttons.cancel')" @click="cancel" />
      </div>
      <div class="actions" v-else>
        <input type="button" :value="$t('common.buttons.edit')" @click="edit" />
        <input type="button" :value="$t('common.buttons.remove')" @click="remove"/>
      </div>
    </div>
    <div class="os-signature-add" v-show="!editMode && !imageData && !imageSrc" >
      <Button left-icon="plus" :label="$t('common.buttons.add')" :tabindex="tabOrder" @click="edit" />
    </div>
  </div>
</template>

<script>
import SignaturePad from 'signature_pad';

import Button from '@/common/components/Button.vue';

import alertSvc from '@/common/services/Alerts.js';

export default {
  props: ['name', 'modelValue', 'headers', 'uploader', 'imageUrl', 'tabOrder'],

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
        alertSvc.error({code: 'common.signature.no_signature'});
        return;
      }

      let data = this.signaturePad.toDataURL('image/jpeg');
      if (this.uploader) {
        let value = await this.uploader(data);
        this.$emit('update:modelValue', value);
        this.imageData = data;
        alertSvc.success({code: 'common.signature.saved'});
      }

      this.editMode = false;
    },

    clear: function() {
      this.signaturePad.clear();
    },

    cancel: function() {
      this.editMode = false;
    },

    getDisplayValue: function() {
      return this.inputValue;
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
  border-radius: 1.125rem;
}

</style>
