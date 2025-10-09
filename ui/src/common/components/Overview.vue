
<template>
  <ul :class="listClass">
    <li class="item" v-for="(field, idx) of fields.simple" :key="idx">
      <strong class="key key-sm">
        <span>{{label(field)}}</span>
      </strong>
      <span class="value value-md">
        <span v-if="(!field.value && field.value != 0) || field.value == '-'">
          <span>-</span>
        </span>
        <span v-else>
          <span v-if="field.type == 'fileUpload'">
            <a :href="field.value.url" target="_blank" rel="noopener">
              <span>{{field.value.filename}}</span>
            </a>
          </span>
          <span v-else-if="field.type == 'signature'">
            <img :src="field.value.url">
          </span>
          <span v-else-if="field.hrefLink">
            <a :href="field.hrefLink" target="_blank" rel="noopener">
              <span>{{field.value}}</span>
            </a>
          </span>
          <span v-else-if="field.type == 'text'">
            <os-html :content="field.value" />
          </span>
          <span v-else-if="field.type == 'component'">
            <component :is="field.component" v-bind="field.value" />
          </span>
          <span v-else>
            <span>{{field.value}}</span>
          </span>
        </span>

        <div class="help">
          <slot :name="field.name"></slot>
        </div>
      </span>
    </li>
  </ul>

  <template v-for="(field, idx) of fields.textArea" :key="idx">
    <Section>
      <template #title>
        <span>{{label(field)}}</span>
      </template>
      <template #content>
        <os-html class="os-textarea" v-if="field.value" :content="field.value" />
      </template>
    </Section>
  </template>

  <template v-for="(field, idx) of fields.sections" :key="idx">
    <Section>
      <template #title>
        <span>{{label(field)}}</span>
      </template>
      <template #content>
        <component :is="field.component" v-bind="field.value" :form="object" :context="object" />
      </template>
    </Section>
  </template>

  <template v-for="(field, idx) of fields.subform" :key="idx">
    <Section>
      <template #title>
        <span>{{label(field)}}</span>
      </template>
      <template #content>
        <div class="os-sf-table">
          <table class="os-table muted-header os-border">
            <thead>
              <tr>
                <th v-for="sfField in field.fields" :key="sfField.name">
                  <span>{{label(sfField)}}</span>
                </th>
              </tr>
            </thead>
            <tbody class="os-table-body">
              <tr class="row" v-for="(rowValue, rowIdx) of field.value" :key="rowIdx">
                <td class="col" v-for="(colValue, colIdx) of rowValue" :key="colIdx">
                  <span v-if="(!colValue && colValue != 0) || colValue == '-'">
                    <span>-</span>
                  </span>
                  <span v-else>
                    <span v-if="field.fields[colIdx].type == 'fileUpload'">
                      <a :href="colValue.url" target="_blank" rel="noopener">
                        <span>{{colValue.filename}}</span>
                      </a>
                    </span>
                    <span v-else-if="field.fields[colIdx].type == 'signature'">
                      <img :src="colValue.url">
                    </span>
                    <span v-else-if="colValue.hrefLink && colValue.value">
                      <a :href="colValue.hrefLink" target="_blank" rel="noopener">
                        <span>{{colValue.value}}</span>
                      </a>
                    </span>
                    <span v-else-if="field.fields[colIdx].type == 'text' || field.fields[colIdx].type == 'textarea'">
                      <os-html :content="colValue" />
                    </span>
                    <span v-else>
                      <span>{{colValue}}</span>
                    </span>
                  </span>
                </td>
              </tr>
              <tr class="row" v-if="!field.value || field.value.length == 0">
                <td class="col" :colspan="field.fields.length">
                  <span v-t="'common.none'">None</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </Section>
  </template>
</template>

<script>
import Section from '@/common/components/Section.vue';
import exprUtil from '@/common/services/ExpressionUtil.js';
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

