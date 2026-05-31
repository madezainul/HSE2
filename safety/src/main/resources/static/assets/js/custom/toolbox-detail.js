// Toolbox Meeting Detail Page

/**
 * Open a fullscreen modal to display a zoomed image
 * @param {HTMLImageElement} img - The image element to zoom
 */
window.openZoomModal = function (img) {
    const src = img.dataset.src || img.src;
    if (!src || (!src.startsWith('data:') && !src.includes('/file/'))) return;
    document.getElementById('zoomedImage').src = src;
    new bootstrap.Modal(document.getElementById('imageZoomModal')).show();
};

/**
 * Delete a toolbox meeting with confirmation
 * @param {Event} event - The click event
 */
function deleteToolboxMeeting(event) {
    event.preventDefault();

    const button = event.target.closest('button');
    const meetingCode = button.getAttribute('data-toolbox-code');

    if (!meetingCode) {
        alert('Error: Toolbox meeting code not found');
        return;
    }

    if (!confirm('Are you sure you want to delete this toolbox meeting? This action cannot be undone.')) {
        return;
    }

    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers    = { 'Content-Type': 'application/json' };
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

    fetch(`/api/toolbox/${meetingCode}`, {
        method: 'DELETE',
        headers: headers
    })
    .then(response => {
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return response.json();
    })
    .then(data => {
        if (data.status === 'success') {
            alert('Toolbox meeting deleted successfully');
            window.location = '/procedure/toolbox';
        } else {
            alert('Error: ' + (data.message || 'Failed to delete toolbox meeting'));
        }
    })
    .catch(error => {
        console.error('Delete error:', error);
        alert('Error deleting toolbox meeting: ' + error.message);
    });
}

/**
 * Wire up the Edit button after DOM load — fetches data from API and opens the modal in update mode.
 */
document.addEventListener('DOMContentLoaded', function () {

    const editBtn = document.getElementById('editToolboxBtn');
    if (!editBtn) return;

    editBtn.addEventListener('click', function (e) {
        e.preventDefault();
        const meetingCode = this.getAttribute('data-toolbox-code');
        if (!meetingCode) {
            alert('Error: Toolbox meeting code not found');
            return;
        }

        fetch(`/api/toolbox/${meetingCode}`)
            .then(response => {
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                return response.json();
            })
            .then(data => {
                // Populate modal fields
                document.getElementById('toolboxCodeHidden').value  = data.code || '';
                document.getElementById('toolboxLocation').value    = data.location || '';
                document.getElementById('toolboxDate').value        = data.meetingDate || '';
                document.getElementById('toolboxTimeStarted').value = data.timeStarted || '';
                document.getElementById('toolboxTimeEnd').value     = data.timeEnd || '';
                document.getElementById('toolboxNotes').value       = data.notes || '';

                // Supervisor
                const supervisorInput  = document.getElementById('toolboxSupervisorName');
                const supervisorHidden = document.getElementById('toolboxSupervisorHidden');
                if (supervisorInput)  supervisorInput.value  = data.supervisor || '';
                if (supervisorHidden) supervisorHidden.value = data.supervisorEmployeeId || '';

                // Status
                const statusSelect = document.getElementById('toolboxStatus');
                if (statusSelect && data.status) statusSelect.value = data.status;

                // Update modal title to indicate Edit mode
                const modalTitle = document.getElementById('addToolboxModalLabel');
                if (modalTitle) modalTitle.textContent = 'Edit Toolbox Meeting';

                // Change submit button to PUT
                const form = document.getElementById('addToolboxForm');
                if (form) form.dataset.editCode = data.code;

                // Open the modal
                const modalEl = document.getElementById('addToolboxModal');
                if (!modalEl) {
                    alert('Error: Modal not found on page');
                    return;
                }
                new bootstrap.Modal(modalEl).show();
            })
            .catch(error => {
                console.error('Error fetching toolbox meeting:', error);
                alert('Error fetching toolbox meeting data: ' + error.message);
            });
    });

    // Override form submit to use PUT when in edit mode
    const form = document.getElementById('addToolboxForm');
    if (!form) return;

    form.addEventListener('submit', function handleDetailEdit(e) {
        const editCode = form.dataset.editCode;
        if (!editCode) return; // let toolbox-modal.js handle create

        e.preventDefault();
        e.stopImmediatePropagation();

        const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const headers    = {};
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

        const spinner  = document.getElementById('submitToolboxSpinner');
        const btn      = document.getElementById('submitToolboxBtn');
        const errorDiv = document.getElementById('toolboxFormError');

        if (spinner) spinner.classList.remove('d-none');
        if (btn)     btn.disabled = true;

        fetch(`/api/toolbox/${editCode}`, {
            method: 'PUT',
            headers: headers,
            body: new FormData(form)
        })
        .then(res => res.json())
        .then(data => {
            if (spinner) spinner.classList.add('d-none');
            if (btn)     btn.disabled = false;
            if (data.status !== 'success') {
                if (errorDiv) { errorDiv.textContent = data.message || 'Update failed.'; errorDiv.classList.remove('d-none'); }
                return;
            }
            bootstrap.Modal.getInstance(document.getElementById('addToolboxModal'))?.hide();
            window.location.reload();
        })
        .catch(err => {
            if (spinner) spinner.classList.add('d-none');
            if (btn)     btn.disabled = false;
            if (errorDiv) { errorDiv.textContent = 'Request failed. Please try again.'; errorDiv.classList.remove('d-none'); }
            console.error('Toolbox update error:', err);
        });
    }, true); // capture phase so it runs before toolbox-modal.js listener
});
