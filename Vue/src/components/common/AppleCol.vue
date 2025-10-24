<template>
  <div class="apple-col" :class="colClass" :style="colStyle">
    <slot></slot>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'AppleCol',
  props: {
    span: {
      type: [Number, String],
      default: 24
    },
    xs: {
      type: [Number, String],
      default: null
    },
    sm: {
      type: [Number, String],
      default: null
    },
    md: {
      type: [Number, String],
      default: null
    },
    lg: {
      type: [Number, String],
      default: null
    },
    xl: {
      type: [Number, String],
      default: null
    },
    offset: {
      type: [Number, String],
      default: 0
    },
    pull: {
      type: [Number, String],
      default: 0
    },
    push: {
      type: [Number, String],
      default: 0
    }
  },
  setup(props) {
    const colClass = computed(() => {
      const classes = ['apple-col']

      // 基础span
      if (props.span !== 24) {
        classes.push(`apple-col--span-${props.span}`)
      }

      // 响应式断点
      if (props.xs !== null) {
        classes.push(`apple-col--xs-${props.xs}`)
      }
      if (props.sm !== null) {
        classes.push(`apple-col--sm-${props.sm}`)
      }
      if (props.md !== null) {
        classes.push(`apple-col--md-${props.md}`)
      }
      if (props.lg !== null) {
        classes.push(`apple-col--lg-${props.lg}`)
      }
      if (props.xl !== null) {
        classes.push(`apple-col--xl-${props.xl}`)
      }

      // 定位
      if (props.offset > 0) {
        classes.push(`apple-col--offset-${props.offset}`)
      }
      if (props.pull > 0) {
        classes.push(`apple-col--pull-${props.pull}`)
      }
      if (props.push > 0) {
        classes.push(`apple-col--push-${props.push}`)
      }

      return classes.join(' ')
    })

    const colStyle = computed(() => {
      const style = {}

      // 计算flex-basis
      const calculateFlexBasis = (span) => {
        return `${(span / 24) * 100}%`
      }

      if (props.span !== 24) {
        style.flexBasis = calculateFlexBasis(props.span)
      }

      return style
    })

    return {
      colClass,
      colStyle
    }
  }
}
</script>

<style scoped>
.apple-col {
  flex: 1;
  max-width: 100%;
}

/* 基础span */
.apple-col--span-1 { flex: 0 0 4.16666667%; max-width: 4.16666667%; }
.apple-col--span-2 { flex: 0 0 8.33333333%; max-width: 8.33333333%; }
.apple-col--span-3 { flex: 0 0 12.5%; max-width: 12.5%; }
.apple-col--span-4 { flex: 0 0 16.66666667%; max-width: 16.66666667%; }
.apple-col--span-5 { flex: 0 0 20.83333333%; max-width: 20.83333333%; }
.apple-col--span-6 { flex: 0 0 25%; max-width: 25%; }
.apple-col--span-7 { flex: 0 0 29.16666667%; max-width: 29.16666667%; }
.apple-col--span-8 { flex: 0 0 33.33333333%; max-width: 33.33333333%; }
.apple-col--span-9 { flex: 0 0 37.5%; max-width: 37.5%; }
.apple-col--span-10 { flex: 0 0 41.66666667%; max-width: 41.66666667%; }
.apple-col--span-11 { flex: 0 0 45.83333333%; max-width: 45.83333333%; }
.apple-col--span-12 { flex: 0 0 50%; max-width: 50%; }
.apple-col--span-13 { flex: 0 0 54.16666667%; max-width: 54.16666667%; }
.apple-col--span-14 { flex: 0 0 58.33333333%; max-width: 58.33333333%; }
.apple-col--span-15 { flex: 0 0 62.5%; max-width: 62.5%; }
.apple-col--span-16 { flex: 0 0 66.66666667%; max-width: 66.66666667%; }
.apple-col--span-17 { flex: 0 0 70.83333333%; max-width: 70.83333333%; }
.apple-col--span-18 { flex: 0 0 75%; max-width: 75%; }
.apple-col--span-19 { flex: 0 0 79.16666667%; max-width: 79.16666667%; }
.apple-col--span-20 { flex: 0 0 83.33333333%; max-width: 83.33333333%; }
.apple-col--span-21 { flex: 0 0 87.5%; max-width: 87.5%; }
.apple-col--span-22 { flex: 0 0 91.66666667%; max-width: 91.66666667%; }
.apple-col--span-23 { flex: 0 0 95.83333333%; max-width: 95.83333333%; }
.apple-col--span-24 { flex: 0 0 100%; max-width: 100%; }

