const plannerStateEl = document.getElementById('planner-state');
let maxCreditsLimit = plannerStateEl && plannerStateEl.dataset.maxCredits ? Number(plannerStateEl.dataset.maxCredits) : 40;
let currentFormattedCodes = plannerStateEl ? (plannerStateEl.dataset.formattedCodes || '') : '';
const studentCompletedCodes = new Set(
    plannerStateEl
        ? Array.from(plannerStateEl.querySelectorAll('.js-completed-code')).map(el => el.dataset.code).filter(Boolean)
        : []
);

function renderCart(cart) {
    maxCreditsLimit = cart.maxCredits;

    // 1. Update running counter (AC 2)
    document.getElementById('current-credits').textContent = cart.totalCredits;
    document.getElementById('max-credits').textContent = cart.maxCredits;

    // 2. Update workload badge & bar (AC 3)
    const badge = document.getElementById('workload-badge');
    badge.textContent = cart.workloadState;
    badge.className = 'workload-badge state-' + cart.workloadState.toLowerCase();

    const bar = document.getElementById('progress-bar');
    const pct = cart.maxCredits > 0 ? Math.min(100, (cart.totalCredits * 100) / cart.maxCredits) : 0;
    bar.style.width = pct + '%';
    bar.className = 'progress-bar bar-' + cart.workloadColor;

    // 3. Update copy codes & enroll button (US-13 + Enrollment)
    currentFormattedCodes = cart.formattedCodes !== undefined && cart.formattedCodes !== null
        ? cart.formattedCodes
        : (cart.items ? cart.items.map(i => i.code).join(', ') : '');
    const copyBtn = document.getElementById('btn-copy-codes');
    if (copyBtn) {
        copyBtn.disabled = cart.empty || !currentFormattedCodes;
        copyBtn.setAttribute('data-codes', currentFormattedCodes);
    }
    const enrollBtn = document.getElementById('btn-enroll-cart');
    if (enrollBtn) {
        enrollBtn.disabled = cart.empty || !cart.items || cart.items.length === 0;
    }

    // 4. Update credit limit warning (US-14)
    const limitWarningBox = document.getElementById('credit-limit-warning');
    const limitWarningMsg = document.getElementById('credit-limit-warning-msg');
    const creditCounter = document.getElementById('credit-counter');

    const isExceeded = cart.exceedsLimit || (cart.totalCredits > cart.maxCredits);
    if (isExceeded) {
        const excess = cart.excessCredits || (cart.totalCredits - cart.maxCredits);
        const msg = cart.limitWarning ||
            `Credit limit exceeded: You have selected ${cart.totalCredits} ECTS, which exceeds the maximum limit of ${cart.maxCredits} ECTS by ${excess} ECTS.`;
        if (limitWarningMsg) limitWarningMsg.textContent = msg;
        if (limitWarningBox) limitWarningBox.style.display = 'flex';
        if (creditCounter) creditCounter.classList.add('limit-exceeded');
    } else {
        if (limitWarningBox) limitWarningBox.style.display = 'none';
        if (creditCounter) creditCounter.classList.remove('limit-exceeded');
    }

    // 5. Update catalog prerequisite badges and In Cart state dynamically (US-12 fix)
    const inCartCodes = new Set(cart.items ? cart.items.map(i => i.code) : []);
    const inCartIds = new Set(cart.items ? cart.items.map(i => String(i.courseId)) : []);
    document.querySelectorAll('.catalog-item').forEach(item => {
        const itemCourseId = item.getAttribute('data-course-id');
        const isEnrolled = item.getAttribute('data-enrolled') === 'true';
        const existingBtn = item.querySelector('.btn-add');
        const existingCartBadge = item.querySelector('.badge-in-cart');
        if (!isEnrolled) {
            if (inCartIds.has(String(itemCourseId))) {
                if (existingBtn) {
                    existingBtn.outerHTML = '<span class="badge-in-cart">In Cart</span>';
                }
            } else {
                if (existingCartBadge) {
                    existingCartBadge.outerHTML = `<button type="button" class="btn-add" data-course-id="${itemCourseId}" onclick="addCourse(${itemCourseId})">+ Add</button>`;
                }
            }
        }

        const hasPrereqs = item.getAttribute('data-has-prereqs') === 'true';
        if (!hasPrereqs) return;

        const prereqCodesStr = item.getAttribute('data-prereqs') || '';
        const prereqCodes = prereqCodesStr ? prereqCodesStr.split(',').map(s => s.trim()).filter(Boolean) : [];
        if (prereqCodes.length === 0) return;

        const missing = prereqCodes.filter(c => !studentCompletedCodes.has(c) && !inCartCodes.has(c));
        const badgeMet = item.querySelector('.badge-prereq-met');
        const badgeMissing = item.querySelector('.badge-prereq-missing');
        const missingDetail = item.querySelector('.missing-detail');

        if (missing.length === 0) {
            item.setAttribute('data-prereqs-met', 'true');
            if (badgeMissing) {
                badgeMissing.className = 'badge-prereq-met';
                badgeMissing.textContent = 'Prereqs Met ✓';
                badgeMissing.title = 'Prerequisites satisfied: ' + prereqCodes.join(', ');
            }
            if (missingDetail) {
                missingDetail.style.display = 'none';
            }
        } else {
            item.setAttribute('data-prereqs-met', 'false');
            if (badgeMet) {
                badgeMet.className = 'badge-prereq-missing';
                badgeMet.textContent = 'Missing Prereq ⚠️';
                badgeMet.title = 'Missing prerequisite: ' + missing.join(', ');
            }
            if (missingDetail) {
                missingDetail.style.display = 'inline';
                missingDetail.textContent = '(Missing: ' + missing.join(', ') + ')';
            }
        }
    });
    filterCatalog();

    // 6. Update cart items list / placeholder (AC 1 & AC 5)
    const placeholder = document.getElementById('cart-placeholder');
    const list = document.getElementById('cart-items-list');

    if (cart.empty || !cart.items || cart.items.length === 0) {
        placeholder.style.display = 'block';
        list.style.display = 'none';
        list.innerHTML = '';
    } else {
        placeholder.style.display = 'none';
        list.style.display = 'flex';
        list.innerHTML = '';

        cart.items.forEach(item => {
            const li = document.createElement('li');
            li.className = 'cart-item';
            li.id = 'cart-item-' + item.courseId;

            const creditsText = (item.credits !== null && item.credits !== undefined ? item.credits : 0) + ' ECTS';

            li.innerHTML = `
                <div class="item-info">
                    <div class="item-code-credits">
                        <span class="course-code">${escapeHtml(item.code)}</span>
                        <span class="credit-tag">${creditsText}</span>
                    </div>
                    <span class="course-name">${escapeHtml(item.name)}</span>
                </div>
                <button type="button"
                        class="btn-remove"
                        data-course-id="${item.courseId}"
                        onclick="removeCourse(${item.courseId})"
                        aria-label="Remove course">×</button>
            `;
            list.appendChild(li);
        });
    }
}

