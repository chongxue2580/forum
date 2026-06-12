<template>
  <div class="tags-container">
    <h1>标签云</h1>
    <tag-list :tags="popularTags" />
  </div>
</template>

<script>
import { defineComponent } from 'vue'
import { mapState } from 'vuex'
import TagList from '../components/TagList.vue'

export default defineComponent({
  name: 'TagsView',
  components: {
    TagList
  },
  computed: {
    ...mapState(['popularTags'])
  },
  async created() {
    if (this.popularTags.length === 0) {
      await this.$store.dispatch('fetchPopularTags')
    }
  }
})
</script>

<style scoped>
.tags-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

h1 {
  margin-bottom: 1.5rem;
  color: #333;
  font-size: 1.75rem;
}
</style> 