
<template>
  <div>
    <pre> {{ forms }} </pre>
    <pre> {{ records }} </pre>
    <pre> {{ formId }} </pre>
    <pre> {{ formCtxtId }} </pre>

    <!-- ListGroup :list="forms" :selected="selectedForm" @on-item-select="onSelect($event)">
      <template #header>
        <span>Forms</span>
      </template>
      <template #default="slotProps">
        <span>{{slotProps.item.formCaption}}</span>
      </template>
    </ListGroup -->
  </div>
</template>

<script>

// import {reactive, watchEffect} from 'vue';
// import userSvc from '@/administrative/services/User.js';

export default {
  props: ['forms', 'records', 'formId', 'formCtxtId'],

  setup() {
  }
}

</script>
