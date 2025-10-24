/**
 * Element Plus to Apple Style Migration Helper
 * Provides utilities to help migrate from Element Plus to Apple-style components
 */

import Icons from './appleIcons.js'

// Create simple Apple-style components to replace common Element Plus components
export const createAppleRow = (gutter = 20) => {
  return {
    template: `
      <div class="apple-row" style="display: flex; flex-wrap: wrap; margin: -${gutter/2}px;">
        <slot></slot>
      </div>
    `
  }
}

export const createAppleCol = (span = 24) => {
  const width = (span / 24) * 100
  return {
    template: `
      <div class="apple-col" style="
        flex: 0 0 ${width}%;
        max-width: ${width}%;
        padding: ${gutter/2}px;
        box-sizing: border-box;
      ">
        <slot></slot>
      </div>
    `,
    props: ['span', 'xs', 'sm', 'md', 'lg']
  }
}

export const createAppleCard = () => {
  return {
    template: `
      <div class="apple-card" style="
        background: rgba(255, 255, 255, 0.8);
        backdrop-filter: blur(20px);
        -webkit-backdrop-filter: blur(20px);
        border: 1px solid rgba(0, 0, 0, 0.1);
        border-radius: 16px;
        padding: 20px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
        transition: all 0.3s ease;
      ">
        <slot></slot>
      </div>
    `
  }
}

export const createAppleIcon = (iconName) => {
  return {
    template: `<div v-html="iconHtml"></div>`,
    props: ['size', 'color'],
    computed: {
      iconHtml() {
        const icon = Icons[iconName]
        if (icon) {
          return icon.render({
            size: this.size || 16,
            color: this.color || 'currentColor'
          })
        }
        return ''
      }
    }
  }
}

// Icon mapping from Element Plus to Apple Icons
export const iconMapping = {
  'User': 'User',
  'Document': 'Document',
  'ChatDotRound': 'ChatDotRound',
  'Connection': 'Connection',
  'Lock': 'Lock',
  'UserSolid': 'UserSolid',
  'Management': 'Management',
  'CheckCircle': 'CheckCircle',
  'Warning': 'Warning',
  'Plus': 'Plus',
  'Edit': 'Edit',
  'Delete': 'Delete',
  'Search': 'Search',
  'View': 'Eye',
  'Star': 'Star',
  'Calendar': 'Calendar',
  'Clock': 'Clock',
  'Refresh': 'Refresh',
  'Download': 'Download',
  'Upload': 'Upload',
  'Share': 'Share',
  'Link': 'Link',
  'Setting': 'Settings',
  'ArrowLeft': 'ArrowLeft',
  'ArrowRight': 'ArrowRight',
  'ArrowUp': 'ArrowUp',
  'ArrowDown': 'ArrowDown',
  'Close': 'Close',
  'Check': 'Check',
  'VideoPlay': 'VideoPlay',
  'Pointer': 'Pointer',
  'Copy': 'Copy',
  'Fold': 'Fold',
  'Expand': 'Expand',
  'Remove': 'Remove',
  'Collection': 'Collection'
}

// Helper function to get Apple icon HTML
export const getAppleIcon = (elementPlusIconName, props = {}) => {
  const appleIconName = iconMapping[elementPlusIconName]
  if (appleIconName && Icons[appleIconName]) {
    return Icons[appleIconName].render(props)
  }
  return ''
}

// Batch replacement script for templates
export const replaceElementComponents = (template) => {
  return template
    // Replace el-row
    .replace(/<el-row[^>]*:gutter="([^"]*)"[^>]*>/g, (match, gutter) => {
      return `<div class="apple-row" style="display: flex; flex-wrap: wrap; margin: -${parseInt(gutter)/2}px;">`
    })
    .replace(/<\/el-row>/g, '</div>')

    // Replace el-col
    .replace(/<el-col[^>]*:span="([^"]*)"[^>]*>/g, (match, span) => {
      const width = (parseInt(span) / 24) * 100
      return `<div class="apple-col" style="flex: 0 0 ${width}%; max-width: ${width}%; padding: 10px; box-sizing: border-box;">`
    })
    .replace(/<\/el-col>/g, '</div>')

    // Replace el-card
    .replace(/<el-card[^>]*>/g, '<div class="apple-card" style="background: rgba(255, 255, 255, 0.8); backdrop-filter: blur(20px); border: 1px solid rgba(0, 0, 0, 0.1); border-radius: 16px; padding: 20px;">')
    .replace(/<\/el-card>/g, '</div>')

    // Replace el-icon
    .replace(/<el-icon><([^>]*)\/><\/el-icon>/g, (match, iconName) => {
      return `<span class="apple-icon">${getAppleIcon(iconName.trim())}</span>`
    })
}