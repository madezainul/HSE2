/**
 * Accident Modal Module
 * Handles live search functionality for accident modal forms
 */

const AccidentModalModule = {
    init: function() {
        try {
            // Wait for AccidentIndexModule to be available
            if (typeof AccidentIndexModule === 'undefined') {
                console.warn('AccidentIndexModule not available, retrying in 200ms...');
                setTimeout(() => this.init(), 200);
                return;
            }
            
            this.setupLiveSearch();
            console.log('AccidentModalModule initialized successfully');
        } catch (error) {
            console.error('Error initializing AccidentModalModule:', error);
        }
    },

    // Setup Live Search with custom dropdowns (no datalist elements)
    setupLiveSearch: function() {
        try {
            // Initialize global data object if needed
            if (!window.accidentIndexData) {
                window.accidentIndexData = {};
            }

            // Load area data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/areas/dropdown',
                inputId: 'areaInput',
                hiddenId: 'areaHidden',
                dropdownId: 'dropdown',
                dataKey: 'areaOptions',
                idField: 'code',
                displayFields: ['name', 'code']
            });

            // Load affected person data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/users/dropdown',
                inputId: 'affectedPersonName',
                hiddenId: 'affectedPersonHidden',
                dropdownId: 'affectedPersonDropdown',
                dataKey: 'affectedPersonOptions',
                idField: 'employeeId',
                displayFields: ['fullName', 'employeeId']
            });

            // Load witness data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/users/dropdown',
                inputId: 'witnessName',
                hiddenId: 'witnessHidden',
                dropdownId: 'witnessDropdown',
                dataKey: 'witnessOptions',
                idField: 'employeeId',
                displayFields: ['fullName', 'employeeId']
            });

            console.log('Live search setup complete for custom dropdowns');
        } catch (error) {
            console.error('Error setting up live search:', error);
        }
    },

    // Helper function to load data from API and setup filter
    loadDataAndSetupFilter: function(config) {
        const { apiUrl, inputId, hiddenId, dropdownId, dataKey, idField, displayFields } = config;
        const input = document.getElementById(inputId);
        const dropdown = document.getElementById(dropdownId);

        if (!input || !dropdown) {
            console.warn(`Elements not found for ${inputId}`);
            return;
        }

        // Fetch data from API
        fetch(apiUrl)
            .then(response => response.json())
            .then(data => {
                if (!Array.isArray(data)) {
                    console.warn('API response is not an array');
                    return;
                }

                // Store data globally
                window.accidentIndexData[dataKey] = data;

                // Setup event listeners for autocomplete
                this.setupAutocomplete(input, dropdown, data, hiddenId, idField, displayFields);
            })
            .catch(error => console.error('Error loading data for ' + inputId + ':', error));
    },

    // Helper function to setup autocomplete on input field
    setupAutocomplete: function(input, dropdown, data, hiddenId, idField, displayFields) {
        const hiddenInput = document.getElementById(hiddenId);

        input.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();
            
            // Filter data
            const filtered = data.filter(item => {
                const displayText = displayFields.map(field => item[field] || '').join(' ').toLowerCase();
                return displayText.includes(searchTerm);
            });

            // Render dropdown items
            this.renderDropdownItems(dropdown, filtered, input, hiddenInput, idField, displayFields);
        });

        input.addEventListener('focus', () => {
            this.renderDropdownItems(dropdown, data, input, hiddenInput, idField, displayFields);
        });

        input.addEventListener('blur', () => {
            setTimeout(() => {
                dropdown.style.display = 'none';
            }, 200);
        });
    },

    // Helper function to render dropdown items
    renderDropdownItems: function(dropdown, items, input, hiddenInput, idField, displayFields) {
        dropdown.innerHTML = '';

        items.forEach(item => {
            const option = document.createElement('div');
            option.className = 'dropdown-item';
            option.style.padding = '0.5rem 1rem';
            option.style.cursor = 'pointer';
            const displayText = displayFields.map(field => item[field] || '').join(' ');
            option.textContent = displayText;
            option.addEventListener('click', () => {
                input.value = displayText;
                if (hiddenInput) {
                    hiddenInput.value = item[idField] || '';
                }
                dropdown.style.display = 'none';
            });
            option.addEventListener('mouseenter', () => {
                option.style.backgroundColor = '#f0f0f0';
            });
            option.addEventListener('mouseleave', () => {
                option.style.backgroundColor = 'transparent';
            });
            dropdown.appendChild(option);
        });

        dropdown.style.display = items.length > 0 ? 'block' : 'none';
    }
};

// Image preview on file selection
    document.getElementById('accidentImage')?.addEventListener('change', (evt) => {
        const preview = document.getElementById('addImagePreview');
        const file = evt.target.files?.[0];
        
        console.log('Image file selection event triggered');
        console.log('File selected:', file);
        
        if (file && file.type.startsWith('image/')) {
            console.log('Valid image file detected:', {
                name: file.name,
                size: file.size,
                type: file.type
            });
            
            const reader = new FileReader();
            reader.onload = () => {
                preview.src = reader.result;
                preview.style.display = 'block';
                console.log('Image preview loaded successfully');
            };
            reader.readAsDataURL(file);
        } else {
            console.warn('Invalid file or not an image:', file?.type);
        }
    });

// Initialize the module when the DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        console.log('DOM Content Loaded - Initializing AccidentModalModule');
        setTimeout(() => AccidentModalModule.init(), 100);
    });
} else {
    console.log('DOM already loaded - Initializing AccidentModalModule');
    setTimeout(() => AccidentModalModule.init(), 100);
}