function copyCartCodes() {
    const btn = document.getElementById('btn-copy-codes');
    const textToCopy = currentFormattedCodes || (btn ? btn.getAttribute('data-codes') : '');

    if (!textToCopy || textToCopy.trim() === '') {
        showAlert('Your cart is empty. There are no course codes to copy.');
        return;
    }

    const onSuccess = () => {
        if (btn) {
            const originalHtml = btn.innerHTML;
            btn.innerHTML = '✓ Copied!';
            btn.classList.add('copied');
            setTimeout(() => {
                btn.innerHTML = originalHtml;
                btn.classList.remove('copied');
            }, 2000);
        }
    };

    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(textToCopy)
            .then(onSuccess)
            .catch(() => fallbackCopy(textToCopy, onSuccess));
    } else {
        fallbackCopy(textToCopy, onSuccess);
    }
}

function fallbackCopy(text, callback) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.left = '-999999px';
    textarea.style.top = '-999999px';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    try {
        document.execCommand('copy');
        if (callback) callback();
    } catch (err) {
        console.error('Fallback copy failed', err);
        showAlert('Could not copy automatically. Course codes: ' + text);
    } finally {
        document.body.removeChild(textarea);
    }
}

function showAlert(msg) {
    const alertBox = document.getElementById('planner-alert');
    const msgSpan = document.getElementById('planner-alert-msg');
    if (alertBox && msgSpan) {
        msgSpan.textContent = msg;
        alertBox.style.display = 'flex';
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function hideAlert() {
    const alertBox = document.getElementById('planner-alert');
    if (alertBox) {
        alertBox.style.display = 'none';
    }
}

function addCourse(courseId) {
    hideAlert();
    fetch('/planner/cart/add?courseId=' + encodeURIComponent(courseId), {
        method: 'POST',
        headers: window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {}
    })
    .then(async res => {
        if (res.status === 401) {
            window.location.href = '/login';
            return null;
        }
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
            const errorMsg = data.error || 'Failed to add course';
            showAlert(errorMsg);
            return null;
        }
        return data;
    })
    .then(cart => {
        if (cart) {
            renderCart(cart);
            // Update catalog item UI
            const catalogItem = document.getElementById('catalog-item-' + courseId);
            if (catalogItem) {
                const btn = catalogItem.querySelector('.btn-add');
                if (btn) {
                    btn.outerHTML = '<span class="badge-in-cart">In Cart</span>';
                }
            }
        }
    })
    .catch(err => {
        console.error(err);
        showAlert('An unexpected error occurred while adding the course.');
    });
}

function removeCourse(courseId) {
    hideAlert();
    fetch('/planner/cart/remove?courseId=' + encodeURIComponent(courseId), {
        method: 'POST',
        headers: window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {}
    })
    .then(async res => {
        if (res.status === 401) {
            window.location.href = '/login';
            return null;
        }
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
            const errorMsg = data.error || 'Failed to remove course';
            showAlert(errorMsg);
            return null;
        }
        return data;
    })
    .then(cart => {
        if (cart) {
            renderCart(cart);
            // If catalog item had "In Cart", restore the button
            const catalogItem = document.getElementById('catalog-item-' + courseId);
            if (catalogItem) {
                const badge = catalogItem.querySelector('.badge-in-cart');
                if (badge) {
                    badge.outerHTML = `<button type="button" class="btn-add" data-course-id="${courseId}" onclick="addCourse(${courseId})">+ Add</button>`;
                }
            }
        }
    })
    .catch(err => {
        console.error(err);
        showAlert('An unexpected error occurred while removing the course.');
    });
}

