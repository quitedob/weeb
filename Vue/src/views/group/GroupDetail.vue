<template>
  <div>
    <h1>Group Detail Page (ID: {{ groupId }})</h1>
    <p>This is a placeholder for the group detail page.</p>
    <router-link to="/groups">Back to Groups</router-link>
  </div>
</template>

<script setup>
import { defineProps } from 'vue';

const props = defineProps({
  groupId: String
});
</script>

<style scoped>
div {
  padding: 20px;
}
</style>