export default {
  props: ['object', 'schema', 'columns', 'bgCol', 'colType', 'showEmptyFields'],

  components: {
    Section
  },

  computed: {
    listClass: function() {
      const classes = ['os-key-value-pairs'];
      classes.push(this.columns == 1 ? 'os-one-col' : 'os-two-cols');
      if (util.isTrue(this.bgCol)) {
        classes.push('bg-col');
      } else if (this.colType) {
        classes.push(this.colType + '-col');
      }

      return classes;
    },

    fields: function() {
      let simpleFields = [], textAreaFields = [], subformFields = [], sectionFields = [];
      for (let field of this.schema) {
        if (field.dataEntry || field.type == 'note') {
          continue;
        }

        if (field.showInOverviewWhen) {
          if (!exprUtil.eval(this.object, field.showInOverviewWhen)) {
            continue;
          }
        }

        if (field.type == 'subform') {
          // convert into 2-d array of values: [ [1, "abc"], [2, "xyz"], [3, "abcxyz"] ]
          let values = this.getValue(this.object, field) || [];
          if (!this.showEmptyFields &&
              (values == undefined || values == null || values == '-' ||
                (values instanceof Array && values.length == 0))
             ) {
            continue;
          }

          if (values != '-') {
            values = values.map(
              element => field.fields.map(
                sfField => {
                  let sfv = this.getValue(element, sfField);
                  if (sfField.type == 'textarea' || sfField.type == 'text') {
                    sfv = util.linkify(sfv);
                  }

                  let hrefLink = null;
                  if (typeof sfField.href == 'function') {
                    hrefLink = sfField.href(element);
                  }

                  return hrefLink ? {hrefLink, value: sfv} : sfv;
                }
              )
            );
          }

          subformFields.push({...field, value: values});
        } else {
          let value = this.getValue(this.object, field);
          if (!this.showEmptyFields && (value == null || value == undefined || value == '-')) {
            continue;
          }

          let item = {...field, value: value};
          if (item.type == 'textarea') {
            item.value = util.linkify(item.value);
            textAreaFields.push(item);
          } else {
            if (typeof item.href == 'function') {
              item.hrefLink = item.href(this.object);
            } else if (item.type == 'text') {
              item.value = util.linkify(item.value);
            }

            if (item.showInSeparateSection) {
              sectionFields.push(item);
            } else {
              simpleFields.push(item);
            }
          }
        }
      }

      return {simple: simpleFields, textArea: textAreaFields, subform: subformFields, sections: sectionFields};
    }
  },

  methods: {
    label: function(field) {
      if (field.labelCode || field.inlineLabelCode) {
        return this.$t(field.labelCode || field.inlineLabelCode);
      } else if (field.label || field.inlineLabel) {
        return field.label || field.inlineLabel;
      } else {
        return 'Unknown';
      }
    },

    getValue: function(object, field) {
      let value = undefined;
      if (field.name && field.name.indexOf('calc') == 0 && field.displayExpr) {
        return exprUtil.eval({...object, fns: util.fns()}, field.displayExpr);
      }

      if (field.type == 'fileUpload' || field.type == 'signature') {
        if (typeof field.value == 'function') {
          value = field.value(object);
        } else {
          value = exprUtil.getValue(object, field.name);
        }

        if (value && typeof field.href == 'function') {
          value = {filename: value, url: field.href(object, value)};
        } else if (value != null && value != undefined && value != '') {
          switch (field.type) {
            case 'fileUpload':
              value.url = http.getUrl('form-files/' + value.fileId +
                '?contentType=' + value.contentType + '&filename=' + value.filename);
              break;

            case 'signature':
              value = {value: value, url: http.getUrl('form-files/' + value)};
              break;
          }
        } else {
          value = '-';
        }
      } else if (typeof field.value == 'function') {
        value = field.value(object);
      } else {
        value = exprUtil.getValue(object, field.name); // props.object[field.name];
        if (field.displayValues) {
          const dispValue = field.displayValues[value];
          if (typeof dispValue == 'function') {
            value = dispValue();
          } else {
            value = dispValue;
          }
        }

        if (value === null || value === undefined || value === '') {
          value = '-';
        } else if (field.type == 'user') {
          if (value instanceof Array) {
            value = value.map(user => user.firstName + ' ' + user.lastName).join(', ') || '-';
          } else if (value.firstName || value.lastName) {
            value = value.firstName + ' ' + value.lastName;
          }
        } else if (field.type == 'site') {
          if (value instanceof Array) {
            value = value.map(site => typeof site == 'object' ? site.name : site).join(', ');
          } else {
            value = typeof value == 'object' ? value.name : value;
          }
        } else if (field.type == 'date' || (field.type == 'datePicker' && field.showTime != true)) {
          if (field.dateOnly) {
            if (typeof value == 'string' && value.length == 10 && value[4] == '-') {
              value = new Date(value);
            }

            value = util.getLocalDate(value);
          }

          if (typeof value != 'string' || ('' + parseInt(value)) == value) {
            value = this.$filters.date(value);
          }
        } else if (field.type == 'datetime' || (field.type == 'datePicker' && field.showTime == true)) {
          if (typeof value != 'string' || ('' + parseInt(value)) == value) {
            value = this.$filters.dateTime(value);
          }
        } else if (field.options instanceof Array) {
          let option = field.options.find((option) => option.value == value);
          if (option) {
            if (option.captionCode) {
              value = this.$t(option.captionCode);
            } else {
              value = option.caption || option.name;
            }
          }
        } else if (field.type == 'dropdown') {
          const ls = field.listSource || {};
          if (typeof value == 'object') {
            if (ls.displayProp) {
              value = value[ls.displayProp];
            }
          } else if (ls.options && ls.options.length > 0 && typeof ls.options[0] == 'object') {
            for (let option of ls.options) {
              if (option[ls.selectProp] == value) {
                value = option[ls.displayProp];
                break;
              }
            }
          }
        } else if (field.type == 'multiselect' || (field.type == 'pv' && field.multiple == true)) {
          const ls = field.listSource || {};
          if (value instanceof Array) {
            if (value.length == 0) {
              value = null;
            } else {
              value = value.map((e) => (typeof e == 'object' && ls.displayProp) ? e[ls.displayProp] : e).join(', ');
            }
          }
        } else if (field.type == 'storage-position') {
          if (value && typeof value == 'object') {
            let position = value;
            value = position.name;
            if (position.displayName) {
              value = position.displayName + ' (' + position.name + ')';
            }

            if (position.mode == 'TWO_D' && position.positionY && position.positionX) {
              value += ' (' + position.positionY + ', ' + position.positionX + ')';
            } else if (position.mode == 'LINEAR' && position.position >= 0) {
              value += ' (' + position.position + ')';
            }
          }

          value = value || 'Not Stored';
        } else if (field.type == 'specimen-quantity' || field.type == 'specimen-measure') {
          const specimen = exprUtil.eval(object || {}, field.specimen || 'specimen');
          const unit = util.getSpecimenMeasureUnit(specimen, field.measure || 'quantity');
          if (value != 0 && (value >= 1e6 || value <= 1e-5)) {
            value = value.toExponential();
          }

          value += ' ' + unit;
        } else if (field.type == 'specimen-type') {
          const specimen = exprUtil.eval(object || {}, field.specimen || 'specimen');
          if (specimen.specimenClass) {
            value += ' (' + specimen.specimenClass + ')';
          }
        } else if (field.type == 'booleanCheckbox' || field.type == 'toggle-checkbox') {
          if (value == true || value == 'true') {
            value = this.$t('common.yes');
          } else if (value == false || value == 'false') {
            value = this.$t('common.no');
          }
        }
      }

      if (value === null || value === undefined || value === '') {
        value = '-';
      }

      return value;
    }
  }
}
</script>

