

const IncidentForm = {
    isUpdateMode: false,
    IncidentCode: null,

    init: function () {
        const form = document.getElementById('addIncidentForm');
        if (!form) return;

        form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleSubmit();
        })
    },

    setUpdateMode: function (incidentCode, incidentData) {
        this.isUpdateMode = true;
        this.IncidentCode = incidentCode;

        // Store IncidentCode in hidden field for code
        const codeField = document.getElementById('incidentCodeHidden');
        if (codeField) codeField.value = incidentCode;

        // Populate form field for date
        const dateInput = document.getElementById('incidentDate');
        if (dateInput && incidentData.incidentDate) {
            const date = new Date(incidentData.incidentDate);
            dateInput.value = date.getFullYear() + '-' +
                String(date.getMonth() + 1).padStart(2, '0') + '-' +
                String(date.getDate()).padStart(2, '0') + 'T' +
                String(date.getHours()).padStart(2, '0') + ':' +
                String(date.getMinutes()).padStart(2, '0');
        }

        // Handle Area
        console.log('Area data:', incidentData.area);
        const areaHidden = document.getElementById('areaHidden');
        const areaInput = document.getElementById('areaInput');
        if (incidentData.area) {
            if (areaHidden) areaHidden.value = incidentData.area.code;
            if (areaInput) areaInput.value = incidentData.area.name || incidentData.area.code || '';
        } else {
            console.warn('Area data is missing from incident');
            if (areaHidden) areaHidden.value = '';
            if (areaInput) areaInput.value = '';
        }

        // Populate severity dropdown
        const severityField = document.getElementById('severity');
        if (severityField && incidentData.severity) {
            severityField.value = incidentData.severity;
        }

        // Populate type dropdown
        const typeField = document.getElementById('type');
        if (typeField && incidentData.type) {
            typeField.value = incidentData.type;
        }

        // Populate description
        const descField = document.getElementById('description');
        if (descField && incidentData.description) {
            descField.value = incidentData.description;
        }

        // Populate involved person name
        const involvedPersonName = document.getElementById('involvedPersonName');
        if (involvedPersonName && incidentData.involvedPersonName) {
            involvedPersonName.value = incidentData.involvedPersonName;
        }

        // Populate involved person ID (hidden)
        const involvedPersonId = document.getElementById('involvedPersonHidden');
        if (involvedPersonId && incidentData.involvedPersonEmployeeId) {
            involvedPersonId.value = incidentData.involvedPersonEmployeeId;
        }

        // Populate involved person position
        const involvedPersonPosition = document.getElementById('involvedPersonPosition');
        if (involvedPersonPosition && incidentData.involvedPersonPosition) {
            involvedPersonPosition.value = incidentData.involvedPersonPosition;
        }

        // Populate witnesses
        const witnessesField = document.getElementById('witnesses');
        if (witnessesField && incidentData.witnesses) {
            witnessesField.value = incidentData.witnesses;
        }

        // Populate witnesses ID (hidden)
        const witnessesId = document.getElementById('witnessesHidden');
        if (witnessesId && incidentData.witnessesEmployeeId) {
            witnessesId.value = incidentData.witnessesEmployeeId;
        }

        // Populate status dropdown
        const statusField = document.getElementById('status');
        if (statusField && incidentData.status) {
            statusField.value = incidentData.status;
        }

        // Populate immediate action
        const immediateAction = document.getElementById('immediateAction');
        if (immediateAction && incidentData.immediateAction) {
            immediateAction.value = incidentData.immediateAction;
        }

        // Populate corrective action
        const correctiveAction = document.getElementById('correctiveAction');
        if (correctiveAction && incidentData.correctiveAction) {
            correctiveAction.value = incidentData.correctiveAction;
        }

        // Populate medical attention checkbox
        const medicalAttentionRequired = document.getElementById('medicalAttentionRequired');
        if (medicalAttentionRequired && incidentData.medicalAttentionRequired) {
            medicalAttentionRequired.checked = incidentData.medicalAttentionRequired;
        }

        // Update modal title and button text
        const modalTitle = document.getElementById('addIncidentModalLabel');
        if (modalTitle) modalTitle.textContent = 'Edit Incident Report';

        const submitBtn = document.getElementById('submitIncidentBtn');
        if (submitBtn) submitBtn.innerHTML = '<i class="fa fa-check me-2"></i> Update Incident';

        console.log('Update mode set successfully');
    },

    resetUpdateMode: function () {
        this.isUpdateMode = false;
        this.IncidentCode = null;

        const modalTitle = document.getElementById('addIncidentModalLabel');
        if (modalTitle) modalTitle.textContent = 'Report New Incident';

        const submitBtn = document.getElementById('submitIncidentBtn');
        if (submitBtn) submitBtn.innerHTML = '<i class="fa fa-save me-2"></i> Report Incident';
    },

    handleSubmit: async function () {
        const submitBtn = document.getElementById('submitIncidentBtn');

        try {
            const validationError = this.validateFormData();
            if (validationError) {
                this.showAlert('warning', validationError);
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Saving...';

            // Collect form data as FormData (multipart/form-data)
            const formData = this.collectFormDataAsMultipart();

            // Optional: Add CSRF token if available
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

            const headers = {};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            // Determine endpoint and method based on mode
            const endpoint = this.isUpdateMode ? `/api/incident/${this.IncidentCode}` : '/api/incident';
            const method = this.isUpdateMode ? 'PUT' : 'POST';
            const expectedStatus = this.isUpdateMode ? 200 : 201;

            const response = await fetch(endpoint, {
                method: method,
                headers: headers,
                body: formData
            });

            const result = await response.json();

            if (response.ok) {
                const successMsg = this.isUpdateMode ? 'Incident updated successfully!' : 'Incident and files saved successfully!';
                this.showAlert('success', '<strong>' + successMsg + '</strong>');
                this.resetForm();
                this.resetUpdateMode();
                const redirectUrl = this.isUpdateMode ? `/incident/${this.IncidentCode}` : '/incident';
                setTimeout(() => { window.location.href = redirectUrl; }, 1500);
            } else {
                const errorMsg = result?.message || 'Unknown error';
                this.showAlert('danger', '<strong>Error:</strong> ' + errorMsg);
            }
        } catch (error) {
            this.showAlert('danger', '<strong>Error:</strong> ' + error.message);
        } finally {
            submitBtn.disabled = false;
            if (this.isUpdateMode) {
                submitBtn.innerHTML = '<i class="fa fa-check me-2"></i> Update Incident';
            } else {
                submitBtn.innerHTML = '<i class="fa fa-save me-2"></i> Report Incident';
            }
        }
    },

    collectFormDataAsMultipart: function () {
        const formData = new FormData();

        // Add incident fields
        formData.append('reportDate', document.getElementById('incidentDate').value);
        formData.append('areaCode', document.getElementById('areaHidden').value);
        formData.append('severity', document.getElementById('severity').value);
        formData.append('type', document.getElementById('type').value);
        formData.append('description', document.getElementById('description').value);

        const involvedPersonName = document.getElementById('involvedPersonName').value;
        if (involvedPersonName) formData.append('involvedPersonName', involvedPersonName);

        const involvedPersonId = document.getElementById('involvedPersonHidden').value;
        if (involvedPersonId) formData.append('involvedPersonEmployeeId', involvedPersonId);

        const involvedPersonPosition = document.getElementById('involvedPersonPosition').value;
        if (involvedPersonPosition) formData.append('involvedPersonPosition', involvedPersonPosition);

        const witnesses = document.getElementById('witnesses').value;
        if (witnesses) formData.append('witnesses', witnesses);

        const witnessesId = document.getElementById('witnessesHidden').value;
        if (witnessesId) formData.append('witnessesEmployeeId', witnessesId);

        const status = document.getElementById('status').value;
        if (status) formData.append('status', status);

        const immediateAction = document.getElementById('immediateAction').value;
        if (immediateAction) formData.append('immediateAction', immediateAction);

        const correctiveAction = document.getElementById('correctiveAction').value;
        if (correctiveAction) formData.append('correctiveAction', correctiveAction);

        const medicalAttention = document.getElementById('medicalAttentionRequired').checked;
        formData.append('medicalAttentionRequired', medicalAttention);

        // Add image files
        const imageFiles = document.getElementById('incidentImages').files;
        for (let i = 0; i < imageFiles.length; i++) {
            formData.append('incidentImages', imageFiles[i]);
        }

        // Add document files
        const documentFiles = document.getElementById('incidentDocuments').files;
        for (let i = 0; i < documentFiles.length; i++) {
            formData.append('incidentDocuments', documentFiles[i]);
        }

        return formData;
    },

    validateFormData: function () {
        if (!document.getElementById('incidentDate').value) {
            return 'Incident date is required';
        }
        if (!document.getElementById('areaHidden').value) {
            return 'Area is required';
        }
        if (!document.getElementById('severity').value) {
            return 'Severity level is required';
        }
        if (!document.getElementById('type').value) {
            return 'Incident type is required';
        }
        if (!document.getElementById('description').value) {
            return 'Description is required';
        }
        return null;
    },

    showAlert: function (type, message) {
        const existing = document.getElementById('incidentFormAlert');
        if (existing) existing.remove();

        const alert = document.createElement('div');
        alert.id = 'incidentFormAlert';
        alert.className = 'alert alert-' + type + ' alert-dismissible fade show mt-2';
        alert.innerHTML = message + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';

        const modalBody = document.querySelector('#addIncidentModal .modal-body');
        if (modalBody) modalBody.prepend(alert);
    },

    resetForm: function () {
        document.getElementById('addIncidentForm').reset();
        document.getElementById('areaHidden').value = '';
        document.getElementById('involvedPersonHidden').value = '';
        document.getElementById('witnessesHidden').value = '';
        const alert = document.getElementById('incidentFormAlert');
        if (alert) alert.remove();
    }
};

document.addEventListener('DOMContentLoaded', () => IncidentForm.init());