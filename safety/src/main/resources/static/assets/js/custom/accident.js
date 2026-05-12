/**
 * Accident Module
 * Handles all accident-related functionality including modal management and form handling
 */

// Suppress Bootstrap backdrop errors globally
const originalConsoleError = console.error;
console.error = function(...args) {
    if (args[0] && typeof args[0] === 'string' && args[0].indexOf('backdrop') > -1) {
        console.warn('Suppressed backdrop error:', args);
        return;
    }
    originalConsoleError.apply(console, args);
};

const AccidentModule = {
    // Initialize the module
    init: function() {
        try {
            this.setupEventListeners();
            this.setupFormValidation();
            console.log('AccidentModule initialized successfully');
        } catch (error) {
            console.error('Error initializing AccidentModule:', error);
        }
    },

    // Setup event listeners
    setupEventListeners: function() {
        try {
            const form = document.getElementById('addAccidentForm');
            const modal = document.getElementById('addAccidentModal');
            const addAccidentBtn = document.getElementById('reportAccidentBtn');
            
            // Attach click handler to the button
            if (addAccidentBtn) {
                addAccidentBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    console.log('Report Accident button clicked');
                    this.openModal();
                });
                console.log('Report Accident button listener attached');
            } else {
                console.warn('reportAccidentBtn not found');
            }

            if (form) {
                form.addEventListener('submit', this.handleFormSubmit.bind(this));
                console.log('Form submission listener attached');
            } else {
                console.warn('addAccidentForm not found');
            }

            // Reset form when modal is closed
            if (modal) {
                modal.addEventListener('hidden.bs.modal', this.resetForm.bind(this));
                console.log('Modal hidden listener attached');
            } else {
                console.warn('addAccidentModal not found');
            }
        } catch (error) {
            console.error('Error setting up event listeners:', error);
        }
    },

    // Setup form validation
    setupFormValidation: function() {
        try {
            const form = document.getElementById('addAccidentForm');
            
            if (form) {
                // Add real-time validation
                const requiredFields = form.querySelectorAll('[required]');
                requiredFields.forEach(field => {
                    field.addEventListener('blur', this.validateField.bind(this));
                    field.addEventListener('change', this.validateField.bind(this));
                });
                console.log('Form validation setup complete');
            }
        } catch (error) {
            console.error('Error setting up form validation:', error);
        }
    },

    // Validate individual field
    validateField: function(event) {
        try {
            const field = event.target;
            const value = field.value.trim();

            if (field.hasAttribute('required') && !value) {
                field.classList.add('is-invalid');
                return false;
            } else {
                field.classList.remove('is-invalid');
                return true;
            }
        } catch (error) {
            console.error('Error validating field:', error);
        }
    },

    // Handle form submission
    handleFormSubmit: function(event) {
        try {
            console.log('=== ACCIDENT FORM SUBMIT HANDLER CALLED ===');
            console.log('Event:', event);
            console.log('Event target:', event.target);
            
            event.preventDefault();
            event.stopPropagation();
            console.log('✓ preventDefault and stopPropagation called');

            const form = event.target;
            console.log('Form target:', form, 'Form ID:', form.id);
            
            // Validate all required fields
            if (!this.validateAllFields(form)) {
                console.error('Validation failed');
                this.showErrorMessage('Please fill in all required fields');
                return false;
            }
            console.log('✓ All fields validated');

            // Show loading state
            this.showLoadingState(form);

            // Set redirectUrl to current page URL
            const redirectUrlInput = document.getElementById('redirectUrl');
            if (redirectUrlInput) {
                const currentUrl = window.location.pathname + window.location.search;
                redirectUrlInput.value = currentUrl;
                console.log('✓ redirectUrl set to:', currentUrl);
            }

            // Submit form directly with FormData (preserves file data)
            console.log('Submitting form directly with FormData - preserves file data');
            console.log('Form action:', form.action, 'Form method:', form.method);
            
            // Get FormData
            const formData = new FormData(form);
            console.log('FormData created with entries:');
            for (let [key, value] of formData.entries()) {
                if (value instanceof File) {
                    console.log(`  ${key}: File(${value.name}, ${value.size} bytes, ${value.type})`);
                } else {
                    console.log(`  ${key}: ${value}`);
                }
            }
            
            // Get the image file to verify
            const imageFile = formData.get('accidentImageFile');
            if (imageFile && imageFile instanceof File) {
                console.log('✓ Image file present in FormData - will be sent to server');
            }
            
            // Submit using form's native submit to preserve multipart/form-data
            form.submit();
            
            // Return false to prevent form submission
            return false;
        } catch (error) {
            console.error('Error handling form submission:', error);
            this.showErrorMessage('An error occurred while processing the form');
            return false;
        }
    },

    // New wrapper function for direct button click
    submitForm: function(form) {
        try {
            console.log('=== SUBMIT FORM BUTTON CLICKED ===');
            console.log('Form:', form);
            
            // Validate all required fields
            if (!this.validateAllFields(form)) {
                console.error('Validation failed');
                this.showErrorMessage('Please fill in all required fields');
                return false;
            }
            console.log('✓ All fields validated');

            // Show loading state
            this.showLoadingState(form);

            // Set redirectUrl to current page URL
            const redirectUrlInput = document.getElementById('redirectUrl');
            if (redirectUrlInput) {
                const currentUrl = window.location.pathname + window.location.search;
                redirectUrlInput.value = currentUrl;
                console.log('✓ redirectUrl set to:', currentUrl);
            }

            // Submit form directly with FormData (preserves file data)
            console.log('Submitting form directly with FormData - preserves file data');
            console.log('Form action:', form.action, 'Form method:', form.method);
            
            // Get FormData
            const formData = new FormData(form);
            console.log('FormData created with entries:');
            for (let [key, value] of formData.entries()) {
                if (value instanceof File) {
                    console.log(`  ${key}: File(${value.name}, ${value.size} bytes, ${value.type})`);
                } else {
                    console.log(`  ${key}: ${value}`);
                }
            }
            
            // Get the image file to verify
            const imageFile = formData.get('accidentImageFile');
            if (imageFile && imageFile instanceof File) {
                console.log('✓ Image file present in FormData - will be sent to server');
            }
            
            // Submit using form's native submit to preserve multipart/form-data
            form.submit();
            
            return false;
        } catch (error) {
            console.error('Error in submitForm:', error);
            this.showErrorMessage('An error occurred while processing the form');
            return false;
        }
    },

    // Validate all required fields
    validateAllFields: function(form) {
        try {
            const requiredFields = form.querySelectorAll('[required]');
            let isValid = true;

            requiredFields.forEach(field => {
                if (!field.value.trim()) {
                    field.classList.add('is-invalid');
                    isValid = false;
                } else {
                    field.classList.remove('is-invalid');
                }
            });

            return isValid;
        } catch (error) {
            console.error('Error validating all fields:', error);
            return false;
        }
    },

    // Prepare accident data from form
    prepareAccidentData: function(formData) {
        try {
            console.log('=== PREPARING ACCIDENT DATA ===');
            console.log('FormData object:', formData);
            
            // Log all entries
            console.log('FormData entries:');
            for (let [key, value] of formData.entries()) {
                if (value instanceof File) {
                    console.log(`  ${key}: File(${value.name}, ${value.size} bytes, ${value.type})`);
                } else {
                    console.log(`  ${key}: ${value}`);
                }
            }
            
            // Check for file
            const imageFile = formData.get('accidentImageFile');
            if (imageFile && imageFile instanceof File) {
                console.warn('⚠ WARNING: Image file found but will be LOST during JSON conversion!');
                console.warn('  File details:', {
                    name: imageFile.name,
                    size: imageFile.size,
                    type: imageFile.type
                });
            }
            
            const areaCode = (formData.get('areaCode') || '').trim();
            const areaName = (formData.get('areaName') || '').trim();
            const area = areaCode ? { code: areaCode, name: areaName } : null;

            const data = {
                accidentDate: formData.get('accidentDate'),
                area: area,
                description: formData.get('description'),
                severity: formData.get('severity'),
                status: formData.get('status'),
                affectedPersonName: formData.get('affectedPersonName') || '',
                affectedPersonEmployeeId: formData.get('affectedPersonEmployeeId') || '',
                affectedPersonPosition: formData.get('affectedPersonPosition') || '',
                injuryType: formData.get('injuryType') || '',
                causeOfAccident: formData.get('causeOfAccident') || '',
                witnesses: formData.get('witnesses') || '',
                medicalAttentionRequired: formData.get('medicalAttentionRequired') ? true : false,
                reportedBy: this.getCurrentUsername()
            };

            console.log('Accident data prepared (JSON, file omitted):', data);
            return data;
        } catch (error) {
            console.error('Error preparing accident data:', error);
            return null;
        }
    },

    // Get current username
    getCurrentUsername: function() {
        // Try to get from data attribute or return 'System'
        const userElement = document.querySelector('[data-username]');
        if (userElement) {
            return userElement.getAttribute('data-username');
        }
        return 'System';
    },

    // Submit accident data
    submitAccident: function(data, form) {
        try {
            if (!data) {
                this.showErrorMessage('Failed to prepare accident data');
                this.hideLoadingState(form);
                return;
            }

            console.log('[submitAccident] Sending POST to /api/observation/accident');
            console.log('[submitAccident] Data:', JSON.stringify(data, null, 2));

            fetch('/api/observation/accident', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': this.getCSRFToken()
                },
                body: JSON.stringify(data)
            })
            .then(response => {
                console.log('[submitAccident] Response status:', response.status);
                console.log('[submitAccident] Response headers:', response.headers);
                
                if (response.ok) {
                    console.log('[submitAccident] Success! Response OK');
                    return response.json().then(responseData => {
                        console.log('[submitAccident] Response data:', responseData);
                        this.showSuccessMessage('Accident reported successfully!');
                        this.closeModal();
                        // Reload the page to show the new accident
                        setTimeout(() => {
                            window.location.reload();
                        }, 1500);
                    });
                } else {
                    console.error('[submitAccident] Response NOT OK, status:', response.status);
                    return response.text().then(text => {
                        console.error('[submitAccident] Response text:', text);
                        throw new Error(`HTTP ${response.status}: ${text}`);
                    });
                }
            })
            .catch(error => {
                console.error('[submitAccident] Error:', error);
                this.showErrorMessage('An error occurred while reporting the accident: ' + error.message);
            })
            .finally(() => {
                this.hideLoadingState(form);
            });
        } catch (error) {
            console.error('[submitAccident] Caught exception:', error);
            this.showErrorMessage('An unexpected error occurred');
            this.hideLoadingState(form);
        }
    },

    // Show loading state
    showLoadingState: function(form) {
        try {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Submitting...';
            }
        } catch (error) {
            console.error('Error showing loading state:', error);
        }
    },

    // Hide loading state
    hideLoadingState: function(form) {
        try {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fa fa-save me-2"></i> Report Accident';
            }
        } catch (error) {
            console.error('Error hiding loading state:', error);
        }
    },

    // Show success message
    showSuccessMessage: function(message) {
        try {
            if (typeof Swal !== 'undefined' && Swal) {
                Swal.fire({
                    icon: 'success',
                    title: 'Success',
                    text: message,
                    confirmButtonColor: '#3085d6'
                });
            } else {
                alert(message);
            }
        } catch (error) {
            console.error('Error showing success message:', error);
            alert(message);
        }
    },

    // Show error message
    showErrorMessage: function(message) {
        try {
            if (typeof Swal !== 'undefined' && Swal) {
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: message,
                    confirmButtonColor: '#d33'
                });
            } else {
                alert('Error: ' + message);
            }
        } catch (error) {
            console.error('Error showing error message:', error);
            alert('Error: ' + message);
        }
    },

    // Open modal
    openModal: function() {
        try {
            const modal = document.getElementById('addAccidentModal');
            if (!modal) {
                console.error('Modal element not found');
                this.fallbackOpenModal();
                return;
            }

            console.log('Opening modal - checking for bootstrap availability');
            
            // Try to use bootstrap if available, but don't let errors stop us
            if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                try {
                    // Remove any existing instances
                    const existingInstance = bootstrap.Modal.getInstance(modal);
                    if (existingInstance) {
                        try {
                            existingInstance.dispose();
                        } catch (e) {
                            console.warn('Could not dispose existing modal instance');
                        }
                    }
                    
                    // Create fresh instance
                    const bsModal = new bootstrap.Modal(modal, {
                        backdrop: 'static',
                        keyboard: false,
                        focus: true
                    });
                    
                    bsModal.show();
                    console.log('Modal opened with Bootstrap');
                    return;
                } catch (bootstrapError) {
                    console.warn('Bootstrap modal initialization failed:', bootstrapError);
                }
            }
            
            // If bootstrap isn't available or failed, use fallback
            this.fallbackOpenModal();
            
        } catch (error) {
            console.error('Error in openModal:', error);
            this.fallbackOpenModal();
        }
    },

    // Fallback modal opening without Bootstrap
    fallbackOpenModal: function() {
        try {
            const modal = document.getElementById('addAccidentModal');
            if (modal) {
                modal.style.display = 'block';
                modal.classList.add('show');
                modal.setAttribute('aria-hidden', 'false');
                
                // Create and append backdrop
                let backdrop = document.querySelector('.modal-backdrop');
                if (!backdrop) {
                    backdrop = document.createElement('div');
                    backdrop.className = 'modal-backdrop fade show';
                    document.body.appendChild(backdrop);
                }
                
                // Prevent body scroll
                document.body.style.overflow = 'hidden';
                document.body.classList.add('modal-open');
                
                console.log('Modal opened using fallback method');
            }
        } catch (error) {
            console.error('Error in fallback modal opening:', error);
        }
    },

    // Close modal
    closeModal: function() {
        try {
            const modal = document.getElementById('addAccidentModal');
            if (!modal) {
                console.warn('Modal not found during close');
                return;
            }

            console.log('Closing modal');
            
            // Try bootstrap close first
            if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                try {
                    const bsModal = bootstrap.Modal.getInstance(modal);
                    if (bsModal) {
                        bsModal.hide();
                        console.log('Modal closed with Bootstrap');
                        return;
                    }
                } catch (e) {
                    console.warn('Bootstrap close failed:', e);
                }
            }
            
            // Use fallback
            this.fallbackCloseModal();
            
        } catch (error) {
            console.error('Error closing modal:', error);
            this.fallbackCloseModal();
        }
    },

    // Fallback modal closing without Bootstrap
    fallbackCloseModal: function() {
        try {
            const modal = document.getElementById('addAccidentModal');
            if (modal) {
                modal.style.display = 'none';
                modal.classList.remove('show');
                modal.setAttribute('aria-hidden', 'true');
                
                // Remove backdrop
                const backdrop = document.querySelector('.modal-backdrop');
                if (backdrop) {
                    backdrop.remove();
                }
                
                // Restore body scroll
                document.body.style.overflow = '';
                document.body.classList.remove('modal-open');
                
                console.log('Modal closed using fallback method');
            }
        } catch (error) {
            console.error('Error in fallback modal closing:', error);
        }
    },

    // Reset form
    resetForm: function() {
        try {
            const form = document.getElementById('addAccidentForm');
            if (form) {
                form.reset();
                form.querySelectorAll('.is-invalid').forEach(field => {
                    field.classList.remove('is-invalid');
                });
            }
        } catch (error) {
            console.error('Error resetting form:', error);
        }
    },

    // Get CSRF token from meta tag or cookie
    getCSRFToken: function() {
        try {
            // Try to get from meta tag first
            const metaTag = document.querySelector('meta[name="csrf-token"]');
            if (metaTag) {
                return metaTag.getAttribute('content');
            }

            // Try X-CSRF-TOKEN header
            const headerToken = document.querySelector('meta[name="_csrf"]');
            if (headerToken) {
                return headerToken.getAttribute('content');
            }

            // Otherwise try to get from cookie
            const name = '_csrf';
            const value = `; ${document.cookie}`;
            const parts = value.split(`; ${name}=`);
            if (parts.length === 2) {
                return parts.pop().split(';').shift();
            }

            return '';
        } catch (error) {
            console.error('Error getting CSRF token:', error);
            return '';
        }
    }
};

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        console.log('DOM Content Loaded - Initializing AccidentModule');
        setTimeout(() => {
            try {
                AccidentModule.init();
            } catch (error) {
                console.error('Failed to initialize AccidentModule:', error);
            }
        }, 100);
    });
} else {
    console.log('DOM already loaded - Initializing AccidentModule');
    try {
        AccidentModule.init();
    } catch (error) {
        console.error('Failed to initialize AccidentModule:', error);
    }
}

// Also add a small delay to ensure all other scripts have loaded
setTimeout(function() {
    console.log('Running delayed AccidentModule check');
    try {
        AccidentModule.init();
    } catch (error) {
        console.error('Failed to re-initialize AccidentModule:', error);
    }
}, 500);
