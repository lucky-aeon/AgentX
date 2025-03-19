<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
:root {
  --color-primary: #4f46e5;
  --color-bg: #f9fafb;
  --color-text: #374151;
  --color-border: #e5e7eb;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  color: var(--color-text);
  background-color: var(--color-bg);
  line-height: 1.5;
}
</style>
