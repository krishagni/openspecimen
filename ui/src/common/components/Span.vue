
<template>
  <span :class="$attrs['md-type'] && 'md-type'">
    <a v-if="link" :href="link" target="_blank" rel="noopener">
      <span>{{displayText}}</span>
    </a>
    <span v-else>{{displayText}}</span>
  </span>
</template>

<script>
import exprUtil from '@/common/services/ExpressionUtil.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'displayType', 'href', 'form', 'context'],

  created() {
    this.customField = this.$attrs.name && this.$attrs.name.indexOf('.extensionDetail.attrsMap.');
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

    displayText: function() {
      switch (this.displayType) {
        case 'storage-position':
          return this._getStorageLocation(this.inputValue);

        case 'user':
          return this._getUser(this.inputValue);

        case 'specimen-quantity':
          return this._getSpecimenMeasure(this.inputValue, this.$attrs, 'quantity');

        case 'specimen-description':
          return this._getSpecimenDescription(this.inputValue, this.$attrs);

        case 'date':
        case 'datePicker':
          return this._getDate(this.inputValue);

        case 'datetime':
          return this._getDate(this.inputValue, true);

        case 'multiselect':
          return (this.inputValue || []).join(', ');
      }

      if (this.customField) {
        const displayValue = exprUtil.eval(this.form, this.$attrs.name + '$displayValue');
        if (displayValue) {
          return displayValue;
        }
      }

      return this.inputValue || '-';
    },

    link: function() {
      if (typeof this.href == 'function') {
        let ctx = this.form || this.context || {};
        let link = this.href(ctx.formData || ctx);
        if (link.indexOf('#') == 0) {
          link = this.$ui.ngServer + link;
        }

        return link;
      } else if (this.displayType == 'storage-position') {
        if (this.inputValue && typeof this.inputValue == 'object' && +this.inputValue.id > 0) {
          return this.$ui.ngServer + '#/containers/' + this.inputValue.id + '/locations';
        }
      }

      return this.href;
    }
  },

  methods: {
    getDisplayValue: function() {
      return this.displayText;
    },

    _getStorageLocation: function(value) {
      let result = value;

      if (value && typeof value == 'object') {
        let position = value;
        result = position.name;
        if (position.mode == 'TWO_D') {
          result += ' (' + position.positionY + ', ' + position.positionX + ')';
        } else if (position.mode == 'LINEAR') {
          result += ' (' + position.position + ')';
        }
      }

      return result || this.$t('specimens.not_stored');
    },

    _getUser: function(value) {
      let result = value;
      if (value && typeof value == 'object') {
        let user = value;
        result = user.firstName;
        if (user.lastName) {
          if (result) {
            result += ' ';
          }

          result += user.lastName;
        }
      }

      return result || '-';
    },

    _getSpecimenMeasure: function(value, attrs, measure) {
      if (value == null || value == undefined) {
        return '-';
      }

      const specimen = exprUtil.eval(this.form || this.context || {}, attrs.specimen || 'specimen');
      const unit = util.getSpecimenMeasureUnit(specimen, measure || 'quantity');
      return value + ' ' + unit;
    },

    _getSpecimenDescription: function(value, attrs) {
      const ns = this.$t('pvs.not_specified');
      const specimen = value || {};
      const detailed = attrs.detailed == 'true' || attrs.detailed == true;

      let result = '';
      if (specimen.lineage == 'New' || detailed) {
        if (specimen.pathology && specimen.pathology != ns) {
          result += specimen.pathology + ' ';
        }

        result += specimen.type;

        if (specimen.specimenClass == 'Tissue' && specimen.anatomicSite && specimen.anatomicSite != ns) {
          result += ' ' + this.$t('specimens.extracted_from', {anatomicSite: specimen.anatomicSite});
        }

        if (specimen.specimenClass == 'Fluid' && specimen.collectionContainer && specimen.collectionContainer != ns) {
          result += ' ' + this.$t('specimens.collected_in', {container: specimen.collectionContainer});
        }
      } else if (specimen.lineage == 'Derived') {
        result += specimen.lineage + ' ' + specimen.type;
      } else if (specimen.lineage == 'Aliquot') {
        result += specimen.lineage;
      }

      return result;
    },

    _getDate: function(value, showTime) {
      if (value instanceof Date || typeof value == 'number') {
        return showTime ? this.$filters.dateTime(value) : this.$filters.date(value);
      }

      return value;
    }
  }
}
</script>

<style scoped>

.md-type {
  display: inline-block;
  padding: 2px 0px;
}

</style>
