<script setup>
import { RouterView, RouterLink } from 'vue-router'
</script>

<template>
  <header>
    <nav>
      <div class="logo">GTA V AI Assistant</div>
      <div class="links">
        <RouterLink to="/">Chat Mode</RouterLink>
        <RouterLink to="/agent">Agent Mode</RouterLink>
      </div>
    </nav>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped>
header {
  background-color: var(--gta-dark);
  color: var(--gta-white);
  padding: 1rem 2rem;
}

nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.logo {
  font-weight: bold;
  font-size: 1.2rem;
  letter-spacing: 1px;
}

.links a {
  color: #aaa;
  text-decoration: none;
  margin-left: 20px;
  font-weight: 500;
  transition: color 0.3s;
}

.links a:hover, .links a.router-link-active {
  color: var(--gta-white);
}

main {
  flex: 1;
  overflow: hidden; /* Prevent double scrollbars */
  display: flex;
  flex-direction: column;
}
</style>
