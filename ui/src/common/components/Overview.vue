
<template>
  <ul class="os-key-values os-two-cols">
    <li class="item" v-for="(field, idx) of fields.simple" :key="idx">
      <strong class="key key-sm">{{field.label}}</strong>
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
          <span v-else-if="field.type == 'text'">
            <span v-html="field.value"></span>
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
        <span>{{field.label}}</span>
      </template>
      <template #content>
        <span v-if="field.value" v-html="field.value"></span>
      </template>
    </Section>
  </template>

  <template v-for="(field, idx) of fields.subform" :key="idx">
    <Section>
      <template #title>
        <span>{{field.label}}</span>
      </template>
      <template #content>
        <div class="os-sf-table">
          <table class="os-table muted-header os-border">
            <thead>
              <tr>
                <th v-for="sfField in field.fields" :key="sfField.name">
                  <span>{{sfField.label}}</span>
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
  props: ['object', 'schema'],

  components: {
    Section
  },

  computed: {
    fields: function() {
      let simpleFields = [], textAreaFields = [], subformFields = [];
      for (let field of this.schema) {
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
            if (item.type == 'text') {
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
            value = option.caption || option.name;
          }
        } else if (field.type == 'dropdown') {
          if (typeof value == 'object') {
            const ls = field.listSource || {};
            if (ls.displayProp) {
              value = value[ls.displayProp];
            }
          }
        }
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
