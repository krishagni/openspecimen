
<template>
  <ul class="os-key-values" :class="{'os-one-col': columns == 1, 'os-two-cols': columns != 1}">
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
            <span v-html="field.value"></span>
          </span>
          <span v-else-if="field.type == 'component'">
            <component :is="field.component" v-bind="field.value" />
          </span>
          <span v-else>
            <span>{{field.value}}</span>
          </span>
        </span>
      </span>
    </li>
  </ul>

  <template v-for="(field, idx) of fields.textArea" :key="idx">
    <Section>
      <template #title>
        <span>{{label(field)}}</span>
      </template>
      <template #content>
        <span v-if="field.value" v-html="field.value"></span>
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
                  <span v-if="!colValue && colValue != 0">
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
                    <span v-else-if="field.fields[colIdx].type == 'text' || field.fields[colIdx].type == 'textarea'">
                      <span v-html="colValue"></span>
                    </span>
                    <span v-else>
                      <span>{{colValue}}</span>
                    </span>
                  </span>
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
  props: ['object', 'schema', 'columns'],

  components: {
    Section
  },

  computed: {
    fields: function() {
      let simpleFields = [], textAreaFields = [], subformFields = [];
      for (let field of this.schema) {
        if (field.dataEntry || field.type == 'note') {
          continue;
        }

        if (field.showWhen || field.showInOverviewWhen) {
          if (!exprUtil.eval(this.object, field.showInOverviewWhen || field.showWhen)) {
            continue;
          }
        }

        if (field.type == 'subform') {
          // convert into 2-d array of values: [ [1, "abc"], [2, "xyz"], [3, "abcxyz"] ]
          let values = this.getValue(this.object, field) || [];
          if (values != '-') {
            values = values.map(
              element => field.fields.map(
                sfField => {
                  let sfv = this.getValue(element, sfField);
                  if (sfField.type == 'textarea' || sfField.type == 'text') {
                    sfv = util.linkify(sfv);
                  }

                  return sfv;
                }
              )
            );
          }

          subformFields.push({...field, value: values});
        } else {
          let value = this.getValue(this.object, field);
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

            simpleFields.push(item);
          }
        }
      }

      return {simple: simpleFields, textArea: textAreaFields, subform: subformFields};
    }
  },

  methods: {
    label: function(field) {
      if (field.labelCode) {
        return this.$t(field.labelCode);
      } else if (field.label) {
        return field.label;
      } else {
        return 'Unknown';
      }
    },

    getValue: function(object, field) {
      let value = undefined;

      if (field.source == 'de') {
        value = exprUtil.getValue(object, field.name);
        if (value != null && value != undefined && value != '') {
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
          if (dispValue) {
            value = dispValue;
          }
        }

        if (value === null || value === undefined || value === '') {
          value = '-';
        } else if (field.type == 'user') {
          if (value instanceof Array) {
            value = value.map(user => user.firstName + ' ' + user.lastName).join(', ') || '-';
          } else {
            value = value.firstName + ' ' + value.lastName;
          }
        } else if (field.type == 'site') {
          if (value instanceof Array) {
            value = value.map(site => typeof site == 'object' ? site.name : site).join(', ');
          } else {
            value = typeof value == 'object' ? value.name : value;
          }
        } else if (field.type == 'date' || (field.type == 'datePicker' && field.showTime != true)) {
          value = this.$filters.date(value);
        } else if (field.type == 'datetime' || (field.type == 'datePicker' && field.showTime == true)) {
          value = this.$filters.dateTime(value);
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

</style>
