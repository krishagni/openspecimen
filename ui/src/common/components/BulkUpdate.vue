<template>
  <div class="form">
    <div v-for="(field, idx) of fields" :key="idx">
      <div class="field-wrapper">
        <div class="field">
          <os-label>
            <span v-t="'common.select_field'"></span>
          </os-label>

          <os-dropdown v-model="field.fm" :list-source="fieldsLs"
            @update:modelValue="toggleFieldSelection(field, idx)" />
        </div>

        <div class="value" v-if="field.fm">
          <os-label>{{field.fm.label}}</os-label>

          <div class="input">
            <div v-if="!field.blank">
              <component v-bind="field.fm" v-model="field.value" :is="field.fm.component" />
            </div>
            <div>
              <os-boolean-checkbox v-model="field.blank" :inline-label-code="'common.set_blank_value'"
                @update:modelValue="toggleSetBlankValue(field)" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="footer">
      <os-button primary :label="$t('common.buttons.submit')" @click="update" />

      <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
    </div>
  </div>
</template>

<script>
import exprUtil     from '@/common/services/ExpressionUtil.js';
import fieldFactory from '@/common/services/FieldFactory.js';
import i18n         from '@/common/services/I18n.js';
import util         from '@/common/services/Util.js';

export default {
  props: ['dict', 'objectPrefix', 'exclusionList'],

  data() {
    return {
      fields: [{}]
    }
  },

  computed: {
    fieldsLs: function() {
      const result = [];

      const exclusionList = this.exclusionList || [];
      const objectPrefix  = this.objectPrefix;
      for (let field of this.dict || []) {
        const {name, type} = field;
        if (!name || (objectPrefix && name.indexOf(objectPrefix) != 0) || exclusionList.indexOf(name) != -1) {
          continue;
        }

        if (type == 'note') {
          continue;
        }

        field = util.clone(field);
        if (!field.component) {
          field.component = fieldFactory.getComponent(field.type);
        }

        if (!field.label) {
          if (field.labelCode) {
            field.label = i18n.msg(field.labelCode);
          } else if (field.inlineLabel) {
            field.label = field.inlineLabel;
          } else if (field.inlineLabelCode) {
            field.label = i18n.msg(field.inlineLabelCode);
          } else {
            field.label = field.name;
          }
        }

        field.id = field.name;
        result.push(field);
      }

      return {options: result, displayProp: 'label'};
    }
  },

  methods: {
    toggleFieldSelection: function(field, idx) {
      if (!field.fm) {
        this.fields.splice(idx, 1);
      }

      if (this.fields.length == 0 || (field.fm && idx == this.fields.length - 1)) {
        this.fields.push({});
      }
    },

    toggleSetBlankValue: function(field) {
      if (field.blank) {
        field.value = null;
      } else {
        delete field.value;
      }
    },

    update: function() {
      const detail = {};
      for (let field of this.fields) {
        if (!field.fm) {
          continue;
        }

        if (field.value || field.value == null || field.value == 0) {
          exprUtil.setValue(detail, field.fm.name, field.value);
        }
      }

      this.$emit('update', detail);
    },

    cancel: function() {
      this.$emit('cancel');
    }
  }
}
</script>

<style scoped>
.form {
  width: 80%;
  margin: auto;
}

.field-wrapper {
  padding: 1.25rem;
  border-bottom: 1px solid #ddd;
}

.field {
  margin-bottom: 1rem;
}

.value .input > div {
  margin-bottom: 1rem;
}

.footer {
  padding: 1.25rem;
}

.footer :deep(.btn) {
  margin-right: 0.5rem;
}
</style>
