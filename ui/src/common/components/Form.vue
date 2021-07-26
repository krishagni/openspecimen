
<template>
  <form novalidate>
    <div class="row" v-for="(formRow, rowIdx) of formRows" :key="rowIdx">
      <template v-for="(field, fieldIdx) of formRow" :key="rowIdx + '_' + fieldIdx">
        <div class="field">
          <label>{{field.label}}</label>
          <component :is="field.component" v-bind="field" v-model="ctx.formData[field.name]"
            @update:model-value="$emit('input', {field: field, data: ctx.formData})">
          </component>
        </div>
      </template>
    </div>

    <div class="row">
      <div class="field">
        <slot></slot>
      </div>
    </div>
  </form>
</template>

<script>

import { reactive } from 'vue';

import fieldFactory from '@/common/services/FieldFactory.js';
import utility from '@/common/services/Utility.js';

import Dropdown from '@/common/components/Dropdown.vue';
import InputText from '@/common/components/InputText.vue';
import RadioButton from '@/common/components/RadioButton.vue';
import Textarea from '@/common/components/Textarea.vue';

export default {
   props: ['schema', 'data'],

   components: {
     Dropdown,
     InputText,
     RadioButton,
     Textarea
   },

   emits: ['input'],

   setup(props) {
     let ctx = reactive({
       formData: props.data
     });

     return {
       ctx
     };
   },

   computed: {
     formRows: function() {
       let result = [];

       for (let row of this.schema.rows) {
         let formRow = [];

         for (let field of row.fields) {
           if (field.showWhen) {
             console.log(field.showWhen);
             if (!utility.eval(field.showWhen, this.ctx.formData)) {
               continue;
             }
           }

           let component = fieldFactory.getComponent(field.type);
           formRow.push({...field, component: component});
         }

         if (formRow.length > 0) {
           result.push(formRow);
         }
       }

       return result;
     }
   },

   watch: {
     'ctx.formData': {
       deep: true,

       handler: function(newVal) {
         console.log('Handler: ');
         console.log(newVal);
         console.log ('----');
       }
     }
   }
}

</script>

<style scoped>

.row {
  display: flex;
}

.row .field label {
  font-weight: bold;
}

.row .field {
  flex: 1 1 0;
  padding: 0.5rem 1rem;
}

.row .field :deep(.btn) {
  margin-right: 0.5rem;
}
</style>
