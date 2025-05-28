document.addEventListener('DOMContentLoaded', function () {
    const emailListUL = document.querySelector('.email-list');
    const logoutButton = document.getElementById('logout-link');

    const API_BASE_URL = 'http://localhost:8080/api';

    const messageContainer = document.getElementById('dashboard-message-container');
    const dashboardErrorEl = document.getElementById('dashboard-error-message');
    const dashboardInfoEl = document.getElementById('dashboard-info-message');

    const schedulerModal = document.getElementById('schedulerModal');
    const closeSchedulerModalBtn = document.getElementById('closeSchedulerModalBtn');
    const cancelSchedulerModalBtn = document.getElementById('cancelSchedulerModalBtn');
    const schedulerForm = document.getElementById('schedulerForm');
    const schedulerEmailSubjectEl = document.getElementById('schedulerEmailSubject');
    const reminderDateTimeInput = document.getElementById('reminderDateTime');
    const reminderNotesInput = document.getElementById('reminderNotes');
    const reminderPriorityModalInput = document.getElementById('reminderPriorityModal');
    let currentSchedulingEmailData = null;

    function showMessage(element, messageText, isError = false, duration = 4000) {
        if (!element || !messageContainer) {
            if (isError) alert("Error: " + messageText); else alert("Info: " + messageText);
            return;
        }
        if (dashboardErrorEl && dashboardErrorEl.classList.contains('visible')) hideMessage(dashboardErrorEl, true);
        if (dashboardInfoEl && dashboardInfoEl.classList.contains('visible')) hideMessage(dashboardInfoEl, true);
        element.innerHTML = `<i class="fas ${isError ? 'fa-times-circle' : 'fa-check-circle'}"></i> ${messageText}`;
        element.className = `message-display ${isError ? 'error-message' : 'info-message'}`;
        messageContainer.style.display = 'block';
        element.style.display = 'block';
        requestAnimationFrame(() => { element.classList.add('visible'); });
        if (!isError && duration > 0) {
            setTimeout(() => hideMessage(element), duration);
        }
    }

    function hideMessage(element, immediate = false) {
        if (element && element.classList.contains('visible')) {
            element.classList.remove('visible');
            const handler = () => {
                if (!element.classList.contains('visible')) {
                    element.style.display = 'none'; element.innerHTML = '';
                    if (messageContainer && !dashboardErrorEl.classList.contains('visible') && !dashboardInfoEl.classList.contains('visible')) {
                        messageContainer.style.display = 'none';
                    }
                }
                element.removeEventListener('transitionend', handler);
            };
            if (immediate) {
                handler();
            } else {
                element.addEventListener('transitionend', handler, { once: true });
                setTimeout(() => { if (element && !element.classList.contains('visible')) { handler(); } }, 450);
            }
        } else if (element) {
            element.style.display = 'none'; element.innerHTML = '';
            if (messageContainer && dashboardErrorEl && dashboardInfoEl && !dashboardErrorEl.classList.contains('visible') && !dashboardInfoEl.classList.contains('visible')) {
                messageContainer.style.display = 'none';
            }
        }
    }

    function showDashboardError(message) { showMessage(dashboardErrorEl, message, true, 0); }
    function showDashboardInfo(message) { showMessage(dashboardInfoEl, message, false); }
    function clearAllMessages() { hideMessage(dashboardErrorEl, true); hideMessage(dashboardInfoEl, true); }

    async function apiFetch(url, options = {}) {
        clearAllMessages();
        try {
            const defaultOptions = {
                credentials: 'include',
                headers: { 'Accept': 'application/json', 'Content-Type': 'application/json', ...(options.headers || {}), },
            };
            const finalOptions = { ...defaultOptions, ...options };
            if (options.body && typeof options.body !== 'string') {
                finalOptions.body = JSON.stringify(options.body);
            }
            const response = await fetch(url, finalOptions);
            if (response.status === 401) {
                showDashboardError("Session expired or unauthorized. Redirecting to login...");
                setTimeout(() => window.location.href = '/login.html', 2500);
                throw new Error("Unauthorized");
            }
            const responseText = await response.text();
            if (!response.ok) {
                let errorDetail = `Server error: ${response.status} for ${url.split('/').pop()}`;
                if (responseText) {
                    try {
                        const errorData = JSON.parse(responseText);
                        errorDetail = errorData.error || errorData.message || errorDetail;
                    } catch (parseError) { errorDetail = responseText.substring(0, 150) || errorDetail; }
                }
                throw new Error(errorDetail);
            }
            if (response.status === 204 || !responseText) return null;
            return JSON.parse(responseText);
        } catch (error) {
            if (error.message !== "Unauthorized" && !dashboardErrorEl.classList.contains('visible')) {
                showDashboardError(error.message || "A network or server error occurred.");
            }
            throw error;
        }
    }

    function createEmailItemElement(email) {
        const item = document.createElement('li');
        item.className = 'email-item';
        item.dataset.emailId = email.id;
        item.emailData = { ...email };

        let displayDate = 'N/A';
        if (email.date) {
            try {
                displayDate = new Date(email.date).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
            } catch (e) { displayDate = email.date; }
        }

        item.innerHTML = `
            <div class="email-date">${displayDate}</div>
            <div class="email-sender">${email.sender || '(Unknown Sender)'}</div>
            <div class="email-info">
                <div class="email-subject">${email.subject || '(No Subject)'}</div>
                <div class="email-snippet">${email.snippet || ''}</div>
            </div>
            <div class="email-actions">
                <div class="priority-selector">
                    <button class="action-btn priority-btn high" title="High Priority" data-level="high"><i class="fas fa-fire"></i></button>
                    <button class="action-btn priority-btn medium" title="Medium Priority" data-level="medium"><i class="fas fa-exclamation-triangle"></i></button>
                    <button class="action-btn priority-btn low" title="Low Priority" data-level="low"><i class="fas fa-check-circle"></i></button>
                </div>
                <div class="schedule-options">
                    <button class="action-btn btn-schedule" title="Set Reminder"><i class="fas fa-clock"></i></button>
                    <button class="action-btn btn-add-calendar" title="Add to Calendar (Info)"><i class="fas fa-calendar-alt"></i></button>
                </div>
            </div>
        `;
        updateItemPriorityVisuals(item, email.currentPriority || 'none');
        attachActionListenersToItem(item);
        return item;
    }

    function updateItemPriorityVisuals(emailItemElement, priorityLevel) {
        emailItemElement.classList.remove('priority-high', 'priority-medium', 'priority-low');
        emailItemElement.dataset.priority = priorityLevel;
        emailItemElement.style.paddingLeft = '';

        if (priorityLevel && priorityLevel !== 'none') {
            emailItemElement.classList.add(`priority-${priorityLevel}`);
        }

        const priorityButtons = emailItemElement.querySelectorAll('.priority-selector .priority-btn');
        priorityButtons.forEach(btn => btn.classList.remove('active'));
        if (priorityLevel && priorityLevel !== 'none') {
            const activeButton = emailItemElement.querySelector(`.priority-btn[data-level="${priorityLevel}"]`);
            if (activeButton) activeButton.classList.add('active');
        }
    }

    function attachActionListenersToItem(emailItemElement) {
        const emailData = emailItemElement.emailData;
        const emailId = emailData.id;

        const priorityButtons = emailItemElement.querySelectorAll('.priority-selector .priority-btn');
        priorityButtons.forEach(button => {
            button.addEventListener('click', async function () {
                clearAllMessages();
                const newPriorityLevel = this.dataset.level;
                const oldPriorityLevel = emailData.currentPriority || 'none';
                updateItemPriorityVisuals(emailItemElement, newPriorityLevel);
                const payload = {
                    priority: newPriorityLevel,
                    subject: emailData.subject || "(No Subject)",
                    sender: emailData.sender || "(Unknown Sender)"
                };
                try {
                    const result = await apiFetch(`${API_BASE_URL}/emails/${emailId}/priority`, {
                        method: 'POST', body: payload
                    });
                    showDashboardInfo(result.message || 'Priority updated!');
                    emailData.currentPriority = newPriorityLevel;
                } catch (error) {
                    updateItemPriorityVisuals(emailItemElement, oldPriorityLevel);
                    emailData.currentPriority = oldPriorityLevel;
                }
            });
        });

        const scheduleButton = emailItemElement.querySelector('.btn-schedule');
        if (scheduleButton) {
            scheduleButton.addEventListener('click', () => { clearAllMessages(); openSchedulerModal(emailData); });
        }

        const calendarButton = emailItemElement.querySelector('.btn-add-calendar');
        if (calendarButton) {
            calendarButton.addEventListener('click', () => {
                clearAllMessages();
                if (emailData.reminderDateTime) {
                    let reminderDateStr = "N/A";
                    try {
                        reminderDateStr = new Date(emailData.reminderDateTime).toLocaleString(undefined, {
                            year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
                        });
                    } catch (e) { console.warn("Error formatting reminderDateTime for info display", e); }
                    showDashboardInfo(
                        `Reminder for "${emailData.subject || '(No Subject)'}" is set for: ${reminderDateStr}. ` +
                        (emailData.notes ? `Notes: "${emailData.notes}"` : "") +
                        " You can add this to your preferred calendar manually."
                    );
                } else {
                    showDashboardInfo(`No reminder set for "${emailData.subject || '(No Subject)'}". Use the clock icon to set one.`);
                }
            });
        }
    }

    function openSchedulerModal(emailData) {
        if (!schedulerModal || !schedulerForm || !schedulerEmailSubjectEl || !reminderDateTimeInput || !reminderNotesInput || !reminderPriorityModalInput) {
            showDashboardError("Scheduler UI elements missing."); return;
        }
        currentSchedulingEmailData = emailData;
        schedulerEmailSubjectEl.textContent = emailData.subject || '(No Subject)';
        let initialDateTimeValue = "";
        if (emailData.reminderDateTime) {
            try {
                const dateObj = new Date(emailData.reminderDateTime);
                if (!isNaN(dateObj.valueOf())) {
                     initialDateTimeValue = `${dateObj.getFullYear()}-${(dateObj.getMonth() + 1).toString().padStart(2, '0')}-${dateObj.getDate().toString().padStart(2, '0')}T${dateObj.getHours().toString().padStart(2, '0')}:${dateObj.getMinutes().toString().padStart(2, '0')}`;
                }
            } catch(e) { console.warn("Error parsing reminderDateTime for modal:", e); }
        }
        if (!initialDateTimeValue) {
            const defaultDate = new Date(Date.now() + 3600000); // 1 hour from now
            initialDateTimeValue = `${defaultDate.getFullYear()}-${(defaultDate.getMonth() + 1).toString().padStart(2, '0')}-${defaultDate.getDate().toString().padStart(2, '0')}T${defaultDate.getHours().toString().padStart(2, '0')}:${defaultDate.getMinutes().toString().padStart(2, '0')}`;
        }
        reminderDateTimeInput.value = initialDateTimeValue;
        reminderNotesInput.value = emailData.notes || "";
        reminderPriorityModalInput.value = emailData.currentPriority || "medium"; // Set current priority or default
        
        schedulerModal.style.display = 'flex';
        requestAnimationFrame(() => { schedulerModal.classList.add('active'); });
    }

    function closeSchedulerModal() {
        if (schedulerModal) {
            schedulerModal.classList.remove('active');
            schedulerModal.addEventListener('transitionend', function handler() {
                if (!schedulerModal.classList.contains('active')) {
                     schedulerModal.style.display = 'none';
                     if(schedulerForm) schedulerForm.reset();
                }
                schedulerModal.removeEventListener('transitionend', handler);
            }, {once: true});
             setTimeout(() => {
                 if (schedulerModal && !schedulerModal.classList.contains('active')) {
                    schedulerModal.style.display = 'none'; if(schedulerForm) schedulerForm.reset();
                 }
            }, 350);
        }
        currentSchedulingEmailData = null;
    }

    if(closeSchedulerModalBtn) closeSchedulerModalBtn.addEventListener('click', closeSchedulerModal);
    if(cancelSchedulerModalBtn) cancelSchedulerModalBtn.addEventListener('click', closeSchedulerModal);
    if(schedulerModal) schedulerModal.addEventListener('click', (e) => { if (e.target === schedulerModal) closeSchedulerModal(); });

    if (schedulerForm) {
        schedulerForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            if (!currentSchedulingEmailData) return;

            const reminderDateTimeValue = reminderDateTimeInput.value;
            const notes = reminderNotesInput.value;
            const priority = reminderPriorityModalInput.value; // Get priority from modal
            const { id: emailId, subject, sender } = currentSchedulingEmailData;

            if (!reminderDateTimeValue) { showDashboardError("Please select a valid date and time."); return; }
            const reminderDateTimeISO = reminderDateTimeValue.length === 16 ? reminderDateTimeValue + ":00" : reminderDateTimeValue;

            try {
                const result = await apiFetch(`${API_BASE_URL}/emails/${emailId}/set-schedule-priority`, { // Use combined endpoint
                    method: 'POST',
                    body: {
                        reminderDateTime: reminderDateTimeISO,
                        priority: priority, // Send priority
                        notes: notes,
                        subject: subject || "(No Subject)",
                        sender: sender || "(Unknown Sender)"
                    }
                });
                if (result) {
                    showDashboardInfo(result.message || 'Reminder and priority saved!');
                    const originalEmailItem = emailListUL.querySelector(`.email-item[data-email-id="${emailId}"]`);
                    if(originalEmailItem && originalEmailItem.emailData && result.data) {
                        originalEmailItem.emailData.reminderDateTime = result.data.reminderDateTime;
                        originalEmailItem.emailData.notes = result.data.notes;
                        originalEmailItem.emailData.currentPriority = result.data.priority;
                        updateItemPriorityVisuals(originalEmailItem, result.data.priority);
                    }
                }
            } catch (error) { /* apiFetch already showed error */ }
            closeSchedulerModal();
        });
    }

    async function loadEmails() {
        clearAllMessages();
        if (!emailListUL) {
            if(document.body) showDashboardError('UI Error: Email list container missing.');
            return;
        }
        emailListUL.innerHTML = '<li class="loading-message"><i class="fas fa-spinner fa-spin"></i> Loading emails...</li>';
        try {
            const emails = await apiFetch(`${API_BASE_URL}/emails`);
            emailListUL.innerHTML = '';
            if (emails && emails.length > 0) {
                emails.forEach(email => {
                    const emailItemElement = createEmailItemElement(email);
                    emailListUL.appendChild(emailItemElement);
                });
            } else if (emails) {
                emailListUL.innerHTML = '<li class="empty-message"><i class="fas fa-envelope-open"></i> No emails to display.</li>';
            } else {
                 if (!dashboardErrorEl.classList.contains('visible') && !dashboardInfoEl.classList.contains('visible')) {
                    showDashboardInfo("No emails to display or an issue occurred.");
                 }
                 if (emailListUL.querySelector('.loading-message')) {
                    emailListUL.innerHTML = '<li class="empty-message"><i class="fas fa-info-circle"></i> No emails found.</li>';
                 }
            }
        } catch (error) {
            if (emailListUL.querySelector('.loading-message')) {
                emailListUL.innerHTML = `<li class="error-message-li"><i class="fas fa-exclamation-triangle"></i> Error loading emails: ${error.message}</li>`;
            }
        }
    }

    if (logoutButton) {
        logoutButton.addEventListener('click', async function (event) {
            event.preventDefault(); clearAllMessages();
            try {
                await apiFetch('/logout', { method: 'POST' });
                showDashboardInfo("Logging you out...");
                setTimeout(() => { window.location.href = '/login.html?logout'; }, 1500);
            } catch (error) {
                if (error.message !== "Unauthorized") showDashboardError('Logout failed. Please try again.');
                setTimeout(() => { window.location.href = '/login.html?logoutFailed'; }, 2000);
            }
        });
    } else { console.warn('Logout button element not found.'); }

    loadEmails();
});