/* SDU Academic Advisor Tailwind design tokens and runtime theme manifest (T-08) */
(function () {
    'use strict';

    const advisorTheme = {
        theme: {
            extend: {
                colors: {
                    sdu: {
                        bg: '#f0f4f8',
                        primary: '#4c6ef5',
                        primaryDark: '#3b5bdb',
                        surface: '#ffffff',
                        slate: '#1a202c',
                        muted: '#718096'
                    }
                },
                fontFamily: {
                    sans: ['Segoe UI', 'system-ui', '-apple-system', 'sans-serif']
                },
                borderRadius: {
                    card: '0.75rem'
                }
            }
        }
    };

    window.advisorTailwindTheme = advisorTheme;
    if (typeof window.tailwind === 'object' && window.tailwind !== null) {
        window.tailwind.config = advisorTheme;
    }
})();
