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

function openCourseDetailModal(courseId) {
    const modal = document.getElementById('course-detail-modal');
    if (!modal || !courseId) return;

    fetch('/planner/courses/' + encodeURIComponent(courseId), {
        headers: { 'Accept': 'application/json' }
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to load course details.');
                return null;
            }
            return data;
        })
        .then(detail => {
            if (!detail) return;
            renderCourseDetailModal(detail);
            modal.style.display = 'flex';
        })
        .catch(() => {
            showAlert('Unable to load course details right now.');
        });
}

function renderCourseDetailModal(detail) {
    document.getElementById('detail-course-code').textContent = detail.code || '';
    document.getElementById('detail-course-name').textContent = detail.name || '';
    document.getElementById('detail-course-credits').textContent = (detail.credits || 0) + ' ECTS';
    document.getElementById('detail-course-category').textContent = detail.category || detail.courseType || 'COURSE';
    document.getElementById('detail-course-season').textContent = detail.season || 'Fall & Spring';
    document.getElementById('detail-course-description').textContent = detail.description || 'No description available.';
    document.getElementById('detail-course-difficulty').textContent = detail.difficultyLabel || ((detail.difficulty || 3) + '/5');

    const diffBar = document.getElementById('detail-difficulty-bar');
    if (diffBar) {
        const pct = Math.min(100, Math.max(20, ((detail.difficulty || 3) * 20)));
        diffBar.style.width = pct + '%';
    }

    const recSemEl = document.getElementById('detail-recommended-semester');
    if (recSemEl) {
        recSemEl.textContent = detail.recommendedSemester ? ('Semester ' + detail.recommendedSemester) : 'Elective / Flexible';
    }

    const statusEl = document.getElementById('detail-course-status');
    if (statusEl) {
        if (detail.completed) statusEl.textContent = 'Completed ✓';
        else if (detail.enrolled) statusEl.textContent = 'Currently Enrolled 📚';
        else if (detail.inCart) statusEl.textContent = 'In Semester Cart 🛒';
        else if (detail.prerequisitesMet) statusEl.textContent = 'Prerequisites Satisfied ✅';
        else statusEl.textContent = 'Missing: ' + (detail.missingSummary || 'Prerequisites');
    }

    const prereqChainEl = document.getElementById('detail-prereq-chain');
    if (prereqChainEl) {
        if (Array.isArray(detail.prerequisiteChain) && detail.prerequisiteChain.length > 0) {
            prereqChainEl.innerHTML = detail.prerequisiteChain
                .map(code => `<span class="detail-chain-pill">${escapeHtml(code)}</span>`)
                .join('<span class="detail-chain-arrow">→</span>')
                + `<span class="detail-chain-arrow">→</span><span class="detail-chain-pill current">${escapeHtml(detail.code)}</span>`;
        } else {
            prereqChainEl.innerHTML = '<span class="detail-empty-chain">No prerequisites required</span>';
        }
    }

    const unlocksEl = document.getElementById('detail-unlocks-chain');
    if (unlocksEl) {
        if (Array.isArray(detail.unlocksCourseCodes) && detail.unlocksCourseCodes.length > 0) {
            unlocksEl.innerHTML = detail.unlocksCourseCodes
                .map(code => `<span class="detail-chain-pill">${escapeHtml(code)}</span>`)
                .join('');
        } else {
            unlocksEl.innerHTML = '<span class="detail-empty-chain">No downstream dependent courses</span>';
        }
    }

    const explainBtn = document.getElementById('detail-btn-explain');
    if (explainBtn) {
        explainBtn.onclick = () => {
            closeCourseDetailModal();
            if (window.openCopilotWithPrompt) {
                window.openCopilotWithPrompt('Explain course ' + detail.code, true);
            }
        };
    }

    const addBtn = document.getElementById('detail-btn-add-cart');
    if (addBtn) {
        if (detail.enrolled || detail.inCart || detail.completed) {
            addBtn.style.display = 'none';
        } else {
            addBtn.style.display = 'inline-flex';
            addBtn.onclick = () => {
                closeCourseDetailModal();
                addCourse(detail.id);
            };
        }
    }
}

function closeCourseDetailModal() {
    const modal = document.getElementById('course-detail-modal');
    if (modal) {
        modal.style.display = 'none';
    }
}

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        closeCourseDetailModal();
    }
});

const courseDetailBackdrop = document.getElementById('course-detail-modal');
if (courseDetailBackdrop) {
    courseDetailBackdrop.addEventListener('click', (e) => {
        if (e.target === courseDetailBackdrop) {
            closeCourseDetailModal();
        }
    });
}

