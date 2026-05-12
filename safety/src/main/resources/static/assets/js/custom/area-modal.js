/**
 * Area Modal Module
 * Handles Add / Edit area modal, checkbox selection, and delete-selected.
 */

// ─── Helpers ──────────────────────────────────────────────────────────────────

function getCsrfHeaders() {
    const token  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json' };
    if (token && header) headers[header] = token;
    return headers;
}

function statusBadgeHtml(status) {
    if (status === 'ACTIVE')            return '<span class="badge bg-success">ACTIVE</span>';
    if (status === 'INACTIVE')          return '<span class="badge bg-secondary">INACTIVE</span>';
    if (status === 'UNDER_MAINTENANCE') return '<span class="badge bg-warning text-dark">UNDER MAINTENANCE</span>';
    return '';
}

// ─── Edit Area (called inline from table row button) ──────────────────────────

function editArea(btn) {
    var areaId = btn.getAttribute('data-id');

    fetch('/areas/' + areaId + '/data', { headers: getCsrfHeaders() })
        .then(function(res) { return res.json(); })
        .then(function(area) {
            document.getElementById('areaModalLabel').textContent  = 'Edit Area';
            document.getElementById('areaId').value                = area.id;
            document.getElementById('areaCode').value              = area.code;
            document.getElementById('areaName').value              = area.name;
            document.getElementById('areaDescription').value       = area.description || '';
            document.getElementById('areaStatus').value            = area.status;
            document.getElementById('areaFormError').classList.add('d-none');
            new bootstrap.Modal(document.getElementById('areaModal')).show();
        })
        .catch(function() {
            alert('Failed to load area data. Please try again.');
        });
}