function renderEnrolledCourses(enrolledCourses, enrolledCredits) {
    const listEl = document.getElementById('planner-enrolled-list');
    const emptyEl = document.getElementById('planner-enrolled-empty');
    const creditsPill = document.getElementById('planner-enrolled-credits');
    const returnBtn = document.getElementById('btn-return-to-cart');

    const courses = Array.isArray(enrolledCourses) ? enrolledCourses : [];
    const totalCr = enrolledCredits !== undefined
        ? enrolledCredits
        : courses.reduce((sum, c) => sum + (c.credits || 0), 0);

    if (creditsPill) {
        creditsPill.textContent = totalCr + ' ECTS';
    }
    if (returnBtn) {
        returnBtn.style.display = courses.length > 0 ? 'inline-block' : 'none';
    }

    if (courses.length === 0) {
        if (emptyEl) emptyEl.style.display = 'block';
        if (listEl) {
            listEl.style.display = 'none';
            listEl.innerHTML = '';
        }
    } else {
        if (emptyEl) emptyEl.style.display = 'none';
        if (listEl) {
            listEl.style.display = 'flex';
            listEl.innerHTML = '';
            courses.forEach(c => {
                const li = document.createElement('li');
                li.className = 'cart-item';
                li.id = 'enrolled-item-' + c.id;
                const crText = c.formattedCredits || ((c.credits || 0) + ' ECTS');
                li.innerHTML = `
                    <div class="item-info">
                        <div class="item-code-credits">
                            <span class="course-code">${escapeHtml(c.code)}</span>
                            <span class="credit-tag">${escapeHtml(crText)}</span>
                            <span class="badge-enrolled">Принят ✓</span>
                        </div>
                        <span class="course-name">${escapeHtml(c.name)}</span>
                    </div>
                    <button type="button"
                            class="btn-cancel-enrolled"
                            data-course-id="${c.id}"
                            onclick="cancelEnrolledCourse(${c.id})"
                            title="Отменить принятый курс">↩️ Отменить</button>
                `;
                listEl.appendChild(li);
            });
        }
    }

    const enrolledIdSet = new Set(courses.map(c => String(c.id)));
    document.querySelectorAll('.catalog-item').forEach(item => {
        const itemCourseId = String(item.getAttribute('data-course-id'));
        const isEnrolled = enrolledIdSet.has(itemCourseId);
        item.setAttribute('data-enrolled', isEnrolled ? 'true' : 'false');

        const headerRow = item.querySelector('.item-code-credits');
        let enrolledBadge = headerRow ? headerRow.querySelector('.badge-enrolled') : null;
        if (isEnrolled && !enrolledBadge && headerRow) {
            const span = document.createElement('span');
            span.className = 'badge-enrolled';
            span.textContent = 'Enrolled 📚';
            headerRow.appendChild(span);
        } else if (!isEnrolled && enrolledBadge) {
            enrolledBadge.remove();
        }

        const actionsBox = item.querySelector('.catalog-item-actions');
        if (!actionsBox) return;
        const cancelBtn = actionsBox.querySelector('.btn-cancel-enrolled');
        const addBtn = actionsBox.querySelector('.btn-add');
        const cartBadge = actionsBox.querySelector('.badge-in-cart');

        if (isEnrolled) {
            if (addBtn) addBtn.remove();
            if (cartBadge) cartBadge.remove();
            if (!cancelBtn) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn-cancel-enrolled';
                btn.setAttribute('data-course-id', itemCourseId);
                btn.setAttribute('onclick', `cancelEnrolledCourse(${itemCourseId})`);
                btn.title = 'Отменить принятый курс';
                btn.textContent = '↩️ Отменить';
                actionsBox.appendChild(btn);
            }
        } else if (cancelBtn) {
            cancelBtn.outerHTML = `<button type="button" class="btn-add" data-course-id="${itemCourseId}" onclick="addCourse(${itemCourseId})">+ Add</button>`;
        }
    });
}