function switchPlannerView(viewName) {
    const cartSection = document.getElementById('cart-panel-section');
    const roadmapSection = document.getElementById('roadmap-board-section');
    const cartTab = document.getElementById('tab-cart-view');
    const roadmapTab = document.getElementById('tab-roadmap-view');
    const isRoadmap = viewName === 'roadmap';

    if (cartSection) cartSection.style.display = isRoadmap ? 'none' : 'block';
    if (roadmapSection) roadmapSection.style.display = isRoadmap ? 'block' : 'none';
    if (cartTab) {
        cartTab.classList.toggle('active', !isRoadmap);
        cartTab.setAttribute('aria-selected', String(!isRoadmap));
    }
    if (roadmapTab) {
        roadmapTab.classList.toggle('active', isRoadmap);
        roadmapTab.setAttribute('aria-selected', String(isRoadmap));
    }
}

let draggedRoadmapCourseId = null;

function handleRoadmapDragStart(event) {
    const card = event.currentTarget;
    draggedRoadmapCourseId = card ? card.getAttribute('data-course-id') : null;
    if (event.dataTransfer && draggedRoadmapCourseId) {
        event.dataTransfer.setData('text/plain', draggedRoadmapCourseId);
        event.dataTransfer.effectAllowed = 'move';
    }
}

function handleRoadmapDragOver(event) {
    event.preventDefault();
    if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move';
    }
    event.currentTarget.classList.add('drag-over');
}

function handleRoadmapDragLeave(event) {
    event.currentTarget.classList.remove('drag-over');
}

function handleRoadmapDrop(event) {
    event.preventDefault();
    const col = event.currentTarget;
    col.classList.remove('drag-over');
    const targetSemester = col.getAttribute('data-semester');
    const courseId = (event.dataTransfer && event.dataTransfer.getData('text/plain')) || draggedRoadmapCourseId;
    draggedRoadmapCourseId = null;
    if (courseId && targetSemester) {
        moveRoadmapCourse(courseId, targetSemester);
    }
}

function moveRoadmapCourse(courseId, targetSemester) {
    hideAlert();
    fetch('/planner/roadmap/move?courseId=' + encodeURIComponent(courseId) + '&targetSemester=' + encodeURIComponent(targetSemester), {
        method: 'POST',
        headers: Object.assign({ 'Accept': 'application/json' }, window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {})
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to move course on roadmap.');
                return null;
            }
            return data;
        })
        .then(board => {
            if (board) {
                renderRoadmapBoard(board);
            }
        })
        .catch(() => showAlert('Unable to update roadmap placement.'));
}

function removeRoadmapCourse(courseId) {
    hideAlert();
    fetch('/planner/roadmap/remove?courseId=' + encodeURIComponent(courseId), {
        method: 'POST',
        headers: Object.assign({ 'Accept': 'application/json' }, window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {})
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to remove course from roadmap.');
                return null;
            }
            return data;
        })
        .then(board => {
            if (board) {
                renderRoadmapBoard(board);
            }
        })
        .catch(() => showAlert('Unable to remove course from roadmap.'));
}

function applyRoadmapPacing() {
    hideAlert();
    const select = document.getElementById('pacing-strategy-select');
    const strategy = select ? select.value : 'BALANCED';
    fetch('/planner/roadmap/pacing?strategy=' + encodeURIComponent(strategy), {
        method: 'POST',
        headers: Object.assign({ 'Accept': 'application/json' }, window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {})
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to apply pacing strategy.');
                return null;
            }
            return data;
        })
        .then(board => {
            if (board) {
                renderRoadmapBoard(board);
            }
        })
        .catch(() => showAlert('Unable to rearrange roadmap right now.'));
}