/* 定位 */
.apple-col--offset-1 { margin-left: 4.16666667%; }
.apple-col--offset-2 { margin-left: 8.33333333%; }
.apple-col--offset-3 { margin-left: 12.5%; }
.apple-col--offset-4 { margin-left: 16.66666667%; }
.apple-col--offset-5 { margin-left: 20.83333333%; }
.apple-col--offset-6 { margin-left: 25%; }
.apple-col--offset-7 { margin-left: 29.16666667%; }
.apple-col--offset-8 { margin-left: 33.33333333%; }
.apple-col--offset-9 { margin-left: 37.5%; }
.apple-col--offset-10 { margin-left: 41.66666667%; }
.apple-col--offset-11 { margin-left: 45.83333333%; }
.apple-col--offset-12 { margin-left: 50%; }

.apple-col--pull-1 { position: relative; right: 4.16666667%; }
.apple-col--pull-2 { position: relative; right: 8.33333333%; }
.apple-col--pull-3 { position: relative; right: 12.5%; }
.apple-col--pull-4 { position: relative; right: 16.66666667%; }
.apple-col--pull-5 { position: relative; right: 20.83333333%; }
.apple-col--pull-6 { position: relative; right: 25%; }

.apple-col--push-1 { position: relative; left: 4.16666667%; }
.apple-col--push-2 { position: relative; left: 8.33333333%; }
.apple-col--push-3 { position: relative; left: 12.5%; }
.apple-col--push-4 { position: relative; left: 16.66666667%; }
.apple-col--push-5 { position: relative; left: 20.83333333%; }
.apple-col--push-6 { position: relative; left: 25%; }

/* 响应式断点 */
@media (max-width: 575px) {
  .apple-col--xs-1 { flex: 0 0 4.16666667%; max-width: 4.16666667%; }
  .apple-col--xs-2 { flex: 0 0 8.33333333%; max-width: 8.33333333%; }
  .apple-col--xs-3 { flex: 0 0 12.5%; max-width: 12.5%; }
  .apple-col--xs-4 { flex: 0 0 16.66666667%; max-width: 16.66666667%; }
  .apple-col--xs-5 { flex: 0 0 20.83333333%; max-width: 20.83333333%; }
  .apple-col--xs-6 { flex: 0 0 25%; max-width: 25%; }
  .apple-col--xs-7 { flex: 0 0 29.16666667%; max-width: 29.16666667%; }
  .apple-col--xs-8 { flex: 0 0 33.33333333%; max-width: 33.33333333%; }
  .apple-col--xs-9 { flex: 0 0 37.5%; max-width: 37.5%; }
  .apple-col--xs-10 { flex: 0 0 41.66666667%; max-width: 41.66666667%; }
  .apple-col--xs-11 { flex: 0 0 45.83333333%; max-width: 45.83333333%; }
  .apple-col--xs-12 { flex: 0 0 50%; max-width: 50%; }
  .apple-col--xs-13 { flex: 0 0 54.16666667%; max-width: 54.16666667%; }
  .apple-col--xs-14 { flex: 0 0 58.33333333%; max-width: 58.33333333%; }
  .apple-col--xs-15 { flex: 0 0 62.5%; max-width: 62.5%; }
  .apple-col--xs-16 { flex: 0 0 66.66666667%; max-width: 66.66666667%; }
  .apple-col--xs-17 { flex: 0 0 70.83333333%; max-width: 70.83333333%; }
  .apple-col--xs-18 { flex: 0 0 75%; max-width: 75%; }
  .apple-col--xs-19 { flex: 0 0 79.16666667%; max-width: 79.16666667%; }
  .apple-col--xs-20 { flex: 0 0 83.33333333%; max-width: 83.33333333%; }
  .apple-col--xs-21 { flex: 0 0 87.5%; max-width: 87.5%; }
  .apple-col--xs-22 { flex: 0 0 91.66666667%; max-width: 91.66666667%; }
  .apple-col--xs-23 { flex: 0 0 95.83333333%; max-width: 95.83333333%; }
  .apple-col--xs-24 { flex: 0 0 100%; max-width: 100%; }
}