function enrollCart() {
    hideAlert();
    fetch('/planner/cart/enroll', { method: 'POST', headers: window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {} })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to confirm course enrollment.');
                return null;
            }
            return data;
        })
        .then(data => {
            if (data) {
                renderEnrolledCourses(data.enrolledCourses, data.enrolledCredits);
                if (data.cart) {
                    renderCart(data.cart);
                }
            }
        })
        .catch(err => {
            console.error(err);
            showAlert('An unexpected error occurred while confirming courses.');
        });
}

function cancelEnrolledCourse(courseId) {
    hideAlert();
    fetch('/planner/enrolled/cancel?courseId=' + encodeURIComponent(courseId), { method: 'POST', headers: window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {} })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to cancel enrolled course.');
                return null;
            }
            return data;
        })
        .then(data => {
            if (data) {
                renderEnrolledCourses(data.enrolledCourses, data.enrolledCredits);
                if (data.cart) {
                    renderCart(data.cart);
                }
            }
        })
        .catch(err => {
            console.error(err);
            showAlert('An unexpected error occurred while cancelling the course.');
        });
}

function returnEnrolledToCart() {
    hideAlert();
    fetch('/planner/enrolled/return-to-cart', { method: 'POST', headers: window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {} })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to return enrolled courses to cart.');
                return null;
            }
            return data;
        })
        .then(data => {
            if (data) {
                renderEnrolledCourses(data.enrolledCourses, data.enrolledCredits);
                if (data.cart) {
                    renderCart(data.cart);
                }
            }
        })
        .catch(err => {
            console.error(err);
            showAlert('An unexpected error occurred while returning courses to cart.');
        });
}

let activeCatalogFilter = 'ALL';

