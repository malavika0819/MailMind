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
    const schedulerModalTitle = document.getElementById('schedulerModalTitle');
    const schedulerEmailSubjectEl = document.getElementById('schedulerEmailSubject');
    const reminderDateTimeInput = document.getElementById('reminderDateTime');
    const reminderNotesInput = document.getElementById('reminderNotes');
    const reminderPriorityModalInput = document.getElementById('reminderPriorityModal');
    let currentSchedulingEmailData = null;

    const contentSections = document.querySelectorAll('.content-section');
    const navItems = document.querySelectorAll('.sidebar-nav .nav-item');
    const mainViewTitle = document.getElementById('main-view-title');
    const mainViewSubtitle = document.getElementById('main-view-subtitle');

    const scheduledRemindersListUL = document.getElementById('scheduled-reminders-list');
    const noRemindersMessage = document.getElementById('no-reminders-message');
    const userProfileDetailsDiv = document.getElementById('user-profile-details');

    const urlParams = new URLSearchParams(window.location.search);
    const isDemoMode = urlParams.get('demo') === 'true';

    function showMessage(element, messageText, isError = false, duration = 4000) {
        if (!element || !messageContainer) return;
        hideMessage(isError ? dashboardInfoEl : dashboardErrorEl, true);
        element.innerHTML = `<i class="fas ${isError ? 'fa-times-circle' : 'fa-check-circle'}"></i> ${messageText}`;
        element.className = `message-display ${isError ? 'error-message' : 'info-message'}`;
        messageContainer.style.display = 'block';
        element.style.display = 'block';
        requestAnimationFrame(() => element.classList.add('visible'));
        if (!isError && duration > 0) setTimeout(() => hideMessage(element), duration);
    }

    function hideMessage(element, immediate = false) {
        if (!element || !element.classList.contains('visible')) return;
        element.classList.remove('visible');
        const handler = () => {
            if (!element.classList.contains('visible')) {
                element.style.display = 'none';
                if (messageContainer && dashboardErrorEl && dashboardInfoEl && !dashboardErrorEl.classList.contains('visible') && !dashboardInfoEl.classList.contains('visible')) {
                    messageContainer.style.display = 'none';
                }
            }
        };
        if (immediate) return handler();
        const fallback = setTimeout(handler, 450);
        element.addEventListener('transitionend', () => { clearTimeout(fallback); handler(); }, { once: true });
    }

    function showDashboardError(message) { showMessage(dashboardErrorEl, message, true, 0); }
    function showDashboardInfo(message) { showMessage(dashboardInfoEl, message, false); }
    function clearAllMessages() { hideMessage(dashboardErrorEl, true); hideMessage(dashboardInfoEl, true); }

    async function apiFetch(url, options = {}) {
        clearAllMessages();
        try {
            const defaultOptions = { credentials: 'include', headers: { 'Accept': 'application/json', 'Content-Type': 'application/json', ...(options.headers || {}), }, };
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

    function showView(viewId) {
        if (!contentSections || !navItems) return;
        contentSections.forEach(section => { section.style.display = section.id === viewId ? 'block' : 'none'; });
        navItems.forEach(item => { item.classList.toggle('active', item.dataset.view === viewId); });

        if (mainViewTitle && mainViewSubtitle) {
            switch (viewId) {
                case 'my-reminders-view':
                    mainViewTitle.innerHTML = '<i class="fas fa-calendar-check"></i> My Reminders';
                    mainViewSubtitle.textContent = 'View and manage all your scheduled reminders.';
                    loadScheduledReminders();
                    break;
                case 'profile-view':
                    mainViewTitle.innerHTML = '<i class="fas fa-id-card"></i> Profile';
                    mainViewSubtitle.textContent = 'View and manage your account details.';
                    loadUserProfile();
                    break;
                default:
                    mainViewTitle.innerHTML = '<i class="fas fa-inbox"></i> Inbox Feed';
                    mainViewSubtitle.textContent = 'Here\'s your latest email overview.';
                    if (emailListUL && !emailListUL.hasChildNodes()) loadEmails();
                    break;
            }
        }
    }

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const viewName = item.dataset.view;
            if (viewName) showView(viewName);
        });
    });

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
            <div class="email-col-date email-date">${displayDate}</div>
            <div class="email-col-sender email-sender">${email.sender || '(Unknown Sender)'}</div>
            <div class="email-col-subject email-info">
                <div class="email-subject">${email.subject || '(No Subject)'}</div>
                <div class="email-snippet">${email.snippet || ''}</div>
            </div>
            <div class="email-col-actions email-actions">
                <div class="priority-selector">
                    <button class="action-btn priority-btn high" title="High Priority" data-level="high"><i class="fas fa-fire"></i></button>
                    <button class="action-btn priority-btn medium" title="Medium Priority" data-level="medium"><i class="fas fa-exclamation-triangle"></i></button>
                    <button class="action-btn priority-btn low" title="Low Priority" data-level="low"><i class="fas fa-check-circle"></i></button>
                </div>
                <div class="schedule-options">
                    <button class="action-btn btn-schedule" title="Set Reminder"><i class="fas fa-clock"></i></button>
                    <button class="action-btn btn-add-calendar" title="Add to Calendar (Info)"><i class="fas fa-calendar-alt"></i></button>
                </div>
            </div>`;
        updateItemPriorityVisuals(item, email.currentPriority || 'none');
        attachActionListenersToItem(item);
        return item;
    }

    function updateItemPriorityVisuals(emailItemElement, priorityLevel) {
        emailItemElement.classList.remove('priority-high', 'priority-medium', 'priority-low');
        emailItemElement.dataset.priority = priorityLevel;
        emailItemElement.style.paddingLeft = ''; // Let CSS handle padding based on class

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
                const newPriorityLevel = this.dataset.level;
                if (isDemoMode) {
                    showDashboardInfo(`Priority set to ${newPriorityLevel}. (Demo Mode)`);
                    updateItemPriorityVisuals(emailItemElement, newPriorityLevel);
                    emailData.currentPriority = newPriorityLevel;
                    return;
                }
                const oldPriorityLevel = emailData.currentPriority || 'none';
                updateItemPriorityVisuals(emailItemElement, newPriorityLevel);
                const payload = { priority: newPriorityLevel, subject: emailData.subject, sender: emailData.sender };
                try {
                    const result = await apiFetch(`${API_BASE_URL}/emails/${emailId}/priority`, { method: 'POST', body: payload });
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
                if (emailData.subject) {
                    const title = encodeURIComponent("Follow up: " + emailData.subject);
                    let details = `Email from: ${emailData.sender || 'N/A'}\nSnippet: ${emailData.snippet || ''}`;
                    if(emailData.notes) details += `\n\nYour notes: ${emailData.notes}`;
                    details = encodeURIComponent(details);
                    let gCalUrl = `https://www.google.com/calendar/render?action=TEMPLATE&text=${title}&details=${details}`;
                    if (emailData.reminderDateTime) {
                        try {
                            const reminderDate = new Date(emailData.reminderDateTime);
                            const startDate = reminderDate.toISOString().replace(/-|:|\.\d+/g, "");
                            const endDate = new Date(reminderDate.getTime() + 60 * 60 * 1000).toISOString().replace(/-|:|\.\d+/g, "");
                            gCalUrl += `&dates=${startDate}/${endDate}`;
                        } catch(e) { console.warn("Could not format date for calendar link", e); }
                    }
                    window.open(gCalUrl, '_blank');
                    showDashboardInfo("Opening Google Calendar event creation link...");
                } else {
                    showDashboardError("Email details missing for calendar event.");
                }
            });
        }
    }

    function openSchedulerModal(emailDataToSchedule) {
        if (!schedulerModal || !schedulerForm || !schedulerEmailSubjectEl || !reminderDateTimeInput || !reminderNotesInput || !reminderPriorityModalInput || !schedulerModalTitle) {
            showDashboardError("Scheduler UI elements missing."); return;
        }
        currentSchedulingEmailData = emailDataToSchedule;
        schedulerModalTitle.innerHTML = '<i class="fas fa-clock"></i> Set Reminder';
        schedulerEmailSubjectEl.textContent = emailDataToSchedule.subject || '(No Subject)';
        let initialDateTimeValue = "";
        if (emailDataToSchedule.reminderDateTime) {
            try {
                const dateObj = new Date(emailDataToSchedule.reminderDateTime);
                if (!isNaN(dateObj.valueOf())) {
                     initialDateTimeValue = `${dateObj.getFullYear()}-${(dateObj.getMonth() + 1).toString().padStart(2, '0')}-${dateObj.getDate().toString().padStart(2, '0')}T${dateObj.getHours().toString().padStart(2, '0')}:${dateObj.getMinutes().toString().padStart(2, '0')}`;
                }
            } catch(e) { console.warn("Error parsing reminderDateTime for modal:", e); }
        }
        if (!initialDateTimeValue) {
            const defaultDate = new Date(Date.now() + 3600000);
            initialDateTimeValue = `${defaultDate.getFullYear()}-${(defaultDate.getMonth() + 1).toString().padStart(2, '0')}-${defaultDate.getDate().toString().padStart(2, '0')}T${defaultDate.getHours().toString().padStart(2, '0')}:${defaultDate.getMinutes().toString().padStart(2, '0')}`;
        }
        reminderDateTimeInput.value = initialDateTimeValue;
        reminderNotesInput.value = emailDataToSchedule.notes || "";
        reminderPriorityModalInput.value = emailDataToSchedule.currentPriority || "medium";
        schedulerModal.style.display = 'flex';
        requestAnimationFrame(() => { schedulerModal.classList.add('active'); });
    }

    function closeSchedulerModal() {
        if (schedulerModal) {
            schedulerModal.classList.remove('active');
            const handler = () => {
                if (!schedulerModal.classList.contains('active')) {
                     schedulerModal.style.display = 'none'; if(schedulerForm) schedulerForm.reset();
                }
                schedulerModal.removeEventListener('transitionend', handler);
            };
            schedulerModal.addEventListener('transitionend', handler, {once: true});
            setTimeout(() => { if (schedulerModal && !schedulerModal.classList.contains('active')) { handler(); } }, 350);
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
            const priority = reminderPriorityModalInput.value;
            const { id: emailId, subject, sender } = currentSchedulingEmailData;
            if (!reminderDateTimeValue) { showDashboardError("Please select a valid date and time."); return; }
            const reminderDateTimeISO = reminderDateTimeValue.length === 16 ? reminderDateTimeValue + ":00" : reminderDateTimeValue;
            
            if (isDemoMode) {
                showDashboardInfo("Reminder saved! (Demo Mode)");
                closeSchedulerModal();
                return;
            }
            const bodyPayload = {
                reminderDateTime: reminderDateTimeISO, priority: priority, notes: notes,
                subject: subject || "(No Subject)", sender: sender || "(Unknown Sender)"
            };
            try {
                const result = await apiFetch(`${API_BASE_URL}/emails/${emailId}/set-schedule-priority`, { method: 'POST', body: bodyPayload });
                if (result) {
                    showDashboardInfo(result.message || 'Reminder and priority saved!');
                    const originalEmailItem = emailListUL?.querySelector(`.email-item[data-email-id="${emailId}"]`);
                    if (originalEmailItem && originalEmailItem.emailData && result.data) {
                        originalEmailItem.emailData.reminderDateTime = result.data.reminderDateTime;
                        originalEmailItem.emailData.notes = result.data.notes;
                        originalEmailItem.emailData.currentPriority = result.data.priority;
                        updateItemPriorityVisuals(originalEmailItem, result.data.priority);
                    }
                }
            } catch (error) { /* apiFetch shows the error */ }
            closeSchedulerModal();
        });
    }

    // --- RESTRUCTURED Data Loading Logic ---

    async function loadEmails() {
        if (isDemoMode) {
            loadDummyEmails();
        } else {
            await fetchRealEmails();
        }
    }

    async function fetchRealEmails() {
        if (!emailListUL) { if (document.body) showDashboardError('UI Error: Email list container missing.'); return; }
        emailListUL.innerHTML = '<li class="loading-message"><i class="fas fa-spinner fa-spin"></i> Loading your emails...</li>';
        try {
            const emails = await apiFetch(`${API_BASE_URL}/emails`);
            if (emailListUL) emailListUL.innerHTML = '';
            if (emails && emails.length > 0) {
                emails.forEach(email => emailListUL.appendChild(createEmailItemElement(email)));
            } else if (emails) {
                emailListUL.innerHTML = '<li class="empty-message"><i class="fas fa-envelope-open"></i> No important emails to display.</li>';
            }
        } catch (error) {
            if (emailListUL.querySelector('.loading-message')) {
                emailListUL.innerHTML = `<li class="error-message-li"><i class="fas fa-exclamation-triangle"></i> Error loading emails: ${error.message}</li>`;
            }
        }
    }

    function loadDummyEmails() {
        if (!emailListUL) return;
        emailListUL.innerHTML = '<li class="loading-message"><i class="fas fa-spinner fa-spin"></i> Loading demo emails...</li>';
        setTimeout(() => {
            const dummyEmails = [
                { id: "demo_1", date: new Date(Date.now() - 12 * 3600 * 1000).toISOString(), sender: "Project Manager", subject: "Action Required: Project Phoenix Update", snippet: "Please review the latest sprint planning documents...", currentPriority: "high" },
                { id: "demo_2", date: new Date(Date.now() - 24 * 3600 * 1000).toISOString(), sender: "HR Department", subject: "Company-wide Town Hall Meeting", snippet: "A friendly reminder that our quarterly town hall is scheduled...", currentPriority: "medium" },
                { id: "demo_3", date: new Date(Date.now() - 2 * 24 * 3600 * 1000).toISOString(), sender: "Cloud Services Weekly", subject: "Your Weekly Cloud Usage Report", snippet: "Here is your summary for the past week. No action is required.", currentPriority: "low" },
                { id: "demo_4", date: new Date(Date.now() - 3 * 24 * 3600 * 1000).toISOString(), sender: "System Alert", subject: "Your scheduled backup was successful", snippet: "The nightly backup of your account data completed successfully.", currentPriority: "none" }
            ];
            if(emailListUL) emailListUL.innerHTML = '';
            dummyEmails.forEach(email => emailListUL.appendChild(createEmailItemElement(email)));
        }, 800);
    }

    async function loadScheduledReminders() {
        if (!scheduledRemindersListUL || !noRemindersMessage) return;
        scheduledRemindersListUL.innerHTML = '<li><i class="fas fa-spinner fa-spin"></i> Loading reminders...</li>';
        noRemindersMessage.style.display = 'none';
        if (isDemoMode) {
            scheduledRemindersListUL.innerHTML = '';
            noRemindersMessage.textContent = 'Reminders are available in live mode.';
            noRemindersMessage.style.display = 'block';
            return;
        }
        try {
            const reminders = await apiFetch(`${API_BASE_URL}/reminders/upcoming`);
            scheduledRemindersListUL.innerHTML = '';
            if (reminders && reminders.length > 0) {
                noRemindersMessage.style.display = 'none';
                reminders.forEach(reminder => {
                    const li = document.createElement('li');
                    li.className = 'reminder-item';
                    const reminderDate = new Date(reminder.reminderDateTime).toLocaleString();
                    li.innerHTML = `<strong>${reminder.subject || '(No Subject)'}</strong><br><small>Due: ${reminderDate}</small>`;
                    scheduledRemindersListUL.appendChild(li);
                });
            } else {
                noRemindersMessage.style.display = 'block';
            }
        } catch (error) {
            scheduledRemindersListUL.innerHTML = `<li class="error-message-li">Error loading reminders: ${error.message}</li>`;
        }
    }

    async function loadUserProfile() {
        if (!userProfileDetailsDiv) return;
        userProfileDetailsDiv.innerHTML = '<p><i class="fas fa-spinner fa-spin"></i> Loading profile...</p>';
        if (isDemoMode) {
            userProfileDetailsDiv.innerHTML = `<p><strong>Name:</strong> Guest User</p><p><strong>Email:</strong> guest@example.com</p>`;
            return;
        }
        try {
            const profile = await apiFetch(`${API_BASE_URL}/profile`);
            if (profile) {
                userProfileDetailsDiv.innerHTML = `<p><strong>Display Name:</strong> ${profile.displayName || 'N/A'}</p><p><strong>Email:</strong> ${profile.email || 'N/A'}</p>`;
            }
        } catch (error) {
            userProfileDetailsDiv.innerHTML = `<p class="error-message-li">Could not load profile: ${error.message}</p>`;
        }
    }

    if (logoutButton) {
        logoutButton.addEventListener('click', async function (event) {
            event.preventDefault(); clearAllMessages();
            if (isDemoMode) {
                window.location.href = 'index.html'; return;
            }
            try {
                await apiFetch('/logout', { method: 'POST' });
                showDashboardInfo("Logging you out...");
                setTimeout(() => { window.location.href = '/login.html?logout'; }, 1500);
            } catch (error) {
                setTimeout(() => { window.location.href = '/login.html?logoutFailed'; }, 2000);
            }
        });
    } else { console.warn('Logout button element not found.'); }

    function initializeApp() {
        if (isDemoMode) {
            if (mainViewTitle) mainViewTitle.innerHTML += ' <span class="demo-badge">(Demo Mode)</span>';
            showDashboardInfo("You are viewing a demo. Data is not real and will not be saved.");
        }
        showView('email-dashboard-view');
        loadEmails();
    }

    initializeApp();
});
