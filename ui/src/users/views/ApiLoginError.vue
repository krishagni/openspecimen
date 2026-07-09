<template>
  <FormCard ref="form" :schema="{rows: []}" :data="{}" :message="true">
    <template #box-title>
      <h3 v-t="'users.api_user_ui_access_denied_title'"></h3>
    </template>

    <template #message>
      <os-message type="error">
        <span v-t="'users.api_user_ui_access_denied'"></span>
      </os-message>
    </template>
  </FormCard>
</template>

<script>
import FormCard from './FormCard.vue';

export default {
  components: {
    FormCard
  }
}
</script>
