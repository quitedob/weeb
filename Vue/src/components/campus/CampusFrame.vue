<template>
  <section class="campus">
    <nav aria-label="校园导航" class="campus-actions"><RouterLink to="/campus">校园空间</RouterLink><slot name="nav" /></nav>
    <slot />
  </section>
</template>
<style>
.campus { max-width: 1100px; margin: auto; padding: 24px; color: var(--apple-text-primary); }
.campus h2 { margin: 12px 0 20px; font-size: 28px; }
.campus h3 { margin: 0 0 12px; }
.campus a { color: var(--apple-blue, #007aff); text-decoration: underline; text-underline-offset: 3px; }
.campus p { line-height: 1.65; overflow-wrap: anywhere; }
.campus .campus-card { border: 1px solid var(--apple-border-secondary, #ddd); background: var(--apple-bg-secondary, #fff); border-radius: 16px; padding: 20px; margin: 16px 0; }
.campus .campus-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(280px, 100%), 1fr)); gap: 16px; }
.campus .campus-grid > .campus-card { margin: 0; }
.campus .campus-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin: 12px 0; }
.campus button, .campus input, .campus select, .campus textarea { font: inherit; }
.campus button { cursor: pointer; min-height: 40px; padding: 8px 16px; border: 1px solid var(--apple-border-secondary, #bbb); border-radius: 8px; background: var(--apple-bg-secondary, #fff); color: var(--apple-text-primary); }
.campus button.primary { background: #0067cc; color: #fff; border-color: #0067cc; }
.campus button:disabled { opacity: .55; cursor: not-allowed; }
.campus :is(button, a, input, textarea, select):focus-visible { outline: 3px solid #007aff; outline-offset: 3px; }
.campus label { display: flex; flex-direction: column; gap: 7px; margin: 12px 0; font-weight: 500; }
.campus label.check { flex-direction: row; align-items: center; }
.campus input:not([type=checkbox]), .campus select, .campus textarea { width: 100%; box-sizing: border-box; padding: 10px; border: 1px solid var(--apple-border-secondary, #bbb); border-radius: 8px; color: var(--apple-text-primary); background: var(--apple-bg-primary, #fff); }
.campus textarea { resize: vertical; min-height: 100px; }
.campus .campus-filters { display: flex; flex-wrap: wrap; gap: 16px; align-items: end; }
.campus .campus-filters label { flex: 1; min-width: 130px; }
.campus .muted { color: var(--apple-text-secondary, #555); font-size: 14px; }
.campus .plain-text { white-space: pre-wrap; overflow-wrap: anywhere; }
.campus .error { border-left: 4px solid #555; padding: 12px; background: var(--apple-bg-secondary); }
.campus .badge { display: inline-block; padding: 3px 8px; border-radius: 6px; background: var(--apple-bg-tertiary, #eee); margin-right: 8px; font-size: 13px; }
.campus fieldset { border: 0; padding: 0; margin: 0; min-width: 0; }
.campus summary { cursor: pointer; min-height: 32px; }
@media (max-width: 640px) { .campus { padding: 12px; } .campus h2 { font-size: 24px; } .campus .campus-card { padding: 14px; } }
</style>