function renderRoadmapBoard(board) {
    if (!board || !Array.isArray(board.semesters)) return;
    const totalCreditsEl = document.getElementById('roadmap-total-credits');
    const totalCoursesEl = document.getElementById('roadmap-total-courses');
    if (totalCreditsEl) totalCreditsEl.textContent = board.totalCredits;
    if (totalCoursesEl) totalCoursesEl.textContent = board.totalCourses;

    const pacingSummaryEl = document.getElementById('roadmap-pacing-summary');
    if (pacingSummaryEl && board.pacingSummary) {
        pacingSummaryEl.textContent = board.pacingSummary;
    }
    const pacingSelect = document.getElementById('pacing-strategy-select');
    if (pacingSelect && board.activePacingStrategy) {
        pacingSelect.value = board.activePacingStrategy;
    }

    const prereqConflictEl = document.getElementById('roadmap-prereq-conflicts');
    if (prereqConflictEl) {
        const count = board.prerequisiteConflictCount || 0;
        prereqConflictEl.className = 'validation-pill ' + (count > 0 ? 'pill-danger' : 'pill-ok');
        prereqConflictEl.textContent = count > 0 ? (`⚠️ ${count} Prereq Conflict(s)`) : '✓ Prereqs Valid';
    }

    const overloadEl = document.getElementById('roadmap-overload-count');
    if (overloadEl) {
        const ov = board.overloadedSemesterCount || 0;
        overloadEl.className = 'validation-pill ' + (ov > 0 ? 'pill-warn' : 'pill-ok');
        overloadEl.textContent = ov > 0 ? (`⚠️ ${ov} Overloaded Semester(s)`) : '✓ Load ≤ 30 ECTS';
    }

    const seasonConflictEl = document.getElementById('roadmap-season-conflicts');
    if (seasonConflictEl) {
        const sc = board.seasonConflictCount || 0;
        seasonConflictEl.className = 'validation-pill ' + (sc > 0 ? 'pill-warn' : 'pill-ok');
        seasonConflictEl.textContent = sc > 0 ? (`⚠️ ${sc} Season Mismatch(es)`) : '✓ Seasons Compatible';
    }

    const grid = document.getElementById('roadmap-grid');
    if (!grid) return;

    grid.innerHTML = '';
    board.semesters.forEach(sem => {
        const col = document.createElement('div');
        col.className = 'roadmap-semester-col' + (sem.overloaded ? ' semester-overloaded' : '');
        col.id = 'roadmap-semester-' + sem.semester;
        col.setAttribute('data-semester', String(sem.semester));
        col.setAttribute('data-season', sem.season || 'FALL');
        col.setAttribute('ondragover', 'handleRoadmapDragOver(event)');
        col.setAttribute('ondragleave', 'handleRoadmapDragLeave(event)');
        col.setAttribute('ondrop', 'handleRoadmapDrop(event)');

        const cardsHtml = Array.isArray(sem.courses) && sem.courses.length > 0
            ? sem.courses.map(card => {
                const violationHtml = card.prerequisiteViolation
                    ? `<div class="badge-roadmap-violation">⚠️ Missing/out-of-order prereq: ${escapeHtml((card.violatedPrerequisites || []).join(', '))}</div>`
                    : '';
                const seasonWarningHtml = card.seasonMismatch
                    ? `<div class="badge-season-conflict">🗓️ ${escapeHtml(card.seasonWarning || 'Season mismatch')}</div>`
                    : '';
                const seasonLabel = card.season === 'FALL' ? 'Fall Only' : (card.season === 'SPRING' ? 'Spring Only' : 'Fall/Spring');
                const moveControlsHtml = card.status !== 'COMPLETED'
                    ? `<div class="roadmap-card-move-controls">
                           <select class="roadmap-move-select" aria-label="Move course to semester" data-course-id="${card.courseId}" onchange="moveRoadmapCourse(this.getAttribute('data-course-id'), this.value)">
                               ${[1,2,3,4,5,6,7,8].map(s => `<option value="${s}" ${s === sem.semester ? 'selected' : ''}>Sem ${s}</option>`).join('')}
                           </select>
                           <button type="button" class="btn-roadmap-remove" title="Remove from roadmap" data-course-id="${card.courseId}" onclick="removeRoadmapCourse(this.getAttribute('data-course-id'))">×</button>
                       </div>`
                    : '';
                const cardClasses = 'roadmap-course-card'
                    + (card.prerequisiteViolation ? ' card-prereq-violation' : '')
                    + (card.seasonMismatch ? ' card-season-mismatch' : '');
                return `
                    <div class="${cardClasses}" id="roadmap-card-${card.courseId}" data-course-id="${card.courseId}" data-semester="${sem.semester}" data-season-mismatch="${Boolean(card.seasonMismatch)}" draggable="true" ondragstart="handleRoadmapDragStart(event)">
                        <div class="roadmap-card-top">
                            <span class="course-code">${escapeHtml(card.code)}</span>
                            <span class="credit-tag">${card.credits} ECTS</span>
                        </div>
                        <div class="roadmap-card-name">${escapeHtml(card.name)}</div>
                        <div class="roadmap-card-badges">
                            <span class="badge-type">${escapeHtml(card.category || card.courseType)}</span>
                            <span class="badge-season">${seasonLabel}</span>
                            <span class="roadmap-status-badge status-${escapeHtml((card.status || 'planned').toLowerCase())}">${escapeHtml(card.status)}</span>
                        </div>
                        ${violationHtml}
                        ${seasonWarningHtml}
                        <div class="roadmap-card-actions">
                            <button type="button" class="btn-course-details" data-course-id="${card.courseId}" onclick="openCourseDetailModal(${card.courseId})">Details</button>
                            ${moveControlsHtml}
                        </div>
                    </div>
                `;
            }).join('')
            : '<div class="roadmap-empty-slot">Drop a course here</div>';

        const stateClass = 'state-' + (sem.workloadState || 'light').toLowerCase();
        const overloadBadge = sem.overloaded ? '<span class="badge-overload-warning">⚠️ Overload (&gt;30 ECTS)</span>' : '';
        col.innerHTML = `
            <div class="roadmap-semester-header">
                <div>
                    <h3 class="roadmap-sem-title">${escapeHtml(sem.title)}</h3>
                    <span class="roadmap-sem-load ${stateClass}">${escapeHtml(sem.workloadState)}</span>
                    ${overloadBadge}
                </div>
                <span class="roadmap-sem-credits">${sem.totalCredits} ECTS</span>
            </div>
            <div class="roadmap-semester-cards">${cardsHtml}</div>
        `;
        grid.appendChild(col);
    });
}

