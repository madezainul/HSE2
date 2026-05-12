/**
 * Incident Modal Submit Handler
 * Handles form submission + file uploads after incident is created
 */

const IncidentFormModule = {

    init: function () {
        this.log('[IncidentFormModule] Script loaded, initializing...');
        const form = document.getElementById('addIncidentForm');
        const submitBtn = document.getElementById('submitIncidentBtn');
        
        if (!form) {
            console.warn('[IncidentFormModule] Form addIncidentForm not found - will retry in 500ms');
            setTimeout(() => this.init(), 500);
            return;
        }

        if (!submitBtn) {
            console.warn('[IncidentFormModule] Submit button not found!');
        }

        const self = this;
        
        form.addEventListener('submit', function(e) {
            self.log('[IncidentFormModule] Form submit event triggered');
            e.preventDefault();
            e.stopPropagation();
            self.handleSubmit();
        });
        
        if (submitBtn) {
            submitBtn.addEventListener('click', function(e) {
                self.log('[IncidentFormModule] Submit button clicked directly');
                e.preventDefault();
                e.stopPropagation();
                const event = new Event('submit', { bubbles: true, cancelable: true });
                form.dispatchEvent(event);
            });
        }

        this.log('[IncidentFormModule] Initialized successfully - ready to submit');
    },

    log: function(message) {
        console.log(message);
        let logs = sessionStorage.getItem('incidentFormLogs') || '';
        logs += message + '\n';
        sessionStorage.setItem('incidentFormLogs', logs);
    },

    getCsrfTokenFromCookie: function() {
        const name = 'XSRF-TOKEN';
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    },

    handleSubmit: async function () {
        this.log('[IncidentFormModule] ========== FORM SUBMISSION STARTED ==========');
        const submitBtn = document.getElementById('submitIncidentBtn');

        try {
            this.log('[IncidentFormModule] Step 0: Validating form data');
            const validationError = this.validateFormData();
            if (validationError) {
                this.log('[IncidentFormModule] ❌ Validation FAILED');
                this.showAlert('warning', validationError);
                return;
            }
            this.log('[IncidentFormModule] ✓ Validation passed');

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Saving...';

            const incidentData = this.collectFormData();
            this.log('[IncidentFormModule] Step 1: Collected form data');

            this.log('[IncidentFormModule] Sending POST request to /incident...');
            
            // Get CSRF token from meta tag or cookie
            const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || 
                         this.getCsrfTokenFromCookie();
            const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
            
            const headers = { 'Content-Type': 'application/json' };
            if (token) {
                headers[header] = token;
                this.log('[IncidentFormModule] CSRF token added to request');
            }
            
            const createResponse = await fetch('/incident/api', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(incidentData)
            });

            this.log('[IncidentFormModule] ✓ POST response received: ' + createResponse.status + ' ' + createResponse.statusText);
            
            let createResult;
            try {
                const responseText = await createResponse.text();
                this.log('[IncidentFormModule] Raw response text: ' + responseText.substring(0, 100));
                createResult = JSON.parse(responseText);
                this.log('[IncidentFormModule] Parsed response data successfully');
            } catch (parseError) {
                this.log('[IncidentFormModule] ❌ Error parsing response: ' + parseError.message);
                throw new Error('Invalid server response: ' + parseError.message);
            }

            if (!createResponse.ok) {
                this.log('[IncidentFormModule] ❌ HTTP error: ' + createResponse.status);
                throw new Error(createResult?.message || 'HTTP ' + createResponse.status + ': Failed to create incident');
            }

            if (!createResult?.success) {
                this.log('[IncidentFormModule] ❌ Success flag is false');
                throw new Error(createResult?.message || 'Failed to create incident');
            }

            const incidentCode = createResult.incidentCode;
            this.log('[IncidentFormModule] ✓ Incident created with code: ' + incidentCode);

            const imageFiles = document.getElementById('incidentImages').files;
            if (imageFiles.length > 0) {
                submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Uploading images...';
                await this.uploadFiles(incidentCode, imageFiles, 'images');
            }

            const documentFiles = document.getElementById('incidentDocuments').files;
            if (documentFiles.length > 0) {
                submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Uploading documents...';
                await this.uploadFiles(incidentCode, documentFiles, 'attachments');
            }

            this.log('[IncidentFormModule] ✓ Incident and files saved successfully');
            this.showAlert('success', '<strong>✓ Incident ' + incidentCode + ' reported successfully!</strong><br><small>Check browser console (F12) for detailed logs</small><br><button class="btn btn-sm btn-primary mt-2" onclick="location.reload()" style="margin-top: 10px;">Reload Page</button>');
            this.resetForm();
            this.log('[IncidentFormModule] Waiting for user to reload page...');

        } catch (error) {
            this.log('[IncidentFormModule] ❌ ERROR: ' + error.message);
            this.showAlert('danger', error.message || 'An error occurred. Please try again.');
        } finally {
            this.log('[IncidentFormModule] handleSubmit completed, re-enabling button');
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fa fa-save me-2"></i> Report Incident';
        }
    },

    validateFormData: function () {
        this.log('[IncidentFormModule] validateFormData called');
        
        const incidentDate = document.getElementById('incidentDate').value;
        this.log('[IncidentFormModule] Incident Date: ' + (incidentDate ? 'OK' : 'EMPTY'));
        if (!incidentDate || incidentDate.trim() === '') {
            return '<i class="fa fa-exclamation-circle me-2"></i> Incident date & time is required';
        }

        const areaCode = document.getElementById('areaHidden').value;
        this.log('[IncidentFormModule] Area Code: ' + (areaCode ? 'OK' : 'EMPTY'));
        if (!areaCode || areaCode.trim() === '') {
            return '<i class="fa fa-exclamation-circle me-2"></i> Area is required';
        }

        const severity = document.getElementById('severity').value;
        this.log('[IncidentFormModule] Severity: ' + (severity ? 'OK' : 'EMPTY'));
        if (!severity || severity.trim() === '') {
            return '<i class="fa fa-exclamation-circle me-2"></i> Severity level is required';
        }

        const type = document.getElementById('type').value;
        this.log('[IncidentFormModule] Type: ' + (type ? 'OK' : 'EMPTY'));
        if (!type || type.trim() === '') {
            return '<i class="fa fa-exclamation-circle me-2"></i> Incident type is required';
        }

        const description = document.getElementById('description').value;
        this.log('[IncidentFormModule] Description: ' + (description ? 'OK' : 'EMPTY'));
        if (!description || description.trim() === '') {
            return '<i class="fa fa-exclamation-circle me-2"></i> Description is required';
        }

        this.log('[IncidentFormModule] ✓ All validations passed');
        return null;
    },

    collectFormData: function () {
        return {
            reportDate: document.getElementById('incidentDate').value,
            area: {
                code: document.getElementById('areaHidden').value,
                name: document.getElementById('areaInput').value
            },
            severity: document.getElementById('severity').value,
            type: document.getElementById('type').value,
            description: document.getElementById('description').value,
            involvedPersonName: document.getElementById('involvedPersonName').value,
            involvedPersonEmployeeId: document.getElementById('involvedPersonHidden').value,
            involvedPersonPosition: document.getElementById('involvedPersonPosition').value,
            witnesses: document.getElementById('witnessHidden').value,
            status: document.getElementById('status').value,
            immediateAction: document.getElementById('immediateAction').value,
            correctiveAction: document.getElementById('correctiveAction').value,
            medicalAttentionRequired: document.getElementById('medicalAttentionRequired').checked
        };
    },

    uploadFiles: async function (incidentCode, files, type) {
        const uploadPromises = Array.from(files).map(file => {
            const formData = new FormData();
            formData.append('file', file);

            return fetch('/incident/' + incidentCode + '/' + type, {
                method: 'POST',
                body: formData
            }).then(response => {
                if (!response.ok) {
                    throw new Error('Failed to upload ' + file.name);
                }
                return response.json();
            }).then(result => {
                this.log('[IncidentFormModule] Uploaded ' + file.name);
            });
        });

        await Promise.all(uploadPromises);
        this.log('[IncidentFormModule] All ' + type + ' uploaded successfully');
    },

    showAlert: function (type, message) {
        this.log('[IncidentFormModule] Showing alert: ' + type);
        
        const existing = document.getElementById('incidentFormAlert');
        if (existing) existing.remove();

        const alert = document.createElement('div');
        alert.id = 'incidentFormAlert';
        alert.className = 'alert alert-' + type + ' alert-dismissible fade show mt-2';
        alert.role = 'alert';
        alert.innerHTML = '<div>' + message + '</div><button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>';

        const modalBody = document.querySelector('#addIncidentModal .modal-body');
        if (modalBody) {
            modalBody.prepend(alert);
            this.log('[IncidentFormModule] Alert added to modal body');
        } else {
            this.log('[IncidentFormModule] WARNING: Modal body not found for alert');
        }
    },

    resetForm: function () {
        this.log('[IncidentFormModule] Resetting form');
        const form = document.getElementById('addIncidentForm');
        if (form) form.reset();

        document.getElementById('areaHidden').value = '';
        document.getElementById('involvedPersonHidden').value = '';
        document.getElementById('witnessHidden').value = '';

        const alert = document.getElementById('incidentFormAlert');
        if (alert) alert.remove();
    }
};

console.log('[IncidentFormModule] Script loaded, document state: ' + document.readyState);

// Expose module globally
window.IncidentFormModule = IncidentFormModule;

if (document.readyState === 'loading') {
    console.log('[IncidentFormModule] DOM still loading, waiting for DOMContentLoaded');
    document.addEventListener('DOMContentLoaded', () => {
        console.log('[IncidentFormModule] DOMContentLoaded fired');
        IncidentFormModule.init();
    });
} else {
    console.log('[IncidentFormModule] DOM already loaded, initializing immediately');
    IncidentFormModule.init();
}
