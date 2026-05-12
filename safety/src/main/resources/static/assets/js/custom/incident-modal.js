/**
 * Incident Modal Module
 * Handles live search functionality for Incident modal forms
 * Uses the same simplified approach as AccidentModalModule
 */

const IncidentModalModule = {
    init: function() {
        try {
            this.setupLiveSearch();
            console.log('IncidentModalModule initialized successfully');
        } catch (error) {
            console.error('Error initializing IncidentModalModule:', error);
        }
    },

    // Setup Live Search with custom dropdowns
    setupLiveSearch: function() {
        try {
            // Initialize global data object if needed
            if (!window.incidentIndexData) {
                window.incidentIndexData = {};
            }

            // Load area data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/areas/dropdown',
                inputId: 'areaInput',
                hiddenId: 'areaHidden',
                dropdownId: 'areaDropdown',
                dataKey: 'areaOptions',
                idField: 'code',
                displayFields: ['name', 'code']
            });

            // Load involved person data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/users/dropdown',
                inputId: 'involvedPersonName',
                hiddenId: 'involvedPersonHidden',
                dropdownId: 'involvedPersonDropdown',
                dataKey: 'involvedPersonOptions',
                idField: 'employeeId',
                displayFields: ['fullName', 'employeeId']
            });

            // Load witness data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/users/dropdown',
                inputId: 'witnesses',
                hiddenId: 'witnessesHidden',
                dropdownId: 'witnessesDropdown',
                dataKey: 'witnessesOptions',
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
                    console.warn('Response:', data);
                    return;
                }

                // Store data globally
                window.incidentIndexData[dataKey] = data;
                console.log(`Loaded ${data.length} items for ${inputId}`);

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
            const displayText = displayFields.map(field => item[field] || '').join(' - ');
            option.textContent = displayText;
            option.addEventListener('click', () => {
                // For inputs with both name and ID, save only the first field (name) to visible input
                // and the ID to hidden input
                const nameValue = item[displayFields[0]] || '';
                input.value = nameValue;
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

// Expose module globally
window.IncidentModalModule = IncidentModalModule;

// Initialize the module when the DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        console.log('DOM Content Loaded - Initializing IncidentModalModule');
        setTimeout(() => IncidentModalModule.init(), 100);
    });
} else {
    console.log('DOM already loaded - Initializing IncidentModalModule');
    setTimeout(() => IncidentModalModule.init(), 100);
}