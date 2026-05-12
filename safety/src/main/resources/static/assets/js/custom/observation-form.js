/**
 * Observation Form Handler
 * Handles form submission with file uploads in a single FormData request
 * Supports both CREATE (POST) and UPDATE (PUT) modes
 */

const ObservationForm = {
    isUpdateMode: false,
    observationCode: null,
    
    init: function() {
        const form = document.getElementById('addObservationForm');
        if (!form) return;
        
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleSubmit();
        });
    },

    // Set form to UPDATE mode with existing observation data
    setUpdateMode: function(observationCode, observationData) {
        this.isUpdateMode = true;
        this.observationCode = observationCode;
        
        console.log('Setting update mode with data:', observationData);
        
        // Store observation code in hidden field
        const codeField = document.getElementById('observationCodeHidden');
        if (codeField) codeField.value = observationCode;
        
        // Populate form fields with null checks
        const dateInput = document.getElementById('observationDate');
        if (dateInput && observationData.observationDate) {
            // Convert ISO format to datetime-local format (yyyy-MM-dd'T'HH:mm)
            const date = new Date(observationData.observationDate);
            dateInput.value = date.getFullYear() + '-' + 
                            String(date.getMonth() + 1).padStart(2, '0') + '-' + 
                            String(date.getDate()).padStart(2, '0') + 'T' +
                            String(date.getHours()).padStart(2, '0') + ':' + 
                            String(date.getMinutes()).padStart(2, '0');
        }
        
        // Handle area
        console.log('Area data:', observationData.area);
        const areaHidden = document.getElementById('areaHidden');
        const areaInput = document.getElementById('areaInput');
        if (observationData.area) {
            if (areaHidden) areaHidden.value = observationData.area.code;
            if (areaInput) areaInput.value = observationData.area.name || observationData.area.code || '';
        } else {
            console.warn('Area data is missing from observation');
            if (areaHidden) areaHidden.value = '';
            if (areaInput) areaInput.value = '';
        }
        
        const categoryField = document.getElementById('category');
        if (categoryField && observationData.category) {
            categoryField.value = observationData.category;
        }
        
        const typeField = document.getElementById('type');
        if (typeField && observationData.type) {
            typeField.value = observationData.type;
        }
        
        const descField = document.getElementById('description');
        if (descField && observationData.description) {
            descField.value = observationData.description;
        }
        
        const statusField = document.getElementById('status');
        if (statusField && observationData.status) {
            statusField.value = observationData.status;
        }
        
        const mitigField = document.getElementById('mitigation');
        if (mitigField && observationData.mitigation) {
            mitigField.value = observationData.mitigation;
        }
        
        const observerName = document.getElementById('observerName');
        if (observerName && observationData.observerName) {
            observerName.value = observationData.observerName;
        }
        
        const observerId = document.getElementById('observerHidden');
        if (observerId && observationData.observerId) {
            observerId.value = observationData.observerId;
        }
        
        const responsibleName = document.getElementById('responsibleName');
        if (responsibleName && observationData.responsibleName) {
            responsibleName.value = observationData.responsibleName;
        }
        
        const responsibleId = document.getElementById('responsibleHidden');
        if (responsibleId && observationData.responsibleId) {
            responsibleId.value = observationData.responsibleId;
        }
        
        const corrAction = document.getElementById('correctiveAction');
        if (corrAction && observationData.correctiveAction) {
            corrAction.value = observationData.correctiveAction;
        }
        
        const remarksField = document.getElementById('remarks');
        if (remarksField && observationData.remarks) {
            remarksField.value = observationData.remarks;
        }
        
        // Update modal title and button text
        const modalTitle = document.getElementById('addObservationModalLabel');
        if (modalTitle) modalTitle.textContent = 'Edit Observation';
        
        const submitBtn = document.getElementById('submitObservationBtn');
        if (submitBtn) submitBtn.innerHTML = '<i class="fa fa-check me-2"></i> Update Observation';
        
        console.log('Update mode set successfully');
    },

    // Reset form to CREATE mode
    resetUpdateMode: function() {
        this.isUpdateMode = false;
        this.observationCode = null;
        
        const modalTitle = document.getElementById('addObservationModalLabel');
        if (modalTitle) modalTitle.textContent = 'Record New Observation';
        
        const submitBtn = document.getElementById('submitObservationBtn');
        if (submitBtn) submitBtn.innerHTML = '<i class="fa fa-save me-2"></i> Record Observation';
    },

    handleSubmit: async function() {
        const submitBtn = document.getElementById('submitObservationBtn');
        
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
            
            // Optional: Add CSRF token if available (for FormData with multipart)
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            
            const headers = {};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }
            
            // Determine endpoint and method based on mode
            const endpoint = this.isUpdateMode ? `/api/observation/${this.observationCode}` : '/api/observation';
            const method = this.isUpdateMode ? 'PUT' : 'POST';
            const expectedStatus = this.isUpdateMode ? 200 : 201;
            
            // Single request with observation data + files
            const response = await fetch(endpoint, {
                method: method,
                headers: headers,  // Don't set Content-Type; browser will set it with boundary
                body: formData
            });

            const result = await response.json();

            if (response.ok) {
                const successMsg = this.isUpdateMode ? 'Observation updated successfully!' : 'Observation and files saved successfully!';
                this.showAlert('success', '<strong>' + successMsg + '</strong>');
                this.resetForm();
                this.resetUpdateMode();
                const redirectUrl = this.isUpdateMode ? `/observation/${this.observationCode}` : '/observation';
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
                submitBtn.innerHTML = '<i class="fa fa-check me-2"></i> Update Observation';
            } else {
                submitBtn.innerHTML = '<i class="fa fa-save me-2"></i> Record Observation';
            }
        }
    },

    collectFormDataAsMultipart: function() {
        const formData = new FormData();
        
        // Add observation fields
        formData.append('observationDate', document.getElementById('observationDate').value);
        formData.append('areaCode', document.getElementById('areaHidden').value);
        formData.append('category', document.getElementById('category').value);
        formData.append('type', document.getElementById('type').value);
        formData.append('description', document.getElementById('description').value);
        formData.append('status', document.getElementById('status').value);
        formData.append('mitigation', document.getElementById('mitigation').value);
        
        const observerId = document.getElementById('observerHidden').value;
        if (observerId) formData.append('observerId', observerId);
        
        const observerName = document.getElementById('observerName').value;
        if (observerName) formData.append('observerName', observerName);
        
        const responsibleId = document.getElementById('responsibleHidden').value;
        if (responsibleId) formData.append('responsibleId', responsibleId);
        
        const responsibleName = document.getElementById('responsibleName').value;
        if (responsibleName) formData.append('responsibleName', responsibleName);
        
        const correctiveAction = document.getElementById('correctiveAction').value;
        if (correctiveAction) formData.append('correctiveAction', correctiveAction);
        
        const remarks = document.getElementById('remarks').value;
        if (remarks) formData.append('remarks', remarks);
        
        // Add image files
        const imageFiles = document.getElementById('observationImages').files;
        for (let i = 0; i < imageFiles.length; i++) {
            formData.append('observationImages', imageFiles[i]);
        }
        
        // Add document files
        const documentFiles = document.getElementById('observationDocuments').files;
        for (let i = 0; i < documentFiles.length; i++) {
            formData.append('observationDocuments', documentFiles[i]);
        }
        
        return formData;
    },

    validateFormData: function() {
        if (!document.getElementById('observationDate').value) {
            return 'Observation date is required';
        }
        if (!document.getElementById('areaHidden').value) {
            return 'Area is required';
        }
        if (!document.getElementById('category').value) {
            return 'Category is required';
        }
        if (!document.getElementById('type').value) {
            return 'Type is required';
        }
        if (!document.getElementById('description').value) {
            return 'Description is required';
        }
        if (!document.getElementById('status').value) {
            return 'Status is required';
        }
        if (!document.getElementById('mitigation').value) {
            return 'Mitigation is required';
        }
        return null;
    },

    showAlert: function(type, message) {
        const existing = document.getElementById('observationFormAlert');
        if (existing) existing.remove();

        const alert = document.createElement('div');
        alert.id = 'observationFormAlert';
        alert.className = 'alert alert-' + type + ' alert-dismissible fade show mt-2';
        alert.innerHTML = message + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';

        const modalBody = document.querySelector('#addObservationModal .modal-body');
        if (modalBody) modalBody.prepend(alert);
    },

    resetForm: function() {
        document.getElementById('addObservationForm').reset();
        document.getElementById('areaHidden').value = '';
        document.getElementById('observerHidden').value = '';
        document.getElementById('responsibleHidden').value = '';
        const alert = document.getElementById('observationFormAlert');
        if (alert) alert.remove();
    }
};

document.addEventListener('DOMContentLoaded', () => ObservationForm.init());
