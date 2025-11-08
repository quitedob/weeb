/**
 * Bug Reporter Utility
 * Centralized bug reporting and logging system for debugging chat issues
 */

class BugReporter {
  constructor() {
    this.reports = [];
    this.maxReports = 100; // Keep last 100 reports
    this.enabled = import.meta.env.DEV || import.meta.env.VITE_ENABLE_BUG_REPORTER === 'true';
  }

  /**
   * Report a bug with context
   * @param {string} category - Bug category (e.g., 'CHAT_ID', 'WEBSOCKET', 'MESSAGE')
   * @param {string} message - Bug description
   * @param {object} context - Additional context data
   * @param {string} severity - 'ERROR', 'WARNING', 'INFO'
   */
  report(category, message, context = {}, severity = 'ERROR') {
    if (!this.enabled) return;

    const report = {
      id: `bug_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      category,
      message,
      context,
      severity,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      stackTrace: new Error().stack
    };

    this.reports.push(report);

    // Keep only last maxReports
    if (this.reports.length > this.maxReports) {
      this.reports.shift();
    }

    // Log to console with appropriate styling
    const style = this.getConsoleStyle(severity);
    console.group(`%c🐛 BUG REPORT [${severity}] ${category}`, style);
    console.log('Message:', message);
    console.log('Context:', context);
    console.log('Timestamp:', report.timestamp);
    console.log('Stack Trace:', report.stackTrace);
    console.groupEnd();

    // Store in localStorage for persistence
    this.persistReports();

    return report;
  }

  /**
   * Report a chat ID mismatch issue
   */
  reportChatIdMismatch(expected, actual, context = {}) {
    return this.report(
      'CHAT_ID_MISMATCH',
      `Chat ID mismatch: expected ${expected}, got ${actual}`,
      {
        expected,
        actual,
        expectedType: typeof expected,
        actualType: typeof actual,
        ...context
      },
      'ERROR'
    );
  }

  /**
   * Report missing sharedChatId
   */
  reportMissingSharedChatId(object, context = {}) {
    return this.report(
      'MISSING_SHARED_CHAT_ID',
      'Object missing sharedChatId field',
      {
        object,
        availableFields: Object.keys(object || {}),
        ...context
      },
      'ERROR'
    );
  }

  /**
   * Report WebSocket connection issue
   */
  reportWebSocketIssue(issue, context = {}) {
    return this.report(
      'WEBSOCKET_ERROR',
      `WebSocket issue: ${issue}`,
      context,
      'ERROR'
    );
  }

  /**
   * Report message routing issue
   */
  reportMessageRoutingIssue(message, reason, context = {}) {
    return this.report(
      'MESSAGE_ROUTING',
      `Message routing failed: ${reason}`,
      {
        message,
        reason,
        ...context
      },
      'ERROR'
    );
  }

  /**
   * Report API response issue
   */
  reportApiIssue(endpoint, issue, response, context = {}) {
    return this.report(
      'API_ERROR',
      `API issue at ${endpoint}: ${issue}`,
      {
        endpoint,
        issue,
        response,
        ...context
      },
      'ERROR'
    );
  }

  /**
   * Get all reports
   */
  getAllReports() {
    return this.reports;
  }

  /**
   * Get reports by category
   */
  getReportsByCategory(category) {
    return this.reports.filter(r => r.category === category);
  }

  /**
   * Get reports by severity
   */
  getReportsBySeverity(severity) {
    return this.reports.filter(r => r.severity === severity);
  }

  /**
   * Export reports as JSON
   */
  exportReports() {
    const data = {
      reports: this.reports,
      exportedAt: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `bug-reports-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Clear all reports
   */
  clearReports() {
    this.reports = [];
    localStorage.removeItem('bug_reports');
    console.log('🧹 All bug reports cleared');
  }

  /**
   * Get console style based on severity
   */
  getConsoleStyle(severity) {
    const styles = {
      ERROR: 'background: #ff4444; color: white; padding: 2px 6px; border-radius: 3px; font-weight: bold;',
      WARNING: 'background: #ffaa00; color: white; padding: 2px 6px; border-radius: 3px; font-weight: bold;',
      INFO: 'background: #4444ff; color: white; padding: 2px 6px; border-radius: 3px; font-weight: bold;'
    };
    return styles[severity] || styles.INFO;
  }

  /**
   * Persist reports to localStorage
   */
  persistReports() {
    try {
      const recentReports = this.reports.slice(-50); // Keep last 50 in storage
      localStorage.setItem('bug_reports', JSON.stringify(recentReports));
    } catch (error) {
      console.warn('Failed to persist bug reports:', error);
    }
  }

  /**
   * Load reports from localStorage
   */
  loadReports() {
    try {
      const stored = localStorage.getItem('bug_reports');
      if (stored) {
        this.reports = JSON.parse(stored);
        console.log(`📋 Loaded ${this.reports.length} bug reports from storage`);
      }
    } catch (error) {
      console.warn('Failed to load bug reports:', error);
    }
  }

  /**
   * Print summary to console
   */
  printSummary() {
    console.group('🐛 Bug Report Summary');
    console.log('Total Reports:', this.reports.length);
    console.log('By Severity:', {
      ERROR: this.getReportsBySeverity('ERROR').length,
      WARNING: this.getReportsBySeverity('WARNING').length,
      INFO: this.getReportsBySeverity('INFO').length
    });
    console.log('By Category:', this.getCategorySummary());
    console.log('Recent Reports:', this.reports.slice(-5));
    console.groupEnd();
  }

  /**
   * Get category summary
   */
  getCategorySummary() {
    const summary = {};
    this.reports.forEach(report => {
      summary[report.category] = (summary[report.category] || 0) + 1;
    });
    return summary;
  }
}

// Create singleton instance
const bugReporter = new BugReporter();

// Load existing reports on initialization
bugReporter.loadReports();

// Expose to window for debugging
if (typeof window !== 'undefined') {
  window.bugReporter = bugReporter;
  console.log('🐛 Bug Reporter initialized. Use window.bugReporter to access reports.');
  console.log('Commands:');
  console.log('  - window.bugReporter.printSummary() - Print summary');
  console.log('  - window.bugReporter.exportReports() - Export to JSON');
  console.log('  - window.bugReporter.clearReports() - Clear all reports');
}

export default bugReporter;
