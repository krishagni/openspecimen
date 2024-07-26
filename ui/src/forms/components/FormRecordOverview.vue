<template>
  <div>
    <ul class="os-key-values bg-col os-one-col" :class="{'vertical': ctx.verticalLayout}">
      <li class="item" v-for="field in ctx.simpleFields" :key="field.udn">
        <strong class="key key-sm strong">
          <span v-html="field.caption"></span>
        </strong>
        <span class="value value-md" v-if="field.type != 'label'">
          <FormFieldValue :field="field" />
        </span>
      </li>
    </ul>

    <template v-for="sf in ctx.sfFields" :key="sf.udn">
      <os-section>
        <template #title>
          <span v-html="sf.caption"></span>
        </template>
        <template #content>
          <div class="os-sf-table">
            <table class="os-table muted-header os-border">
              <thead>
                <tr>
                  <th v-for="field in sf.value[0].fields" :key="field.udn">
                    <span v-html="field.caption"></span>
                  </th>
                </tr>
              </thead>
                <tbody class="os-table-body">
                <tr class="row" v-for="(sfr, rowIdx) in sf.value" :key="rowIdx">
                  <td class="col" v-for="field in sfr.fields" :key="field.udn">
                    <FormFieldValue :field="field" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </os-section>
    </template>
  </div>
</template>

<script>
import FormFieldValue from '@/forms/components/FormFieldValue.vue';

export default {

  props: ['record'],

  components: {
    FormFieldValue
  },

  data() {
    return {
      ctx: { }
    };
  },

  created() {
    this.loadRecord();
  },

  watch: {
    'record': function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.loadRecord();
    }
  },

  methods: {
    loadRecord: function() {
      const record = this.record;
      if (!record) {
        return;
      }

      let longerCaptionFields  = 0;
      let totalFields = 0;

      let simpleFields = [];
      let sfFields = [];

      record.fields.forEach(
        (field) => {
          if (field.type == 'subForm') {
            if (field.value && field.value.length > 0) {
              sfFields.push(field);
            }

            return;
          }
          
          if (field.caption && field.caption.length >= 30) {
            ++longerCaptionFields;
          }

          ++totalFields;
          simpleFields.push(field);
        }
      );

      this.ctx.verticalLayout = (longerCaptionFields * 100 / totalFields >= 30);
      this.ctx.simpleFields = simpleFields;
      this.ctx.sfFields = sfFields;
    }
  }
}
</script>

<style scoped>

.os-sf-table {
  width: 100%;
  overflow: auto;
  padding: 0.125rem 0.250rem;
}

.os-sf-table img {
  height: 150px;
  width: 300px;
}

.os-sf-table td {
  max-width: 300px;
  word-break: break-word;
  white-space: break-spaces;
}

</style>