function toggleCatalogDrawer(forceOpen) {
    const drawer = document.getElementById('catalog-drawer');
    const main = document.getElementById('planner-main');
    const toggleBtn = document.getElementById('btn-toggle-catalog-drawer');
    if (!drawer) return;

    const currentlyOpen = drawer.classList.contains('open') && !drawer.classList.contains('collapsed');
    const shouldOpen = typeof forceOpen === 'boolean' ? forceOpen : !currentlyOpen;

    drawer.classList.toggle('open', shouldOpen);
    drawer.classList.toggle('collapsed', !shouldOpen);
    if (main) {
        main.classList.toggle('drawer-collapsed', !shouldOpen);
    }
    if (toggleBtn) {
        toggleBtn.setAttribute('aria-expanded', String(shouldOpen));
        toggleBtn.classList.toggle('active', shouldOpen);
        toggleBtn.textContent = shouldOpen ? '≡ Hide Catalog' : '≡ Open Catalog';
    }
    if (shouldOpen) {
        const searchInput = document.getElementById('catalog-search');
        if (searchInput) searchInput.focus();
    }
}

function clearCatalogSearch() {
    const searchInput = document.getElementById('catalog-search');
    if (searchInput) {
        searchInput.value = '';
        searchInput.focus();
    }
    const seasonSelect = document.getElementById('catalog-season-filter');
    if (seasonSelect) seasonSelect.value = 'ALL';
    const difficultySelect = document.getElementById('catalog-difficulty-filter');
    if (difficultySelect) difficultySelect.value = 'ALL';
    filterCatalog();
}

function setCatalogFilter(filter, btn) {
    activeCatalogFilter = filter;
    document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
    if (btn) btn.classList.add('active');
    filterCatalog();
}

function filterCatalog() {
    const searchInput = document.getElementById('catalog-search');
    const clearBtn = document.getElementById('btn-clear-catalog-search');
    const countEl = document.getElementById('catalog-match-count');
    const emptyState = document.getElementById('catalog-empty-state');
    const seasonSelect = document.getElementById('catalog-season-filter');
    const difficultySelect = document.getElementById('catalog-difficulty-filter');
    const query = searchInput ? searchInput.value.trim().toLowerCase() : '';
    const selectedSeason = seasonSelect ? seasonSelect.value.toUpperCase() : 'ALL';
    const selectedDifficulty = difficultySelect ? difficultySelect.value : 'ALL';

    if (clearBtn) {
        clearBtn.style.display = query ? 'inline-flex' : 'none';
    }

    let visibleCount = 0;
    document.querySelectorAll('.catalog-item').forEach(item => {
        const code = (item.getAttribute('data-course-code') || '').toLowerCase();
        const name = (item.getAttribute('data-course-name') || '').toLowerCase();
        const type = (item.getAttribute('data-course-type') || '').toUpperCase();
        const category = (item.getAttribute('data-course-category') || type).toUpperCase();
        const season = (item.getAttribute('data-course-season') || 'BOTH').toUpperCase();
        const difficulty = String(item.getAttribute('data-course-difficulty') || '');
        const completed = item.getAttribute('data-completed') === 'true';
        const prereqsMet = item.getAttribute('data-prereqs-met') === 'true';

        const matchesQuery = !query || code.includes(query) || name.includes(query);
        let matchesFilter = true;
        if (activeCatalogFilter === 'READY') {
            matchesFilter = prereqsMet && !completed;
        } else if (activeCatalogFilter === 'CORE') {
            matchesFilter = category === 'CORE';
        } else if (activeCatalogFilter === 'ELECTIVE') {
            matchesFilter = category === 'ELECTIVE';
        } else if (activeCatalogFilter === 'GENERAL') {
            matchesFilter = category === 'GENERAL';
        } else if (activeCatalogFilter === 'UNCOMPLETED') {
            matchesFilter = !completed;
        }
        const matchesSeason = selectedSeason === 'ALL' || season === 'BOTH' || season === selectedSeason;
        const matchesDifficulty = selectedDifficulty === 'ALL' || difficulty === selectedDifficulty;

        const isVisible = matchesQuery && matchesFilter && matchesSeason && matchesDifficulty;
        item.style.display = isVisible ? 'flex' : 'none';
        if (isVisible) visibleCount++;
    });

    if (countEl) {
        countEl.textContent = visibleCount + (visibleCount === 1 ? ' course' : ' courses');
    }
    if (emptyState) {
        emptyState.style.display = visibleCount === 0 ? 'block' : 'none';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
