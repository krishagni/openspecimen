<template>
  <div class="login-container">
    <div class="title" v-if="$slots.title">
      <slot name="title" />
    </div>
  
    <div class="login-box">
      <div class="box-title">
        <slot name="box-title" />
      </div>

      <img class="logo" :src="logo" v-if="logo" />

      <os-form class="user-form" ref="form" :schema="schema" :data="data">
        <template #message v-if="message">
          <slot name="message" />
        </template>

        <div class="actions">
          <div class="primary" v-if="$slots['primary-action'] && $slots['primary-action']().length > 0">
            <slot name="primary-action" />
          </div>
          <div class="secondary">
            <slot name="secondary-actions" />
          </div>
        </div>
      </os-form>
    </div>
  </div>
</template>

<script>
export default {
  props: ['schema', 'data', 'message', 'logo'],

  methods: {
    validate: function() {
      return this.$refs.form.validate();
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #205081, #2d67a3);
  padding-top: 1.25rem;
}

.login-box {
  padding: 0.5rem;
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  width: 420px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.title :deep(h3) {
  color: #fff;
  font-size: 24px;
  font-weight: 500;
  line-height: 1.1;
}

.box-title {
  width: 90%;
}

.user-form {
  width: 90%;
  max-width: 90%;
}

.user-form :deep(.row) {
  margin-left: -0.5rem;
  margin-right: -0.5rem;
}

.user-form :deep(.row:last-child) {
  margin-top: 1rem;
}

.user-form :deep(.row .field) {
  padding: 0.5rem;
}

.user-form :deep(.p-float-label label) {
  line-height: 1.1
}

.user-form :deep(.p-divider) {
  display: none;
}

.user-form :deep(.os-input-text .p-float-label .p-inputtext),
.user-form :deep(.os-input-password .p-float-label .p-inputtext),
.user-form :deep(.os-dropdown .p-float-label .p-dropdown) {
  border: 1px solid #1f1f1f;
  border-radius: 4px;
  padding: 13px 15px;
}

.user-form :deep(.os-input-text .p-float-label label),
.user-form :deep(.os-input-password .p-float-label label),
.user-form :deep(.os-dropdown .p-float-label label) {
  padding: 0px 8px;
  left: 8px;
  color: #444746;
}

.user-form :deep(.os-input-text .p-float-label .p-inputtext:not(:enabled:focus) ~ label),
.user-form :deep(.os-dropdown .p-float-label .p-dropdown ~ label) {
  color: #444746;
}

.user-form :deep(.os-input-text .p-float-label .p-inputtext:enabled:focus ~ label),
.user-form :deep(.os-input-password .p-float-label .p-password.p-inputwrapper-focus ~ label),
.user-form :deep(.os-dropdown .p-float-label .p-dropdown.p-inputwrapper-focus ~ label) {
  color: #007bff;
}

.user-form :deep(.os-dropdown .p-float-label .p-dropdown .p-inputtext),
.user-form :deep(.os-dropdown .p-float-label .p-dropdown.p-inputwrapper-filled .p-inputtext) {
  padding: 0;
}

.user-form :deep(.p-float-label:has(input:focus) label), 
.user-form :deep(.p-float-label:has(input.p-filled) label),
.user-form :deep(.p-float-label:has(input:-webkit-autofill) label),
.user-form :deep(.p-float-label:has(textarea:focus) label),
.user-form :deep(.p-float-label:has(textarea.p-filled) label),
.user-form :deep(.p-float-label:has(.p-inputwrapper-focus) label),
.user-form :deep(.p-float-label:has(.p-inputwrapper-filled) label) {
  top: 2px;
  z-index: 1;
  background: #fff;
  width: max-content;
}

.user-form :deep(.p-dropdown-trigger) {
  width: auto;
}

.user-form :deep(.os-dropdown .p-float-label .p-dropdown.p-inputwrapper-focus),
.user-form :deep(.os-input-text .p-float-label .p-inputtext:enabled:focus),
.user-form :deep(.os-input-password .p-float-label .p-inputtext:enabled:focus) {
  border: 1px solid #007bff;
}

.logo {
  height: 100px;
  width: 100px;
  border-radius: 50%;
}

.user-form .actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.user-form .actions > .primary {
  width: 100%;
  margin-bottom: 1rem;
}

.user-form .actions > .primary :deep(.btn) {
  width: 100%;
}

.user-form .actions > .secondary {
  display: flex;
  justify-content: center;
  align-items: center;
}

.user-form .actions > .secondary :deep(.btn:last-child) {
  margin-right: 0;
}

.user-form :deep(.os-message.p-message) {
  margin: 0;
}
</style>