// ─── DOMContentLoaded ─────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', function() {

    var selectAllCheckbox = document.getElementById('selectAllCheckbox');
    var selectedCountSpan = document.getElementById('selectedCount');
    var exportBtn         = document.getElementById('exportBtn');
    var deleteBtn         = document.getElementById('deleteBtn');
    var addAreaBtn        = document.getElementById('addAreaBtn');
    var saveAreaBtn       = document.getElementById('saveAreaBtn');

    // ── Checkbox helpers ──

    function getCheckboxes() {
        return Array.from(document.querySelectorAll('.area-checkbox'));
    }

    function updateSelectedCount() {
        var n = getCheckboxes().filter(function(cb) { return cb.checked; }).length;
        if (selectedCountSpan) selectedCountSpan.textContent = n;
    }

    function updateButtonStates() {
        var n = getCheckboxes().filter(function(cb) { return cb.checked; }).length;
        if (exportBtn) exportBtn.disabled = n === 0;
        if (deleteBtn) deleteBtn.disabled = n === 0;
    }

    function syncSelectAll() {
        var all  = getCheckboxes();
        var checked = all.filter(function(cb) { return cb.checked; });
        if (!selectAllCheckbox) return;
        selectAllCheckbox.checked       = checked.length === all.length && all.length > 0;
        selectAllCheckbox.indeterminate = checked.length > 0 && checked.length < all.length;
    }

    // ── Select-all checkbox (above table) ──
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            getCheckboxes().forEach(function(cb) { cb.checked = selectAllCheckbox.checked; });
            updateSelectedCount();
            updateButtonStates();
        });
    }

    // ── Table-header checkbox ──
    var tableHeaderCheckbox = document.querySelector('thead input[type="checkbox"]');
    if (tableHeaderCheckbox) {
        tableHeaderCheckbox.addEventListener('change', function() {
            getCheckboxes().forEach(function(cb) { cb.checked = tableHeaderCheckbox.checked; });
            if (selectAllCheckbox) selectAllCheckbox.checked = tableHeaderCheckbox.checked;
            updateSelectedCount();
            updateButtonStates();
        });
    }

    // ── Individual checkboxes (event delegation on tbody) ──
    var tbody = document.querySelector('tbody');
    if (tbody) {
        tbody.addEventListener('change', function(e) {
            if (e.target.classList.contains('area-checkbox')) {
                updateSelectedCount();
                updateButtonStates();
                syncSelectAll();
            }
        });
    }

    // ── Delete Selected ──
    if (deleteBtn) {
        deleteBtn.addEventListener('click', function() {
            var selected = getCheckboxes()
                .filter(function(cb) { return cb.checked; })
                .map(function(cb) { return cb.value; });

            if (selected.length === 0) return;
            if (!confirm('Are you sure you want to delete ' + selected.length + ' area(s)? This cannot be undone.')) return;

            fetch('/areas/batch', {
                method: 'DELETE',
                headers: getCsrfHeaders(),
                body: JSON.stringify(selected)
            })
            .then(function(res) { return res.json(); })
            .then(function() {
                selected.forEach(function(id) {
                    var row = document.getElementById('area-' + id);
                    if (row) row.remove();
                });
                var badge = document.querySelector('.badge.badge-primary');
                if (badge) {
                    badge.textContent = document.querySelectorAll('.area-checkbox').length + ' Total';
                }
                updateSelectedCount();
                updateButtonStates();
            })
            .catch(function(err) {
                console.error('Delete failed:', err);
                alert('Failed to delete areas. Please try again.');
            });
        });
    }

    // ── Add New Area ──
    if (addAreaBtn) {
        addAreaBtn.addEventListener('click', function() {
            document.getElementById('areaModalLabel').textContent = 'Add New Area';
            document.getElementById('areaId').value               = '';
            document.getElementById('areaCode').value             = '';
            document.getElementById('areaName').value             = '';
            document.getElementById('areaDescription').value      = '';
            document.getElementById('areaStatus').value           = 'ACTIVE';
            document.getElementById('areaFormError').classList.add('d-none');
            new bootstrap.Modal(document.getElementById('areaModal')).show();
        });
    }

    // ── Save (Create or Update) ──
    if (saveAreaBtn) {
        saveAreaBtn.addEventListener('click', function() {
            var id          = document.getElementById('areaId').value;
            var code        = document.getElementById('areaCode').value.trim();
            var name        = document.getElementById('areaName').value.trim();
            var description = document.getElementById('areaDescription').value.trim();
            var status      = document.getElementById('areaStatus').value;
            var errorDiv    = document.getElementById('areaFormError');
            var spinner     = document.getElementById('saveAreaSpinner');

            if (!code || !name) {
                errorDiv.textContent = 'Code and Name are required.';
                errorDiv.classList.remove('d-none');
                return;
            }
            errorDiv.classList.add('d-none');
            spinner.classList.remove('d-none');

            var isEdit = id !== '';
            var url    = isEdit ? '/areas/' + id : '/areas';
            var method = isEdit ? 'PUT' : 'POST';

            fetch(url, {
                method:  method,
                headers: getCsrfHeaders(),
                body:    JSON.stringify({ code: code, name: name, description: description, status: status })
            })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                spinner.classList.add('d-none');
                if (!data.success) {
                    errorDiv.textContent = data.message || 'Failed to save area.';
                    errorDiv.classList.remove('d-none');
                    return;
                }
                bootstrap.Modal.getInstance(document.getElementById('areaModal')).hide();
                if (isEdit) {
                    var row = document.getElementById('area-' + id);
                    if (row) {
                        row.cells[1].textContent = data.code;
                        row.cells[2].textContent = data.name;
                        row.cells[3].textContent = data.description || '-';
                        row.cells[4].innerHTML   = statusBadgeHtml(data.status);
                    }
                } else {
                    location.reload();
                }
            })
            .catch(function(err) {
                spinner.classList.add('d-none');
                errorDiv.textContent = 'Request failed. Please try again.';
                errorDiv.classList.remove('d-none');
            });
        });
    }
});