function setDraftName(name) {
    const input = document.getElementById('draft-name-input');
    if (input) {
        input.value = name;
        input.focus();
    }
}

function savePlanDraft() {
    hideAlert();
    const input = document.getElementById('draft-name-input');
    const name = input ? input.value.trim() : '';
    if (!name) {
        showAlert('Please enter a draft name (e.g. Safe plan or Ambitious plan).');
        return;
    }
    const pacingSelect = document.getElementById('pacing-strategy-select');
    const strategy = pacingSelect ? pacingSelect.value : 'BALANCED';

    fetch('/planner/drafts/save?name=' + encodeURIComponent(name) + '&pacingStrategy=' + encodeURIComponent(strategy), {
        method: 'POST',
        headers: Object.assign({ 'Accept': 'application/json' }, window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {})
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to save plan draft.');
                return null;
            }
            return data;
        })
        .then(data => {
            if (data && Array.isArray(data.drafts)) {
                if (input) input.value = '';
                renderPlanDrafts(data.drafts);
            }
        })
        .catch(() => showAlert('Unable to save plan draft.'));
}

function loadPlanDraft(draftId) {
    hideAlert();
    fetch('/planner/drafts/' + encodeURIComponent(draftId) + '/load', {
        method: 'POST',
        headers: Object.assign({ 'Accept': 'application/json' }, window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {})
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to load plan draft.');
                return null;
            }
            return data;
        })
        .then(data => {
            if (data) {
                if (data.cart) renderCart(data.cart);
                if (data.roadmap) renderRoadmapBoard(data.roadmap);
            }
        })
        .catch(() => showAlert('Unable to load plan draft.'));
}

function deletePlanDraft(draftId) {
    hideAlert();
    fetch('/planner/drafts/' + encodeURIComponent(draftId) + '/delete', {
        method: 'POST',
        headers: Object.assign({ 'Accept': 'application/json' }, window.advisorCsrfHeaders ? window.advisorCsrfHeaders() : {})
    })
        .then(async res => {
            if (res.status === 401) {
                window.location.href = '/login';
                return null;
            }
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                showAlert(data.error || 'Failed to delete plan draft.');
                return null;
            }
            return data;
        })
        .then(data => {
            if (data && Array.isArray(data.drafts)) {
                renderPlanDrafts(data.drafts);
            }
        })
        .catch(() => showAlert('Unable to delete plan draft.'));
}

function renderPlanDrafts(drafts) {
    const emptyEl = document.getElementById('saved-drafts-empty');
    const listEl = document.getElementById('saved-drafts-list');
    if (!listEl || !emptyEl) return;

    if (!Array.isArray(drafts) || drafts.length === 0) {
        emptyEl.style.display = 'block';
        listEl.style.display = 'none';
        listEl.innerHTML = '';
        return;
    }
    emptyEl.style.display = 'none';
    listEl.style.display = 'flex';
    listEl.innerHTML = drafts.map(d => `
        <li class="cart-item draft-item" id="draft-item-${d.id}">
            <div class="item-info">
                <div class="item-code-credits">
                    <span class="course-code">${escapeHtml(d.name)}</span>
                    <span class="credit-tag">${d.totalCredits} ECTS</span>
                    <span class="badge-type">${escapeHtml(d.pacingStrategy || 'BALANCED')}</span>
                </div>
                <span class="course-name">${escapeHtml(d.cartCodesSummary || 'Saved plan')}</span>
            </div>
            <div class="draft-item-actions">
                <button type="button" class="btn-course-details" data-draft-id="${d.id}" onclick="loadPlanDraft(${d.id})">Load</button>
                <button type="button" class="btn-cancel-enrolled" data-draft-id="${d.id}" onclick="deletePlanDraft(${d.id})">Delete</button>
            </div>
        </li>
    `).join('');
}
