
<template>
  <div>
    <div class="os-signature" v-show="imageData || imageSrc">
      <div class="read">
        <img :src="imageData" :tabindex="tabOrder" v-if="imageData" />
        <img :src="imageSrc"  :tabindex="tabOrder" v-else-if="imageSrc" />
      </div>

      <div class="actions" v-if="!disabled">
        <input type="button" :value="$t('common.buttons.edit')" @click="edit" />
        <input type="button" :value="$t('common.buttons.remove')" @click="remove"/>
      </div>
    </div>

    <div class="os-signature-add" v-show="!imageData && !imageSrc" >
      <Button left-icon="plus" :label="$t('common.buttons.add')" :tabindex="tabOrder"
        @click="edit" v-show="!disabled" />
      <span v-t="'common.none'" v-show="disabled"></span>
    </div>

    <os-dialog ref="dialog" :closable="false" @opened="initSignaturePad">
      <template #header>
        <span v-t="'common.draw_signature'">Draw Signature</span>
      </template>
      <template #content>
        <div class="os-canvas-container">
          <canvas ref="canvasEl"></canvas>
        </div>
      </template>
      <template #footer>
        <os-button text      :label="$t('common.buttons.cancel')" @click="cancel" />
        <os-button secondary :label="$t('common.buttons.clear')"  @click="clear" />
        <os-button primary   :label="$t('common.buttons.done')"   @click="save" />
      </template>
    </os-dialog>
  </div>
</template>

<script>
import SignaturePad from 'signature_pad';

import Button from '@/common/components/Button.vue';

import alertSvc from '@/common/services/Alerts.js';

export default {
  props: ['modelValue', 'headers', 'uploader', 'imageUrl', 'tabOrder', 'disabled'],

  components: {
    Button
  },

  data() {
    return {
      imageData: ''
    }
  },

  mounted() {
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
      this.signaturePad = null;
      this.$refs.dialog.open();
    },

    initSignaturePad: function() {
      this.signaturePad = new SignaturePad(this.$refs.canvasEl, {backgroundColor: 'rgb(255, 255, 255)'});
      this._resizeCanvas();
    },

    remove: function() {
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

      this.$refs.dialog.close();
      this.signaturePad = null;
    },

    clear: function() {
      this.signaturePad.clear();
    },

    cancel: function() {
      this.$refs.dialog.close();
      this.signaturePad = null;
    },

    getDisplayValue: function() {
      return this.inputValue;
    },

    _resizeCanvas: function() {
      const canvas = this.$refs.canvasEl;
      const ratio = Math.max(window.devicePixelRatio || 1, 1);

      canvas.width = canvas.offsetWidth * ratio;
      canvas.height = canvas.offsetHeight * ratio;
      canvas.getContext("2d").scale(ratio, ratio);

      this.signaturePad.clear();
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

.os-signature img {
  width: 300px;
  height: 150px;
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

.os-canvas-container {
  margin: auto;
  padding: 0.5rem 0.75rem;
  border: 1px solid;
  border-color: #ced4da;
  border-radius: 4px;
  width: fit-content;
}

.os-canvas-container canvas {
  width: 450px;
  height: 225px;
}
</style>
