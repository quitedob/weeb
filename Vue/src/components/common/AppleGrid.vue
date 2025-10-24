<template>
  <div class="apple-grid" :class="gridClass" :style="gridStyle">
    <slot></slot>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'AppleGrid',
  props: {
    gutter: {
      type: [Number, Array],
      default: 0
    },
    columns: {
      type: [Number, Object],
      default: null
    },
    justify: {
      type: String,
      default: 'flex-start',
      validator: (value) => ['flex-start', 'flex-end', 'center', 'space-between', 'space-around', 'space-evenly'].includes(value)
    },
    align: {
      type: String,
      default: 'stretch',
      validator: (value) => ['flex-start', 'flex-end', 'center', 'stretch', 'baseline'].includes(value)
    },
    responsive: {
      type: Boolean,
      default: true
    }
  },
  setup(props) {
    const gridClass = computed(() => {
      const classes = ['apple-grid']

      if (props.justify !== 'flex-start') {
        classes.push(`apple-grid--justify-${props.justify}`)
      }

      if (props.align !== 'stretch') {
        classes.push(`apple-grid--align-${props.align}`)
      }

      if (props.responsive) {
        classes.push('apple-grid--responsive')
      }

      return classes.join(' ')
    })

    const gridStyle = computed(() => {
      const style = {}

      // 设置间距
      if (Array.isArray(props.gutter)) {
        style.gap = `${props.gutter[0]}px ${props.gutter[1]}px`
      } else if (props.gutter > 0) {
        style.gap = `${props.gutter}px`
      }

      // 设置列数
      if (props.columns) {
        if (typeof props.columns === 'number') {
          style.gridTemplateColumns = `repeat(${props.columns}, 1fr)`
        } else if (typeof props.columns === 'object') {
          const columns = []
          if (props.columns.xs) columns.push(`repeat(${props.columns.xs}, 1fr)`)
          if (props.columns.sm) columns.push(`repeat(${props.columns.sm}, 1fr)`)
          if (props.columns.md) columns.push(`repeat(${props.columns.md}, 1fr)`)
          if (props.columns.lg) columns.push(`repeat(${props.columns.lg}, 1fr)`)
          if (props.columns.xl) columns.push(`repeat(${props.columns.xl}, 1fr)`)

          if (columns.length > 0) {
            style.gridTemplateColumns = columns.join(' ')
          }
        }
      }

      return style
    })

    return {
      gridClass,
      gridStyle
    }
  }
}
</script>

<style scoped>
.apple-grid {
  display: grid;
  width: 100%;
}

.apple-grid--responsive {
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
}

.apple-grid--justify-flex-start {
  justify-content: flex-start;
}

.apple-grid--justify-flex-end {
  justify-content: flex-end;
}

.apple-grid--justify-center {
  justify-content: center;
}

.apple-grid--justify-space-between {
  justify-content: space-between;
}

.apple-grid--justify-space-around {
  justify-content: space-around;
}

.apple-grid--justify-space-evenly {
  justify-content: space-evenly;
}

.apple-grid--align-flex-start {
  align-items: flex-start;
}

.apple-grid--align-flex-end {
  align-items: flex-end;
}

.apple-grid--align-center {
  align-items: center;
}

.apple-grid--align-stretch {
  align-items: stretch;
}

.apple-grid--align-baseline {
  align-items: baseline;
}

/* 响应式断点 */
@media (max-width: 768px) {
  .apple-grid--responsive {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 769px) and (max-width: 991px) {
  .apple-grid--responsive {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 992px) and (max-width: 1199px) {
  .apple-grid--responsive {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1200px) {
  .apple-grid--responsive {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>