@media (min-width: 576px) and (max-width: 767px) {
  .apple-col--sm-1 { flex: 0 0 4.16666667%; max-width: 4.16666667%; }
  .apple-col--sm-2 { flex: 0 0 8.33333333%; max-width: 8.33333333%; }
  .apple-col--sm-3 { flex: 0 0 12.5%; max-width: 12.5%; }
  .apple-col--sm-4 { flex: 0 0 16.66666667%; max-width: 16.66666667%; }
  .apple-col--sm-5 { flex: 0 0 20.83333333%; max-width: 20.83333333%; }
  .apple-col--sm-6 { flex: 0 0 25%; max-width: 25%; }
  .apple-col--sm-7 { flex: 0 0 29.16666667%; max-width: 29.16666667%; }
  .apple-col--sm-8 { flex: 0 0 33.33333333%; max-width: 33.33333333%; }
  .apple-col--sm-9 { flex: 0 0 37.5%; max-width: 37.5%; }
  .apple-col--sm-10 { flex: 0 0 41.66666667%; max-width: 41.66666667%; }
  .apple-col--sm-11 { flex: 0 0 45.83333333%; max-width: 45.83333333%; }
  .apple-col--sm-12 { flex: 0 0 50%; max-width: 50%; }
  .apple-col--sm-13 { flex: 0 0 54.16666667%; max-width: 54.16666667%; }
  .apple-col--sm-14 { flex: 0 0 58.33333333%; max-width: 58.33333333%; }
  .apple-col--sm-15 { flex: 0 0 62.5%; max-width: 62.5%; }
  .apple-col--sm-16 { flex: 0 0 66.66666667%; max-width: 66.66666667%; }
  .apple-col--sm-17 { flex: 0 0 70.83333333%; max-width: 70.83333333%; }
  .apple-col--sm-18 { flex: 0 0 75%; max-width: 75%; }
  .apple-col--sm-19 { flex: 0 0 79.16666667%; max-width: 79.16666667%; }
  .apple-col--sm-20 { flex: 0 0 83.33333333%; max-width: 83.33333333%; }
  .apple-col--sm-21 { flex: 0 0 87.5%; max-width: 87.5%; }
  .apple-col--sm-22 { flex: 0 0 91.66666667%; max-width: 91.66666667%; }
  .apple-col--sm-23 { flex: 0 0 95.83333333%; max-width: 95.83333333%; }
  .apple-col--sm-24 { flex: 0 0 100%; max-width: 100%; }
}

@media (min-width: 768px) and (max-width: 991px) {
  .apple-col--md-1 { flex: 0 0 4.16666667%; max-width: 4.16666667%; }
  .apple-col--md-2 { flex: 0 0 8.33333333%; max-width: 8.33333333%; }
  .apple-col--md-3 { flex: 0 0 12.5%; max-width: 12.5%; }
  .apple-col--md-4 { flex: 0 0 16.66666667%; max-width: 16.66666667%; }
  .apple-col--md-5 { flex: 0 0 20.83333333%; max-width: 20.83333333%; }
  .apple-col--md-6 { flex: 0 0 25%; max-width: 25%; }
  .apple-col--md-7 { flex: 0 0 29.16666667%; max-width: 29.16666667%; }
  .apple-col--md-8 { flex: 0 0 33.33333333%; max-width: 33.33333333%; }
  .apple-col--md-9 { flex: 0 0 37.5%; max-width: 37.5%; }
  .apple-col--md-10 { flex: 0 0 41.66666667%; max-width: 41.66666667%; }
  .apple-col--md-11 { flex: 0 0 45.83333333%; max-width: 45.83333333%; }
  .apple-col--md-12 { flex: 0 0 50%; max-width: 50%; }
  .apple-col--md-13 { flex: 0 0 54.16666667%; max-width: 54.16666667%; }
  .apple-col--md-14 { flex: 0 0 58.33333333%; max-width: 58.33333333%; }
  .apple-col--md-15 { flex: 0 0 62.5%; max-width: 62.5%; }
  .apple-col--md-16 { flex: 0 0 66.66666667%; max-width: 66.66666667%; }
  .apple-col--md-17 { flex: 0 0 70.83333333%; max-width: 70.83333333%; }
  .apple-col--md-18 { flex: 0 0 75%; max-width: 75%; }
  .apple-col--md-19 { flex: 0 0 79.16666667%; max-width: 79.16666667%; }
  .apple-col--md-20 { flex: 0 0 83.33333333%; max-width: 83.33333333%; }
  .apple-col--md-21 { flex: 0 0 87.5%; max-width: 87.5%; }
  .apple-col--md-22 { flex: 0 0 91.66666667%; max-width: 91.66666667%; }
  .apple-col--md-23 { flex: 0 0 95.83333333%; max-width: 95.83333333%; }
  .apple-col--md-24 { flex: 0 0 100%; max-width: 100%; }
}