<style scoped>

.os-sf-table {
  width: 100%;
  padding: 0.125rem 0.250rem;
  overflow-x: auto;
}

.os-sf-table img {
  height: 150px;
  width: 300px;
}

.os-textarea {
  word-break: break-word;
  white-space: break-spaces;
}

.os-key-value-pairs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 0.5rem;
  list-style-type: none;
  padding: 0rem;
  margin: 1.25rem 0rem;
}

/* Width of a medium column is 250px for key and 330px for value = 580px */
.os-key-value-pairs.md-col {
  grid-template-columns: repeat(auto-fit, minmax(580px, 1fr));
}

/* Width of a big column is 300px for key and 400px for value = 700px */
.os-key-value-pairs.bg-col {
  grid-template-columns: repeat(auto-fit, minmax(700px, 1fr));
}

/* Width of a XL column is 400px for key and 530px for value = 930px */
.os-key-value-pairs.xl-col {
  grid-template-columns: repeat(auto-fit, minmax(930px, 1fr));
}

.os-key-value-pairs.os-one-col,
.os-key-value-pairs.md-col.os-one-col,
.os-key-value-pairs.bg-col.os-one-col,
.os-key-value-pairs.xl-col.os-one-col {
  grid-template-columns: 1fr;
}

.os-key-value-pairs .item {
  display: flex;
  align-items: center;
  min-width: 350px;
}

.os-key-value-pairs.md-col .item {
  min-width: 580px;
}

.os-key-value-pairs.bg-col .item {
  min-width: 700px;
}

.os-key-value-pairs.bg-col .item {
  min-width: 930px;
}

.os-key-value-pairs .item .key {
  color: #707070;
  font-weight: normal;
  width: 150px;
}

.os-key-value-pairs.md-col .item .key {
  width: 250px;
}

.os-key-value-pairs.bg-col .item .key {
  width: 300px;
}

.os-key-value-pairs.xl-col .item .key {
  width: 400px;
}

.os-key-value-pairs .item .value {
  flex: 1;
  word-wrap: break-word;
  max-width: 100%;
}
</style>
