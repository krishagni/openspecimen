
<template>
  <ul class="os-key-values os-two-cols">
    <li class="item" v-for="(field, idx) of fields.simple" :key="idx">
      <strong class="key key-sm">{{field.label}}</strong>
      <span class="value value-md">
        <span v-if="!field.value && field.value != 0">
          <span>-</span>
        </span>
        <span v-else>
          <span v-if="field.type == 'fileUpload'">
            <a :href="field.value.url" target="_blank" rel="noopener">
              {{field.value.filename}}
            </a>
          </span>
          <span v-else-if="field.type == 'signature'">
            <img :src="field.value.url">
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
        <span>{{field.value}}</span>
      </template>
    </Section>
  </template>

  <template v-for="(field, idx) of fields.subform" :key="idx">
    <Section>
      <template #title>
        <span>{{field.label}}</span>
      </template>
      <template #content>
        <div class="sf-table">
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
          let collection = exprUtil.getValue(this.object, field.name);
          let values = collection.map(element => field.fields.map(sfField => this.getValue(element, sfField)));
          subformFields.push({...field, value: values});
        } else {
          let value = this.getValue(this.object, field);
          let item = {...field, value: value};
          if (item.type == 'textarea') {
            textAreaFields.push(item);
          } else {
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
        if (value) {
          switch (field.type) {
            case 'fileUpload':
              value.url = http.getUrl('form-files/' + value.fileId +
                '?contentType=' + value.contentType + '&filename=' + value.filename);
              break;

            case 'signature':
              value = {value: value, url: http.getUrl('form-files/' + value)};
              break;
          }
        }
      } else {
        value = exprUtil.getValue(object, field.name); // props.object[field.name];
        if (value === null || value === undefined || value === '') {
          value = '-';
        } else if (field.type == 'user') {
          if (value instanceof Array) {
            value = value.map(user => user.firstName + ' ' + user.lastName).join(', ') || '-';
          } else {
            value = value.firstName + ' ' + value.lastName;
          }
        } else if (field.options instanceof Array) {
          let option = field.options.find((option) => option.value == value);
          if (option) {
            value = option.caption;
          }
        } else if (field.displayValues) {
          let dispValue = field.displayValues[value];
          if (dispValue) {
            value = dispValue;
          }
        }
      }

      return value;
    }
  }
}
</script>

<style scoped>

.sf-table {
  width: 100%;
  padding: 2px 5px;
  overflow-x: auto;
}

</style>
