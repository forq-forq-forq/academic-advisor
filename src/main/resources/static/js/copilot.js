(function () {
    const rootEl        = document.getElementById('ai-copilot-root');
    const studentKey    = (rootEl && rootEl.getAttribute('data-student-id')) || 'guest';
    const storageKey    = 'sdu_advisor_copilot_history_' + studentKey;
    const openStateKey  = 'sdu_advisor_copilot_open_' + studentKey;

    const fabBtn        = document.getElementById('copilot-fab');
    const drawer        = document.getElementById('copilot-drawer');
    const closeBtn      = document.getElementById('copilot-close-btn');
    const clearBtn      = document.getElementById('copilot-clear-btn');
    const chatWindow    = document.getElementById('chat-window');
    const input         = document.getElementById('message-input');
    const sendBtn       = document.getElementById('send-btn');
    const validationMsg = document.getElementById('validation-msg');
    const welcomeHint   = document.getElementById('welcome-hint');

    function setDrawerOpen(isOpen) {
        drawer.classList.toggle('open', isOpen);
        fabBtn.setAttribute('aria-expanded', String(isOpen));
        try {
            sessionStorage.setItem(openStateKey, isOpen ? '1' : '0');
        } catch (e) {}
        if (isOpen) {
            chatWindow.scrollTop = chatWindow.scrollHeight;
            input.focus();
        }
    }

    fabBtn.addEventListener('click', () => {
        setDrawerOpen(!drawer.classList.contains('open'));
    });

    closeBtn.addEventListener('click', () => setDrawerOpen(false));

    clearBtn.addEventListener('click', () => {
        try {
            sessionStorage.removeItem(storageKey);
        } catch (e) {}
        chatWindow.innerHTML = '';
        if (welcomeHint) {
            chatWindow.appendChild(welcomeHint);
        }
    });

    function loadSavedMessages() {
        try {
            const raw = sessionStorage.getItem(storageKey);
            if (!raw) return;
            const items = JSON.parse(raw);
            if (Array.isArray(items) && items.length > 0) {
                if (welcomeHint && welcomeHint.parentNode) {
                    welcomeHint.remove();
                }
                items.forEach(item => {
                    appendBubbleDOM(item.role, item.text, item.recommendations);
                });
            }
        } catch (e) {}
    }

    function saveMessageToHistory(role, text, recommendations) {
        try {
            const raw = sessionStorage.getItem(storageKey);
            const items = raw ? JSON.parse(raw) : [];
            items.push({ role, text, recommendations: recommendations || [] });
            sessionStorage.setItem(storageKey, JSON.stringify(items.slice(-30)));
        } catch (e) {}
    }

    // Restore drawer state & history across page navigations
    loadSavedMessages();
    try {
        if (sessionStorage.getItem(openStateKey) === '1') {
            setDrawerOpen(true);
        }
    } catch (e) {}

    input.addEventListener('input', () => {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 90) + 'px';
        if (input.value.trim()) {
            input.classList.remove('invalid');
            validationMsg.textContent = '';
        }
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    sendBtn.addEventListener('click', sendMessage);

    document.querySelectorAll('.quick-prompt').forEach(button => {
        button.addEventListener('click', () => {
            setDrawerOpen(true);
            input.value = button.textContent.trim();
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.focus();
        });
    });

    window.openCopilotWithPrompt = function (promptText, autoSend) {
        setDrawerOpen(true);
        input.value = promptText || '';
        input.dispatchEvent(new Event('input', { bubbles: true }));
        if (autoSend && input.value.trim()) {
            sendMessage();
        } else {
            input.focus();
        }
    };

    function sendMessage() {
        const text = input.value.trim();

        if (!text) {
            input.classList.add('invalid');
            validationMsg.textContent = '⚠ Please enter a message before sending.';
            input.focus();
            return;
        }

        input.classList.remove('invalid');
        validationMsg.textContent = '';

        if (welcomeHint && welcomeHint.parentNode) {
            welcomeHint.remove();
        }

        appendBubble('user', text);
        input.value = '';
        input.style.height = 'auto';
        sendBtn.disabled = true;

        const typingRow = appendTypingIndicator();

        const formData = new FormData();
        formData.append('message', text);
        formData.append('page', window.location.pathname);

        fetch('/dashboard/chat', { method: 'POST', headers: window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {}, body: formData })
            .then(res => res.json())
            .then(data => {
                typingRow.remove();
                if (data.reply) {
                    appendBubble('ai', data.reply, data.recommendations);
                } else {
                    appendBubble('ai', '⚠ ' + (data.error || 'Unexpected error. Please try again.'));
                }
            })
            .catch(() => {
                typingRow.remove();
                appendBubble('ai', '⚠ Could not reach the advisor. Check your connection and try again.');
            })
            .finally(() => {
                sendBtn.disabled = false;
                input.focus();
            });
    }

    function appendBubble(role, text, recommendations) {
        saveMessageToHistory(role, text, recommendations);
        return appendBubbleDOM(role, text, recommendations);
    }

    function appendBubbleDOM(role, text, recommendations) {
        const row = document.createElement('div');
        row.className = 'msg-row ' + role;

        const av = document.createElement('div');
        av.className = 'avatar ' + (role === 'user' ? 'user-av' : 'ai-av');
        av.textContent = role === 'user' ? 'Me' : 'AI';

        const bubble = document.createElement('div');
        bubble.className = 'bubble';
        bubble.textContent = text;

        if (role === 'ai' && Array.isArray(recommendations) && recommendations.length > 0) {
            const recList = document.createElement('div');
            recList.className = 'copilot-rec-list';
            recommendations.forEach(rec => {
                const card = document.createElement('div');
                card.className = 'copilot-rec-card';

                const header = document.createElement('div');
                header.className = 'copilot-rec-header';

                const title = document.createElement('span');
                title.className = 'copilot-rec-title';
                title.textContent = rec.code + ' — ' + rec.name;

                const meta = document.createElement('span');
                meta.className = 'copilot-rec-meta';
                meta.textContent = rec.credits + ' ECTS · ' + (rec.courseType || 'COURSE');

                header.appendChild(title);
                header.appendChild(meta);

                const reason = document.createElement('div');
                reason.className = 'copilot-rec-reason';
                reason.textContent = '💡 Why this course: ' + (rec.reason || 'Recommended for your degree plan.');

                const addBtn = document.createElement('button');
                addBtn.type = 'button';
                addBtn.className = 'copilot-rec-add-btn';
                if (rec.inCart) {
                    addBtn.disabled = true;
                    addBtn.textContent = '✓ In Cart';
                } else {
                    addBtn.textContent = '+ Add to Cart';
                    addBtn.addEventListener('click', () => addRecommendedCourseToCart(rec.courseId, addBtn));
                }

                card.appendChild(header);
                card.appendChild(reason);
                card.appendChild(addBtn);
                recList.appendChild(card);
            });
            bubble.appendChild(recList);
        }

        if (role === 'user') {
            row.appendChild(bubble);
            row.appendChild(av);
        } else {
            row.appendChild(av);
            row.appendChild(bubble);
        }

        chatWindow.appendChild(row);
        chatWindow.scrollTop = chatWindow.scrollHeight;
        return row;
    }

    function addRecommendedCourseToCart(courseId, btnEl) {
        if (typeof window.addCourse === 'function') {
            window.addCourse(String(courseId));
            btnEl.disabled = true;
            btnEl.textContent = '✓ Added';
            return;
        }
        btnEl.disabled = true;
        btnEl.textContent = 'Adding...';
        fetch('/planner/cart/add?courseId=' + encodeURIComponent(courseId), { method: 'POST', headers: window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {} })
            .then(res => res.json().then(body => ({ ok: res.ok, body })))
            .then(({ ok, body }) => {
                if (ok) {
                    btnEl.textContent = '✓ Added to Cart';
                } else {
                    btnEl.disabled = false;
                    btnEl.textContent = '+ Add to Cart';
                    alert(body.error || 'Could not add course to cart.');
                }
            })
            .catch(() => {
                btnEl.disabled = false;
                btnEl.textContent = '+ Add to Cart';
            });
    }

    function appendTypingIndicator() {
        const row = document.createElement('div');
        row.className = 'msg-row ai';

        const av = document.createElement('div');
        av.className = 'avatar ai-av';
        av.textContent = 'AI';

        const typing = document.createElement('div');
        typing.className = 'typing-bubble';
        typing.innerHTML = '<span></span><span></span><span></span>';

        row.appendChild(av);
        row.appendChild(typing);
        chatWindow.appendChild(row);
        chatWindow.scrollTop = chatWindow.scrollHeight;
        return row;
    }
})();
