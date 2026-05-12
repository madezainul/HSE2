/**
 * Toolbox Meeting Modal Module
 * Handles live search for Supervisor field and form submission.
 */

const ToolboxModalModule = {

    init: function () {
        try {
            this.setupLiveSearch();
            this.setupFormSubmit();
        } catch (error) {
            console.error('Error initialising ToolboxModalModule:', error);
        }
    },

    setupLiveSearch: function () {
        this.loadDataAndSetupFilter({
            apiUrl: '/api/users/dropdown',
            inputId: 'toolboxSupervisorName',
            hiddenId: 'toolboxSupervisorHidden',
            dropdownId: 'toolboxSupervisorDropdown',
            dataKey: 'supervisorOptions',
            idField: 'employeeId',
            displayFields: ['fullName', 'employeeId']
        });
    },

    loadDataAndSetupFilter: function (config) {
        var self = this;
        var input    = document.getElementById(config.inputId);
        var dropdown = document.getElementById(config.dropdownId);

        if (!input || !dropdown) return;

        fetch(config.apiUrl)
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (!Array.isArray(data)) return;
                if (!window.toolboxModalData) window.toolboxModalData = {};
                window.toolboxModalData[config.dataKey] = data;
                self.setupAutocomplete(input, dropdown, data, config.hiddenId, config.idField, config.displayFields);
            })
            .catch(function (e) { console.error('Error loading data for ' + config.inputId, e); });
    },

    setupAutocomplete: function (input, dropdown, data, hiddenId, idField, displayFields) {
        var self = this;
        var hiddenInput = document.getElementById(hiddenId);

        input.addEventListener('input', function () {
            var term = input.value.toLowerCase();
            var filtered = data.filter(function (item) {
                return displayFields.map(function (f) { return item[f] || ''; }).join(' ').toLowerCase().includes(term);
            });
            self.renderDropdownItems(dropdown, filtered, input, hiddenInput, idField, displayFields);
        });

        input.addEventListener('focus', function () {
            self.renderDropdownItems(dropdown, data, input, hiddenInput, idField, displayFields);
        });

        input.addEventListener('blur', function () {
            setTimeout(function () { dropdown.style.display = 'none'; }, 200);
        });
    },

    renderDropdownItems: function (dropdown, items, input, hiddenInput, idField, displayFields) {
        dropdown.innerHTML = '';
        if (items.length === 0) {
            dropdown.style.display = 'none';
            return;
        }
        items.forEach(function (item) {
            var a = document.createElement('a');
            a.className = 'dropdown-item';
            a.href = '#';
            a.textContent = displayFields.map(function (f) { return item[f] || ''; }).join(' – ');
            a.addEventListener('mousedown', function (e) {
                e.preventDefault();
                input.value = displayFields.map(function (f) { return item[f] || ''; }).join(' – ');
                if (hiddenInput) hiddenInput.value = item[idField] || '';
                dropdown.style.display = 'none';
            });
            dropdown.appendChild(a);
        });
        dropdown.style.display = 'block';
    },

    setupFormSubmit: function () {
        var form    = document.getElementById('addToolboxForm');
        var btn     = document.getElementById('submitToolboxBtn');
        var spinner = document.getElementById('submitToolboxSpinner');
        var errorDiv = document.getElementById('toolboxFormError');

        if (!form || !btn) return;

        form.addEventListener('submit', function (e) {
            e.preventDefault();

            var locationVal = document.getElementById('toolboxLocation').value.trim();
            var date        = document.getElementById('toolboxDate').value;
            var timeStarted = document.getElementById('toolboxTimeStarted').value;
            var status      = document.getElementById('toolboxStatus').value;

            if (!locationVal || !date || !timeStarted || !status) {
                errorDiv.textContent = 'Location, Date, Time Started and Status are required.';
                errorDiv.classList.remove('d-none');
                return;
            }
            errorDiv.classList.add('d-none');

            var csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

            var formData = new FormData(form);

            btn.disabled = true;
            spinner.classList.remove('d-none');

            var headers = {};
            if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

            fetch('/api/toolbox', {
                method: 'POST',
                headers: headers,
                body: formData
            })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                spinner.classList.add('d-none');
                btn.disabled = false;
                if (data.status !== 'success') {
                    errorDiv.textContent = data.message || 'Failed to save toolbox meeting.';
                    errorDiv.classList.remove('d-none');
                    return;
                }
                bootstrap.Modal.getInstance(document.getElementById('addToolboxModal')).hide();
                window.location.reload();
            })
            .catch(function (err) {
                spinner.classList.add('d-none');
                btn.disabled = false;
                errorDiv.textContent = 'Request failed. Please try again.';
                errorDiv.classList.remove('d-none');
                console.error('Toolbox form submit error:', err);
            });
        });
    }
};

document.addEventListener('DOMContentLoaded', function () {
    ToolboxModalModule.init();
});
