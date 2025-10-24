/**
 * Apple-style confirm dialog to replace ElMessageBox
 * Provides native-looking confirmation dialogs
 */

class AppleConfirm {
  constructor() {
    this.container = null;
    this.init();
  }

  init() {
    if (typeof document !== 'undefined') {
      this.container = document.createElement('div');
      this.container.className = 'apple-confirm-overlay';
      this.container.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.4);
        backdrop-filter: blur(20px);
        -webkit-backdrop-filter: blur(20px);
        z-index: 10000;
        display: none;
        align-items: center;
        justify-content: center;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
      `;
      document.body.appendChild(this.container);
    }
  }

  confirm(title, message, options = {}) {
    return new Promise((resolve) => {
      if (!this.container) {
        resolve(false);
        return;
      }

      const {
        confirmText = '确认',
        cancelText = '取消',
        type = 'warning' // success, warning, error, info
      } = options;

      this.container.innerHTML = `
        <div class="apple-confirm-dialog" style="
          background: rgba(255, 255, 255, 0.95);
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          border: 1px solid rgba(0, 0, 0, 0.1);
          border-radius: 16px;
          padding: 24px;
          max-width: 320px;
          width: 90%;
          box-shadow: 0 12px 28px rgba(0, 0, 0, 0.24);
          transform: scale(0.9);
          opacity: 0;
          transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
        ">
          <div class="apple-confirm-header" style="
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;
          ">
            <div class="apple-confirm-icon" style="
              width: 32px;
              height: 32px;
              border-radius: 50%;
              display: flex;
              align-items: center;
              justify-content: center;
              font-size: 18px;
              font-weight: 600;
              ${this.getIconStyle(type)}
            ">
              ${this.getIcon(type)}
            </div>
            <h3 class="apple-confirm-title" style="
              margin: 0;
              font-size: 17px;
              font-weight: 600;
              color: #1d1d1f;
            ">
              ${title}
            </h3>
          </div>

          <div class="apple-confirm-message" style="
            margin-bottom: 24px;
            font-size: 15px;
            line-height: 1.4;
            color: #515154;
          ">
            ${message}
          </div>

          <div class="apple-confirm-actions" style="
            display: flex;
            gap: 12px;
            justify-content: flex-end;
          ">
            <button class="apple-confirm-btn apple-confirm-cancel" style="
              background: transparent;
              border: none;
              padding: 8px 16px;
              border-radius: 8px;
              font-size: 15px;
              font-weight: 500;
              color: #007AFF;
              cursor: pointer;
              transition: background-color 0.2s;
            ">
              ${cancelText}
            </button>
            <button class="apple-confirm-btn apple-confirm-ok" style="
              background: #007AFF;
              border: none;
              padding: 8px 16px;
              border-radius: 8px;
              font-size: 15px;
              font-weight: 500;
              color: white;
              cursor: pointer;
              transition: background-color 0.2s;
            ">
              ${confirmText}
            </button>
          </div>
        </div>
      `;

      this.container.style.display = 'flex';

      // Trigger animation
      requestAnimationFrame(() => {
        const dialog = this.container.querySelector('.apple-confirm-dialog');
        dialog.style.transform = 'scale(1)';
        dialog.style.opacity = '1';
      });

      // Button handlers
      const cancelBtn = this.container.querySelector('.apple-confirm-cancel');
      const okBtn = this.container.querySelector('.apple-confirm-ok');

      cancelBtn.addEventListener('click', () => {
        this.hide();
        resolve(false);
      });

      okBtn.addEventListener('click', () => {
        this.hide();
        resolve(true);
      });

      // Hover effects
      cancelBtn.addEventListener('mouseenter', () => {
        cancelBtn.style.backgroundColor = 'rgba(0, 122, 255, 0.1)';
      });
      cancelBtn.addEventListener('mouseleave', () => {
        cancelBtn.style.backgroundColor = 'transparent';
      });

      okBtn.addEventListener('mouseenter', () => {
        okBtn.style.backgroundColor = '#0056CC';
      });
      okBtn.addEventListener('mouseleave', () => {
        okBtn.style.backgroundColor = '#007AFF';
      });

      // Close on overlay click
      this.container.addEventListener('click', (e) => {
        if (e.target === this.container) {
          this.hide();
          resolve(false);
        }
      });
    });
  }

  getIcon(type) {
    const icons = {
      success: '✓',
      error: '✕',
      warning: '⚠',
      info: 'ℹ'
    };
    return icons[type] || icons.warning;
  }

  getIconStyle(type) {
    const styles = {
      success: 'background: #30D158; color: white;',
      error: 'background: #FF3B30; color: white;',
      warning: 'background: #FF9500; color: white;',
      info: 'background: #007AFF; color: white;'
    };
    return styles[type] || styles.warning;
  }

  hide() {
    const dialog = this.container.querySelector('.apple-confirm-dialog');
    if (dialog) {
      dialog.style.transform = 'scale(0.9)';
      dialog.style.opacity = '0';
      setTimeout(() => {
        this.container.style.display = 'none';
        this.container.innerHTML = '';
      }, 300);
    }
  }
}

// Create singleton instance
const appleConfirm = new AppleConfirm();

export default appleConfirm;