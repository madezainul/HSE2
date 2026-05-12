(function () {
    const AccidentIndexModule = {
        /**
         * Load data into a datalist element from an API endpoint
         * @param {Object} config - Configuration object
         * @param {string} config.datalistId - ID of the datalist element
         * @param {string} config.apiUrl - API endpoint URL
         * @param {string} config.idField - Field name to use as ID (e.g., 'code', 'id')
         * @param {Array} config.setupCallbacks - Array of callback functions to execute after loading
         */
        datalistLoader: function (config) {
            try {
                const { datalistId, apiUrl, idField, setupCallbacks } = config;
                const datalist = document.getElementById(datalistId);

                if (!datalist) {
                    console.warn(`Datalist element with id '${datalistId}' not found`);
                    return;
                }

                // Fetch data from API
                fetch(apiUrl)
                    .then(response => {
                        if (!response.ok) {
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }
                        return response.json();
                    })
                    .then(data => {
                        if (!Array.isArray(data)) {
                            console.warn('API response is not an array');
                            console.warn('Response data:', data);
                            return;
                        }

                        // Initialize global data object if it doesn't exist
                        if (!window.accidentIndexData) {
                            window.accidentIndexData = {};
                        }

                        // Store the data globally for filter use (merge instead of replace)
                        window.accidentIndexData[datalistId] = data.map(item => {
                            // Determine the display name
                            let displayName = item.name || item.fullName || item.label || item[idField] || '';
                            // Determine the ID value
                            let idValue = item[idField] || item.id || '';
                            // Format as "Name (ID)"
                            let display = idValue ? `${displayName} (${idValue})` : displayName;

                            return {
                                display: display,
                                id: idValue
                            };
                        });

                        console.log(`Loaded ${data.length} items for ${datalistId}`);

                        // Execute setup callbacks
                        if (Array.isArray(setupCallbacks)) {
                            setupCallbacks.forEach(callback => {
                                if (typeof callback === 'function') {
                                    try {
                                        callback();
                                    } catch (error) {
                                        console.error('Error executing setup callback:', error);
                                    }
                                }
                            });
                        }
                    })
                    .catch(error => {
                        console.error(`Error loading data from ${apiUrl}:`, error);
                    });
            } catch (error) {
                console.error('Error in datalistLoader:', error);
            }
        },

        /**
         * Setup filter input with hidden field for storing ID values
         * @param {Object} config - Configuration object
         * @param {string} config.inputId - ID of the input element
         * @param {string} config.hiddenId - ID of the hidden field to store the ID
         * @param {string} config.datalistId - ID of the datalist element (for data access)
         * @param {string} config.dropdownId - ID of the dropdown div to populate with items
         * @param {string} config.idAttr - Data attribute name for the ID (e.g., 'code')
         */
        setupFilterInput: function (config) {
            try {
                const { inputId, hiddenId, datalistId, dropdownId, idAttr } = config;
                const input = document.getElementById(inputId);
                const hiddenField = document.getElementById(hiddenId);
                const dropdown = document.getElementById(dropdownId);

                if (!input) {
                    console.warn(`Input element with id '${inputId}' not found`);
                    return;
                }

                if (!dropdown) {
                    console.warn(`Dropdown element with id '${dropdownId}' not found`);
                    return;
                }

                // Helper function to render dropdown items
                const renderDropdownItems = (items) => {
                    dropdown.innerHTML = '';

                    if (items.length === 0) {
                        return;
                    }

                    items.forEach(item => {
                        const itemEl = document.createElement('a');
                        itemEl.href = '#';
                        itemEl.className = 'dropdown-item';
                        itemEl.textContent = item.display;
                        itemEl.setAttribute('data-id', item.id);

                        itemEl.addEventListener('click', (e) => {
                            e.preventDefault();
                            console.log(`Selected: ${item.display}, id: ${item.id}`);
                            input.value = item.display;
                            if (hiddenField) {
                                hiddenField.value = item.id;
                            }
                            dropdown.style.display = 'none';
                        });

                        dropdown.appendChild(itemEl);
                    });
                };

                // Handle input events for filtering
                input.addEventListener('input', (e) => {
                    const searchTerm = e.target.value.toLowerCase();

                    if (!window.accidentIndexData || !window.accidentIndexData[datalistId]) {
                        return;
                    }

                    const allItems = window.accidentIndexData[datalistId];

                    const filtered = allItems.filter(item =>
                        item.display.toLowerCase().includes(searchTerm) ||
                        item.id.toLowerCase().includes(searchTerm)
                    );

                    if (searchTerm.length > 0 && filtered.length > 0) {
                        renderDropdownItems(filtered);
                        dropdown.style.display = 'block';
                    } else if (searchTerm.length === 0) {
                        renderDropdownItems(allItems);
                        dropdown.style.display = 'block';
                    } else {
                        dropdown.style.display = 'none';
                    }
                });

                // Handle focus to show all items
                input.addEventListener('focus', () => {
                    if (window.accidentIndexData && window.accidentIndexData[datalistId]) {
                        const items = window.accidentIndexData[datalistId];
                        renderDropdownItems(items);
                        dropdown.style.display = 'block';
                    }
                });

                // Handle blur to hide dropdown
                input.addEventListener('blur', () => {
                    setTimeout(() => {
                        dropdown.style.display = 'none';
                    }, 200);
                });

                console.log(`Filter input setup complete for ${inputId}`);
            } catch (error) {
                console.error('Error in setupFilterInput:', error);
            }
        }
    };

    // Expose module globally for other scripts to consume
    window.AccidentIndexModule = AccidentIndexModule;

    console.log('AccidentIndexModule loaded successfully');

})();