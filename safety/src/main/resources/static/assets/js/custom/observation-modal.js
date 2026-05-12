/**
 * Observation Modal Module
 * Handles live search functionality for Observation modal forms
 * Uses the same simplified approach as IncidentModalModule
 */

const ObservationModalModule = {
    init: function () {
        try {
            this.setupLiveSearch();
        } catch (error) {
            console.error('Error initializing ObservationModalModule:', error);
        }
    },

    // Setup Live Search with custom dropdowns
    setupLiveSearch: function () {
        try {
            // Initialize global data object if needed
            if (!window.observationIndexData) {
                window.observationIndexData = {};
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

            // Load observer data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/users/dropdown',
                inputId: 'observerName',
                hiddenId: 'observerHidden',
                dropdownId: 'observerDropdown',
                dataKey: 'observerOptions',
                idField: 'employeeId',
                displayFields: ['fullName', 'employeeId']
            });

            // Load responsible person data and setup filter
            this.loadDataAndSetupFilter({
                apiUrl: '/api/users/dropdown',
                inputId: 'responsibleName',
                hiddenId: 'responsibleHidden',
                dropdownId: 'responsibleDropdown',
                dataKey: 'responsibleOptions',
                idField: 'employeeId',
                displayFields: ['fullName', 'employeeId']
            });
        } catch (error) {
            console.error('Error setting up live search:', error);
        }
    },

    // Helper function to load data from API and setup filter
    loadDataAndSetupFilter: function (config) {
        const { apiUrl, inputId, hiddenId, dropdownId, dataKey, idField, displayFields } = config;
        const input = document.getElementById(inputId);
        const dropdown = document.getElementById(dropdownId);
        const self = this; // Preserve 'this' context for use in Promise

        if (!input || !dropdown) {
            return;
        }

        // Fetch data from API
        fetch(apiUrl)
            .then(response => response.json())
            .then(data => {
                if (!Array.isArray(data)) {
                    return;
                }

                // Store data globally
                window.observationIndexData[dataKey] = data;

                // Setup event listeners for autocomplete
                self.setupAutocomplete(input, dropdown, data, hiddenId, idField, displayFields);
            })
            .catch(error => console.error('Error loading data for ' + inputId + ':', error));
    },

    // Helper function to setup autocomplete on input field
    setupAutocomplete: function (input, dropdown, data, hiddenId, idField, displayFields) {
        const hiddenInput = document.getElementById(hiddenId);
        const self = this; // Preserve 'this' context

        input.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();

            // Filter data
            const filtered = data.filter(item => {
                const displayText = displayFields.map(field => item[field] || '').join(' ').toLowerCase();
                return displayText.includes(searchTerm);
            });

            // Render dropdown items
            self.renderDropdownItems(dropdown, filtered, input, hiddenInput, idField, displayFields);
        });

        input.addEventListener('focus', () => {
            self.renderDropdownItems(dropdown, data, input, hiddenInput, idField, displayFields);
        });

        input.addEventListener('blur', () => {
            setTimeout(() => {
                dropdown.style.display = 'none';
            }, 200);
        });
    },

    // Helper function to render dropdown items
    renderDropdownItems: function (dropdown, items, input, hiddenInput, idField, displayFields) {
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
window.ObservationModalModule = ObservationModalModule;

// Initialize the module when the DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        setTimeout(() => ObservationModalModule.init(), 100);
    });
} else {
    setTimeout(() => ObservationModalModule.init(), 100);
}
