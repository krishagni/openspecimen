
<template>
  <div class="os-user-avatar">
    <os-avatar :label="text" shape="circle" :class="bgcolor"></os-avatar>
  </div>
</template>

<script>
export default {
  props: ['name'],

  computed: {
    text: function() {
      let result = this.name.trim().toUpperCase().split(/\s+/).reduce(
        (acc, part) => {
          if (part.trim().length > 0) {
            acc += part.trim().charAt(0);
          }

          return acc;
        },
        ''
      );

      return result.length == 0 ? 'U' : result;
    },

    bgcolor: function() {
      let palatte = [
        'orange', 'red', 'maroon', 'light-red',
        'purple', 'violet', 'blue', 'light-blue',
        'teal', 'dark-green'
      ];

      return palatte[(this.text.charCodeAt(0) - 64) % palatte.length];
    }
  }
}
</script>

<style scoped>

.os-user-avatar :deep(.p-avatar) {
  font-weight: bold;
}

.orange {
  background: #ffb900;
  color: #333;
}

.red {
  background: #d83b01;
  color: #fff;
}

.maroon {
  background: #b50e0e;
  color: #fff;
}

.light-red {
  background: #e81123;
  color: #fff;
}

.purple {
  background: #b4009e;
  color: #fff;
}

.violet {
  background: #5c2d91;
  color: #fff;
}

.blue {
  background: #0078d7;
  color: #fff;
}

.light-blue {
  background: #00b4ff;
  color: #333;
}

.teal {
  background: #008272;
  color: #fff;
}

.dark-green {
  background: #107c10;
  color: #fff;
}

</style>
