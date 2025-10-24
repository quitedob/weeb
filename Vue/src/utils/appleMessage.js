/**
 * Apple-style message utility to replace ElMessage
 * Provides toast notifications with Apple design language
 */

class AppleMessage {
  constructor() {
    this.container = null;
    this.init();
  }

  init() {
    if (typeof document !== 'undefined') {
      this.container = document.createElement('div');
      this.container.className = 'apple-message-container';
      this.container.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 9999;
        pointer-events: none;
      `;
      document.body.appendChild(this.container);
    }
  }

  show(message, type = 'info', duration = 3000) {
    if (!this.container) return;

    const messageEl = document.createElement('div');
    const icon = this.getIcon(type);

    messageEl.className = `apple-message apple-message-${type}`;
    messageEl.style.cssText = `
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-radius: 12px;
      padding: 12px 16px;
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 8px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
      font-size: 14px;
      color: #1d1d1f;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      max-width: 300px;
      pointer-events: auto;
      cursor: pointer;
      transform: translateX(100%);
      transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      opacity: 0;
    `;

    messageEl.innerHTML = `
      <span style="font-size: 16px;">${icon}</span>
      <span style="flex: 1;">${message}</span>
    `;

    this.container.appendChild(messageEl);

    // Trigger animation
    requestAnimationFrame(() => {
      messageEl.style.transform = 'translateX(0)';
      messageEl.style.opacity = '1';
    });

    // Auto remove
    setTimeout(() => {
      messageEl.style.transform = 'translateX(100%)';
      messageEl.style.opacity = '0';
      setTimeout(() => {
        if (messageEl.parentNode) {
          messageEl.parentNode.removeChild(messageEl);
        }
      }, 300);
    }, duration);

    // Click to dismiss
    messageEl.addEventListener('click', () => {
      messageEl.style.transform = 'translateX(100%)';
      messageEl.style.opacity = '0';
      setTimeout(() => {
        if (messageEl.parentNode) {
          messageEl.parentNode.removeChild(messageEl);
        }
      }, 300);
    });
  }

  getIcon(type) {
    const icons = {
      success: '✓',
      error: '✕',
      warning: '⚠',
      info: 'ℹ'
    };
    return icons[type] || icons.info;
  }

  success(message, duration) {
    this.show(message, 'success', duration);
  }

  error(message, duration) {
    this.show(message, 'error', duration);
  }

  warning(message, duration) {
    this.show(message, 'warning', duration);
  }

  info(message, duration) {
    this.show(message, 'info', duration);
  }
}

// Create singleton instance
const appleMessage = new AppleMessage();

export default appleMessage;