@media (min-width: 992px) and (max-width: 1199px) {
  .apple-col--lg-1 { flex: 0 0 4.16666667%; max-width: 4.16666667%; }
  .apple-col--lg-2 { flex: 0 0 8.33333333%; max-width: 8.33333333%; }
  .apple-col--lg-3 { flex: 0 0 12.5%; max-width: 12.5%; }
  .apple-col--lg-4 { flex: 0 0 16.66666667%; max-width: 16.66666667%; }
  .apple-col--lg-5 { flex: 0 0 20.83333333%; max-width: 20.83333333%; }
  .apple-col--lg-6 { flex: 0 0 25%; max-width: 25%; }
  .apple-col--lg-7 { flex: 0 0 29.16666667%; max-width: 29.16666667%; }
  .apple-col--lg-8 { flex: 0 0 33.33333333%; max-width: 33.33333333%; }
  .apple-col--lg-9 { flex: 0 0 37.5%; max-width: 37.5%; }
  .apple-col--lg-10 { flex: 0 0 41.66666667%; max-width: 41.66666667%; }
  .apple-col--lg-11 { flex: 0 0 45.83333333%; max-width: 45.83333333%; }
  .apple-col--lg-12 { flex: 0 0 50%; max-width: 50%; }
  .apple-col--lg-13 { flex: 0 0 54.16666667%; max-width: 54.16666667%; }
  .apple-col--lg-14 { flex: 0 0 58.33333333%; max-width: 58.33333333%; }
  .apple-col--lg-15 { flex: 0 0 62.5%; max-width: 62.5%; }
  .apple-col--lg-16 { flex: 0 0 66.66666667%; max-width: 66.66666667%; }
  .apple-col--lg-17 { flex: 0 0 70.83333333%; max-width: 70.83333333%; }
  .apple-col--lg-18 { flex: 0 0 75%; max-width: 75%; }
  .apple-col--lg-19 { flex: 0 0 79.16666667%; max-width: 79.16666667%; }
  .apple-col--lg-20 { flex: 0 0 83.33333333%; max-width: 83.33333333%; }
  .apple-col--lg-21 { flex: 0 0 87.5%; max-width: 87.5%; }
  .apple-col--lg-22 { flex: 0 0 91.66666667%; max-width: 91.66666667%; }
  .apple-col--lg-23 { flex: 0 0 95.83333333%; max-width: 95.83333333%; }
  .apple-col--lg-24 { flex: 0 0 100%; max-width: 100%; }
}

@media (min-width: 1200px) {
  .apple-col--xl-1 { flex: 0 0 4.16666667%; max-width: 4.16666667%; }
  .apple-col--xl-2 { flex: 0 0 8.33333333%; max-width: 8.33333333%; }
  .apple-col--xl-3 { flex: 0 0 12.5%; max-width: 12.5%; }
  .apple-col--xl-4 { flex: 0 0 16.66666667%; max-width: 16.66666667%; }
  .apple-col--xl-5 { flex: 0 0 20.83333333%; max-width: 20.83333333%; }
  .apple-col--xl-6 { flex: 0 0 25%; max-width: 25%; }
  .apple-col--xl-7 { flex: 0 0 29.16666667%; max-width: 29.16666667%; }
  .apple-col--xl-8 { flex: 0 0 33.33333333%; max-width: 33.33333333%; }
  .apple-col--xl-9 { flex: 0 0 37.5%; max-width: 37.5%; }
  .apple-col--xl-10 { flex: 0 0 41.66666667%; max-width: 41.66666667%; }
  .apple-col--xl-11 { flex: 0 0 45.83333333%; max-width: 45.83333333%; }
  .apple-col--xl-12 { flex: 0 0 50%; max-width: 50%; }
  .apple-col--xl-13 { flex: 0 0 54.16666667%; max-width: 54.16666667%; }
  .apple-col--xl-14 { flex: 0 0 58.33333333%; max-width: 58.33333333%; }
  .apple-col--xl-15 { flex: 0 0 62.5%; max-width: 62.5%; }
  .apple-col--xl-16 { flex: 0 0 66.66666667%; max-width: 66.66666667%; }
  .apple-col--xl-17 { flex: 0 0 70.83333333%; max-width: 70.83333333%; }
  .apple-col--xl-18 { flex: 0 0 75%; max-width: 75%; }
  .apple-col--xl-19 { flex: 0 0 79.16666667%; max-width: 79.16666667%; }
  .apple-col--xl-20 { flex: 0 0 83.33333333%; max-width: 83.33333333%; }
  .apple-col--xl-21 { flex: 0 0 87.5%; max-width: 87.5%; }
  .apple-col--xl-22 { flex: 0 0 91.66666667%; max-width: 91.66666667%; }
  .apple-col--xl-23 { flex: 0 0 95.83333333%; max-width: 95.83333333%; }
  .apple-col--xl-24 { flex: 0 0 100%; max-width: 100%; }
}
</style>