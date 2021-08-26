<template>
  <div class="os-subform">
    <div class="table-wrapper" v-if="inputValue && inputValue.length > 0">
      <table>
        <thead>
          <tr>
            <th v-for="(field, idx) of fields" :key="idx">
              <span v-html="field.label"></span>
            </th>
            <th class="actioncol">&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(sfRowData, sfRdIdx) of inputValue" :key="sfRdIdx">
            <td v-for="(field, cidx) of fields" :key="cidx">
              <component :is="field.component" v-bind="field" v-model="sfRowData[field.name]"
                @update:model-value="handleInput(sfRowData, sfRdIdx, field)">
              </component>
              <div v-if="v$.inputValue[sfRdIdx][field.name] && v$.inputValue[sfRdIdx][field.name].$error">
                <os-inline-message>{{errorMessages[sfRdIdx][field.name]}}</os-inline-message>
              </div>
            </td>
            <td class="actioncol">
              <Button class="inline-button" left-icon="times" @click="removeSfRow(sfRdIdx)"/>
            </td>
          </tr>
        </tbody>
      </table> 
    </div>

    <Button class="inline-button" left-icon="plus"
      :label="inputValue && inputValue.length > 0 ? 'Add Another' : 'Add'"
      @click="addSfRow"
    />
  </div>
</template>

<script>

import useVuelidate from '@vuelidate/core'
import fieldFactory from '@/common/services/FieldFactory.js';

import BooleanCheckbox from '@/common/components/BooleanCheckbox.vue';
import Button from '@/common/components/Button.vue';
import Checkbox from '@/common/components/Checkbox.vue';
import DatePicker from '@/common/components/DatePicker.vue';
import Divider from '@/common/components/Divider.vue';
import Dropdown from '@/common/components/Dropdown.vue';
import FileUpload from '@/common/components/FileUpload.vue';
import InlineMessage from '@/common/components/InlineMessage.vue';
import InputNumber from '@/common/components/InputNumber.vue';
import InputText from '@/common/components/InputText.vue';
import Label from '@/common/components/Label.vue';
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';
import Password from '@/common/components/Password.vue';
import PvDropdown from '@/common/components/PvDropdown.vue';
import RadioButton from '@/common/components/RadioButton.vue';
import SignaturePad from '@/common/components/SignaturePad.vue';
import SiteDropdown from '@/common/components/SiteDropdown.vue';
import StorageContainerDropdown from '@/common/components/StorageContainerDropdown.vue';
import Subform from '@/common/components/Subform.vue';
import Textarea from '@/common/components/Textarea.vue';
import UserDropdown from '@/common/components/UserDropdown.vue';

export default {
  props: ['modelValue', 'fields'],

  components: {
    Dropdown,
    MultiSelectDropdown,
    DatePicker,
    InputText,
    Password,
    InputNumber,
    RadioButton,
    BooleanCheckbox,
    Checkbox,
    Textarea,
    FileUpload,
    SignaturePad,
    UserDropdown,
    PvDropdown,
    SiteDropdown,
    StorageContainerDropdown,
    Subform,
    Button,
    'os-label': Label,
    'os-inline-message': InlineMessage,
    'os-divider': Divider
  },

  setup() {
    return {
      v$: useVuelidate(),
    };
  },

  data() {
    return {
    }
  },

  validations() {
    if (!this.inputValue) {
      return {inputValue: []};
    }

    return {
      inputValue: this.inputValue.map(() => fieldFactory.getValidationRules(this.fields))
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue ? this.modelValue : [];
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    errorMessages: function() {
      let result = [];
      if (!this.inputValue) {
        return result;
      }

      for (let rowIdx = 0; rowIdx < this.inputValue.length; ++rowIdx) {
        result.push({});

        for (let field of this.fields) {
          let validators = this.v$.inputValue &&
            this.v$.inputValue[rowIdx] &&
            this.v$.inputValue[rowIdx][field.name];

          if (!validators) {
            continue;
          }

          for (let rule in field.validations) {
            if (validators[rule] && validators[rule].$invalid) {
              result[rowIdx][field.name] = field.validations[rule].message;
              break;
            }
          }
        }
      }

      return result;
    }
  },

  methods: {
    handleInput: function(sfRowData, sfRdIdx, field) {
      this.$emit('input', {field: field, data: sfRowData})
      if (this.v$.inputValue[sfRdIdx][field.name]) {
        this.v$.inputValue[sfRdIdx][field.name].$touch();
      }
    },

    validate: function() {
      this.v$.$touch();
      return !this.v$.$invalid;
    },

    addSfRow: function() {
      this.inputValue = this.inputValue || [];
      this.inputValue.push({});
    },

    removeSfRow: function(idx) {
      this.inputValue.splice(idx, 1);
    }
  }
}
</script>

<style scoped>

.os-subform table {
  border-collapse: collapse;
  width: 100%;
}

.os-subform table th {
  padding: 0.5rem 0.75rem;
  min-width: 175px;
  color: #707070;
  background: #efefef;
  border-bottom: 1px solid #dee2e6;
  font-weight: normal;
  text-align: left;
}

.os-subform table td {
  padding: 0.5rem 0.75rem;
}

.os-subform .table-wrapper {
  width: 100%;
  overflow-y: auto;
  margin-bottom: 1.25rem;
}

.os-subform table .actioncol {
  min-width: 5rem;
  max-width: 5rem;
}

.os-subform table .actioncol :deep(button) {
  background: #fff;
}
</style>
