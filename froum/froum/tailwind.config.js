/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  darkMode: ['selector', 'html[data-mode="dark"]'],
  theme: {
    extend: {
      colors: {
        kumo: {
          base: 'var(--kumo-bg-base)',
          subtle: 'var(--kumo-bg-subtle)',
          elevated: 'var(--kumo-bg-elevated)',
          recessed: 'var(--kumo-bg-recessed)',
          overlay: 'var(--kumo-bg-overlay)',
          brand: 'var(--kumo-bg-brand)',
          'brand-strong': 'var(--kumo-bg-brand-strong)',
          'brand-soft': 'var(--kumo-bg-brand-soft)',
          accent: 'var(--kumo-bg-accent)',
          'accent-soft': 'var(--kumo-bg-accent-soft)',
          warm: 'var(--kumo-bg-warm)'
        },
        text: {
          kumo: {
            default: 'var(--kumo-text-default)',
            muted: 'var(--kumo-text-muted)',
            subtle: 'var(--kumo-text-subtle)',
            inverse: 'var(--kumo-text-inverse)'
          }
        },
        status: {
          success: 'var(--kumo-status-success)',
          'success-tint': 'var(--kumo-status-success-tint)',
          warning: 'var(--kumo-status-warning)',
          'warning-tint': 'var(--kumo-status-warning-tint)',
          danger: 'var(--kumo-status-danger)',
          'danger-tint': 'var(--kumo-status-danger-tint)',
          info: 'var(--kumo-status-info)',
          'info-tint': 'var(--kumo-status-info-tint)'
        },
        hairline: 'var(--kumo-hairline)'
      },
      borderColor: {
        'kumo-hairline': 'var(--kumo-hairline)',
        'kumo-hairline-strong': 'var(--kumo-hairline-strong)'
      },
      boxShadow: {
        'kumo-sm': 'var(--kumo-shadow-sm)',
        kumo: 'var(--kumo-shadow-md)',
        'kumo-lg': 'var(--kumo-shadow-lg)'
      },
      borderRadius: {
        kumo: 'var(--kumo-radius-md)',
        'kumo-lg': 'var(--kumo-radius-lg)',
        'kumo-xl': 'var(--kumo-radius-xl)'
      }
    }
  },
  plugins: []
}
