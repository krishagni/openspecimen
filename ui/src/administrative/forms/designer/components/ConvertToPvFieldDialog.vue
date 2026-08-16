<template>
  <Dialog header="Convert to Permissible Value" v-model:visible="model.visible"
    :modal="true" :closable="!model.saving" :style="{width: '36rem'}">
    <div class="p-fluid">
      <div class="p-field">
        <div class="p-field-radiobutton">
          <RadioButton input-id="existing-pv" v-model="model.source" value="existing" />
          <label for="existing-pv">Use an existing permissible value attribute</label>
        </div>
        <div class="p-field-radiobutton">
          <RadioButton input-id="new-pv" v-model="model.source" value="new" />
          <label for="new-pv">Create a new permissible value attribute using the current options</label>
        </div>
      </div>

      <div class="p-field" v-if="model.source == 'existing'">
        <label>Options</label>
        <PvAttributeDropdown v-model="model.attribute" :form-id="form.id" />
      </div>

      <div v-else>
        <div class="p-field">
          <label>Caption</label>
          <InputText v-model="model.caption" />
        </div>
        <div class="p-field">
          <label>Name</label>
          <InputText :model-value="suggestedAttributeName" disabled />
          <small>The final name might include a numeric suffix if this name is already in use.</small>
        </div>
      </div>
    </div>

    <template #footer>
      <Button label="Cancel" class="p-button-text" :disabled="model.saving" @click="cancel" />
      <Button label="Convert" :loading="model.saving" :disabled="!valid" @click="convert" />
    </template>
  </Dialog>
</template>

<script>
import Button from 'primevue/button';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import RadioButton from 'primevue/radiobutton';

import PvAttributeDropdown from './PvAttributeDropdown.vue';
import utility from '../services/Utility.js';

export default {
  components: {
    Button,
    Dialog,
    InputText,
    RadioButton,
    PvAttributeDropdown
  },

  props: {
    visible: Boolean,
    field: Object,
    form: Object,
    saving: Boolean
  },

  emits: ['cancel', 'convert'],

  data() {
    return {
      model: {
        visible: false,
        source: 'existing',
        attribute: null,
        caption: '',
        saving: false
      }
    };
  },

  computed: {
    valid() {
      return this.model.source == 'existing' ?
        !!this.model.attribute :
        !!this.model.caption.trim();
    },

    suggestedAttributeName() {
      return utility.toPvAttributeName(this.form.name, this.model.caption);
    }
  },

  watch: {
    visible(visible) {
      this.model.visible = visible;
      if (visible) {
        this.model.source = 'existing';
        this.model.attribute = null;
        this.model.caption = this.field.caption || '';
      }
    },

    saving(saving) {
      this.model.saving = saving;
    },

    'model.visible'(visible) {
      if (!visible && this.visible) {
        this.$emit('cancel');
      }
    }
  },

  methods: {
    cancel() {
      this.$emit('cancel');
    },

    convert() {
      this.$emit('convert', {
        attribute: this.model.source == 'existing' ? this.model.attribute : null,
        newAttributeCaption: this.model.source == 'new' ? this.model.caption.trim() : null,
        useFormOptions: this.model.source == 'new'
      });
    }
  }
};
</script>
