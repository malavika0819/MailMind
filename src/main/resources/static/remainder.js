document.addEventListener('DOMContentLoaded', function () {
    const remindersListUL = document.getElementById('remindersListUL');
    const noRemindersMessage = document.getElementById('no-reminders-message');
    const logoutButton = document.getElementById('logout-link-reminders'); // Specific ID for this page

    const API_BASE_URL = 'http://localhost:8080/api'; // Your Spring Boot backend

    // Message Display Elements
    const messageContainer = document.getElementById('reminders-message-container');
    const remindersErrorEl = document.getElementById('reminders-error-message');
    const remindersInfoEl = document.getElementById('reminders-info-message');

    // Scheduler/Edit Modal Elements (using 'Reminders' suffix for IDs from HTML)
    const schedulerModal = document.getElementById('schedulerModal'); // Reusing the same modal structure
    const closeSchedulerModalBtn = document.getElementById('closeSchedulerModalBtnReminders');
    const cancelSchedulerModalBtn = document.getElementById('cancelSchedulerModalBtnReminders');
    const schedulerForm = document.getElementById('schedulerFormReminders');
    const schedulerModalTitle = document.getElementById('schedulerModalTitle');
    const schedulerEmailSubjectEl = document.getElementById('schedulerEmailSubjectReminders');
    const reminderDateTimeInput = document.getElementById('reminderDateTimeReminders');
    const reminderNotesInput = document.getElementById('reminderNotesReminders');
    const reminderPriorityModalInput = document.getElementById('reminderPriorityModalReminders');
    let currentEditingReminderData = null; // To store reminder data for the modal

    // --- Message Handling (Copied from Dashboard.js - consider a shared utility JS file later) ---
    function showMessage(element, messageText, isError = false, duration = 4000) {
        if (!element || !messageContainer) {
            console.error("Message display elements not found. Fallback alert:", messageText);
            if (isError) alert("Error: " + messageText); else alert("Info: " + messageText);
            return;
        }
        if (remindersErrorEl && remindersErrorEl.classList.contains('visible')) hideMessage(remindersErrorEl, true);
        if (remindersInfoEl && remindersInfoEl.classList.contains('visible')) hideMessage(remindersInfoEl, true);
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
                    if (messageContainer && !remindersErrorEl.classList.contains('visible') && !remindersInfoEl.classList.contains('visible')) {
                        messageContainer.style.display = 'none';
                    }
                }
                element.removeEventListener('transitionend', handler);
            };
            if (immediate) { handler(); } else {
                element.addEventListener('transitionend', handler, { once: true });
                setTimeout(() => { if (element && !element.classList.contains('visible')) { handler(); } }, 450);
            }
        } else if (element) {
            element.style.display = 'none'; element.innerHTML = '';
            if (messageContainer && remindersErrorEl && remindersInfoEl && !remindersErrorEl.classList.contains('visible') && !remindersInfoEl.classList.contains('visible')) {
                messageContainer.style.display = 'none';
            }
        }
    }
    function showRemindersError(message) { showMessage(remindersErrorEl, message, true, 0); }
    function showRemindersInfo(message) { showMessage(remindersInfoEl, message, false); }
    function clearAllMessages() { hideMessage(remindersErrorEl, true); hideMessage(remindersInfoEl, true); }

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
                showRemindersError("Session expired or unauthorized. Redirecting to login...");
                setTimeout(() => window.location.href = '/login.html', 2500);
                throw new Error("Unauthorized");
            }
            const responseText = await response.text();
            if (!response.ok) {
                let errorDetail = `Server error: ${response.status} for ${url.split('/').pop()}`;
                if (responseText) {
                    try { const errorData = JSON.parse(responseText); errorDetail = errorData.error || errorData.message || errorDetail;
                    } catch (parseError) { errorDetail = responseText.substring(0, 150) || errorDetail; }
                }
                throw new Error(errorDetail);
            }
            if (response.status === 204 || !responseText) return null;
            return JSON.parse(responseText);
        } catch (error) {
            if (error.message !== "Unauthorized" && !remindersErrorEl.classList.contains('visible')) {
                showRemindersError(error.message || "A network or server error occurred.");
            }
            throw error;
        }
    }

    // --- Reminder Item Creation ---
    function createReminderItemElement(reminder) { // reminder is an EmailMetadata object
        const item = document.createElement('li');
        item.className = 'reminder-item';
        // Add status class based on reminderDateTime for styling (e.g., overdue, upcoming)
        const now = new Date();
        const reminderDate = reminder.reminderDateTime ? new Date(reminder.reminderDateTime) : null;
        if (reminder.notified) { // Assuming 'notified' field from EmailMetadata
            item.classList.add('status-completed'); // Or 'status-notified'
        } else if (reminderDate && reminderDate < now) {
            item.classList.add('status-overdue');
        } else if (reminderDate) {
            item.classList.add('status-upcoming');
        } else {
            item.classList.add('status-pending'); // If no date, or for other states
        }

        item.dataset.metadataId = reminder.id; // Store EmailMetadata ID
        item.dataset.gmailId = reminder.gmailMessageId;
        item.reminderData = { ...reminder }; // Store full data

        let displayReminderTime = "Not set";
        if (reminder.reminderDateTime) {
            try {
                displayReminderTime = new Date(reminder.reminderDateTime).toLocaleString(undefined, {
                    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit'
                });
            } catch (e) { console.warn("Error formatting reminder date for display:", e); }
        }

        item.innerHTML = `
            <div class="reminder-item-main">
                <h3 class="reminder-subject">${reminder.subject || '(No Subject)'}</h3>
                <p class="reminder-email-info">
                    From: <span class="sender">${reminder.sender || 'N/A'}</span>
                    (Email ID: ${reminder.gmailMessageId.substring(0,10)}...)
                </p>
                <p class="reminder-datetime">
                    <i class="fas fa-clock"></i> Due: ${displayReminderTime}
                    ${reminder.priority && reminder.priority !== 'none' ? `| Priority: <span class="priority-text-${reminder.priority}">${reminder.priority.charAt(0).toUpperCase() + reminder.priority.slice(1)}</span>` : ''}
                </p>
                <p class="reminder-notes-preview">Notes: ${reminder.notes || '<em>No notes added.</em>'}</p>
            </div>
            <div class="reminder-item-actions">
                <button class="btn btn-sm btn-edit-reminder" title="Edit Reminder"><i class="fas fa-edit"></i> Edit</button>
                <button class="btn btn-sm btn-delete-reminder" title="Delete Reminder"><i class="fas fa-trash-alt"></i> Delete</button>
                ${!reminder.notified && reminderDate && reminderDate < now ? '<button class="btn btn-sm btn-mark-complete" title="Mark as Done/Notified"><i class="fas fa-check-square"></i> Done</button>' : ''}
            </div>
        `;

        attachReminderActionListeners(item);
        return item;
    }

    function attachReminderActionListeners(reminderItemElement) {
        const reminderData = reminderItemElement.reminderData;
        const metadataId = reminderData.id; // This is EmailMetadata.id

        const editButton = reminderItemElement.querySelector('.btn-edit-reminder');
        if (editButton) {
            editButton.addEventListener('click', () => {
                clearAllMessages();
                openEditReminderModal(reminderData);
            });
        }

        const deleteButton = reminderItemElement.querySelector('.btn-delete-reminder');
        if (deleteButton) {
            deleteButton.addEventListener('click', async () => {
                clearAllMessages();
                if (confirm(`Are you sure you want to delete the reminder for "${reminderData.subject || 'this email'}"?`)) {
                    try {
                        await apiFetch(`${API_BASE_URL}/reminders/${metadataId}`, { method: 'DELETE' });
                        showRemindersInfo("Reminder deleted successfully.");
                        reminderItemElement.remove(); // Remove from UI
                        checkIfRemindersListEmpty();
                    } catch (error) {
                        // Error already shown by apiFetch
                    }
                }
            });
        }
        
        const markCompleteButton = reminderItemElement.querySelector('.btn-mark-complete');
        if(markCompleteButton){
            markCompleteButton.addEventListener('click', async () => {
                clearAllMessages();
                 try {
                    // Assume backend endpoint to mark as notified/completed
                    await apiFetch(`${API_BASE_URL}/reminders/${metadataId}/complete`, { method: 'POST' });
                    showRemindersInfo("Reminder marked as completed.");
                    reminderItemElement.classList.remove('status-overdue', 'status-upcoming', 'status-pending');
                    reminderItemElement.classList.add('status-completed');
                    markCompleteButton.remove(); // Remove the button once completed
                } catch (error) {
                    // Error already shown by apiFetch
                }
            });
        }
    }

    function openEditReminderModal(reminderData) {
        if (!schedulerModal || !schedulerForm || !schedulerEmailSubjectEl || !reminderDateTimeInput || !reminderNotesInput || !reminderPriorityModalInput || !schedulerModalTitle) {
            showRemindersError("Edit Reminder modal UI elements missing."); return;
        }
        currentEditingReminderData = reminderData; // Store the full EmailMetadata object
        schedulerModalTitle.innerHTML = '<i class="fas fa-edit"></i> Edit Reminder';
        schedulerEmailSubjectEl.textContent = reminderData.subject || '(No Subject)';
        
        let initialDateTimeValue = "";
        if (reminderData.reminderDateTime) {
            try {
                const dateObj = new Date(reminderData.reminderDateTime);
                if (!isNaN(dateObj.valueOf())) {
                     initialDateTimeValue = `${dateObj.getFullYear()}-${(dateObj.getMonth() + 1).toString().padStart(2, '0')}-${dateObj.getDate().toString().padStart(2, '0')}T${dateObj.getHours().toString().padStart(2, '0')}:${dateObj.getMinutes().toString().padStart(2, '0')}`;
                }
            } catch(e) { console.warn("Error parsing reminderDateTime for modal:", e); }
        }
        reminderDateTimeInput.value = initialDateTimeValue;
        reminderNotesInput.value = reminderData.notes || "";
        reminderPriorityModalInput.value = reminderData.priority || "none";
        
        schedulerModal.style.display = 'flex';
        requestAnimationFrame(() => { schedulerModal.classList.add('active'); });
    }

    // The closeSchedulerModal and schedulerForm submit listener needs to be adapted
    // if you are reusing the SAME modal for editing.
    // For now, let's assume closeSchedulerModal is generic.
    // The submit listener will need to know if it's editing or creating.
    // We can use currentEditingReminderData to check this.

    function closeSchedulerModal() { // Generic close
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
        currentEditingReminderData = null; // Clear editing context
    }

    if(closeSchedulerModalBtn) closeSchedulerModalBtn.addEventListener('click', closeSchedulerModal);
    if(cancelSchedulerModalBtn) cancelSchedulerModalBtn.addEventListener('click', closeSchedulerModal);
    if(schedulerModal) schedulerModal.addEventListener('click', (e) => { if (e.target === schedulerModal) closeSchedulerModal(); });

    if (schedulerForm) { // This form is now for EDITING existing reminders from this page
        schedulerForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            if (!currentEditingReminderData) {
                showRemindersError("No reminder selected for editing."); return;
            }
            clearAllMessages();

            const reminderDateTimeValue = reminderDateTimeInput.value;
            const notes = reminderNotesInput.value;
            const priority = reminderPriorityModalInput.value;
            const metadataId = currentEditingReminderData.id; // Use EmailMetadata ID
            const { gmailMessageId, subject, sender } = currentEditingReminderData;


            if (!reminderDateTimeValue) { showRemindersError("Please select a valid date and time."); return; }
            const reminderDateTimeISO = reminderDateTimeValue.length === 16 ? reminderDateTimeValue + ":00" : reminderDateTimeValue;

            try {
                // Use the combined endpoint for updates as well, or create a dedicated PUT endpoint
                const result = await apiFetch(`${API_BASE_URL}/emails/${gmailMessageId}/set-schedule-priority`, { // Or a dedicated PUT /api/reminders/{metadataId}
                    method: 'POST', // Should ideally be PUT if updating an existing resource by its ID
                    body: {
                        reminderDateTime: reminderDateTimeISO,
                        priority: priority,
                        notes: notes,
                        subject: subject, // Send original subject
                        sender: sender   // Send original sender
                    }
                });
                if (result) {
                    showRemindersInfo(result.message || 'Reminder updated successfully!');
                    loadScheduledReminders(); // Reload the list to show changes
                }
            } catch (error) { /* apiFetch already showed error */ }
            closeSchedulerModal();
        });
    }

    async function loadScheduledReminders() {
        clearAllMessages();
        if (!remindersListUL || !noRemindersMessage) {
            if(document.body) showRemindersError('UI Error: Reminders list container missing.');
            return;
        }
        remindersListUL.innerHTML = '<li class="loading-message"><i class="fas fa-spinner fa-spin"></i> Loading your reminders...</li>';
        noRemindersMessage.style.display = 'none';

        try {
            const reminders = await apiFetch(`${API_BASE_URL}/reminders/upcoming`); // Assuming this returns List<EmailMetadata>
            remindersListUL.innerHTML = '';
            if (reminders && reminders.length > 0) {
                reminders.forEach(reminder => {
                    const reminderItemElement = createReminderItemElement(reminder);
                    remindersListUL.appendChild(reminderItemElement);
                });
            } else if (reminders) { // Empty array
                remindersListUL.innerHTML = ''; // Clear loading
                noRemindersMessage.style.display = 'block';
            } else {
                 if (!remindersErrorEl.classList.contains('visible') && !remindersInfoEl.classList.contains('visible')) {
                    showRemindersInfo("No reminders found or an issue occurred.");
                 }
                 if (remindersListUL.querySelector('.loading-message')) {
                    remindersListUL.innerHTML = '';
                    noRemindersMessage.style.display = 'block';
                 }
            }
        } catch (error) {
            if (remindersListUL.querySelector('.loading-message')) {
                 remindersListUL.innerHTML = `<li class="error-message-li"><i class="fas fa-exclamation-triangle"></i> Failed to load reminders: ${error.message}</li>`;
            }
             noRemindersMessage.style.display = 'none';
        }
    }
    
    function checkIfRemindersListEmpty() {
        if (remindersListUL && noRemindersMessage) {
            noRemindersMessage.style.display = remindersListUL.children.length === 0 ? 'block' : 'none';
        }
    }


    if (logoutButton) {
        logoutButton.addEventListener('click', async function (event) {
            event.preventDefault(); clearAllMessages();
            try {
                await apiFetch('/logout', { method: 'POST' });
                showRemindersInfo("Logging you out...");
                setTimeout(() => { window.location.href = '/login.html?logout'; }, 1500);
            } catch (error) {
                if (error.message !== "Unauthorized") showRemindersError('Logout failed.');
                setTimeout(() => { window.location.href = '/login.html?logoutFailed'; }, 2000);
            }
        });
    } else { console.warn('Logout button for reminders page not found.'); }

    // Initial Load
    loadScheduledReminders();
});
