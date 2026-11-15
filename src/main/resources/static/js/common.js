/* Shared frontend helpers for SDU Academic Advisor (T-07) */
(function () {
    'use strict';

    window.advisorCsrfHeaders = function advisorCsrfHeaders(extraHeaders) {
        const headers = Object.assign({}, extraHeaders || {});
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        if (tokenMeta && headerMeta) {
            const token = tokenMeta.getAttribute('content');
            const headerName = headerMeta.getAttribute('content');
            if (token && headerName) {
                headers[headerName] = token;
            }
        }
        return headers;
    };
})();
