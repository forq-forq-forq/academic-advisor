function toggleFaqItem(btn) {
    const targetSelector = btn.getAttribute('data-bs-target');
    const collapseEl = targetSelector ? document.querySelector(targetSelector) : btn.closest('.accordion-item')?.querySelector('.accordion-collapse');
    if (!collapseEl) return;
    const isOpen = collapseEl.classList.contains('show');
    collapseEl.classList.toggle('show', !isOpen);
    btn.classList.toggle('collapsed', isOpen);
}

function askAiFromFaq(btn) {
    const item = btn.closest('.faq-item');
    const question = item ? item.querySelector('.accordion-button')?.textContent.trim() : '';
    const promptText = question
        ? ('Regarding the FAQ "' + question + '": how does this apply to my current academic standing and transcript?')
        : 'Can you explain how SDU academic regulations apply to my current standing?';
    if (typeof window.openCopilotWithPrompt === 'function') {
        window.openCopilotWithPrompt(promptText, false);
    }
}

// US-35 Real-time search script
function filterFAQ() {
    const query = document.getElementById('faqSearchInput').value.toLowerCase();
    const faqItems = document.querySelectorAll('.faq-item');

    faqItems.forEach(item => {
        const text = item.innerText.toLowerCase();
        if (text.includes(query)) {
            item.style.display = '';
        } else {
            item.style.display = 'none';
        }
    });
}
