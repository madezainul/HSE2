/**
 * hse-document.js
 * Shared JS for document upload, delete, and PDF viewer
 * across Risk Assessment, Work Instruction, and Work Permit pages.
 */

'use strict';

// ── Upload ──────────────────────────────────────────────────────────────────

function initDocumentUpload(moduleUrl) {
    const form = document.getElementById('uploadDocForm');
    if (!form) return;

    form.addEventListener('submit', async function (e) {
        e.preventDefault();

        const submitBtn = form.querySelector('[type="submit"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin me-2"></i> Uploading...';

        const formData = new FormData(form);

        try {
            const resp = await fetch(`/api/documents/${moduleUrl}`, {
                method: 'POST',
                body: formData
            });
            const data = await resp.json();

            if (data.status === 'success') {
                showDocToast('success', data.message || 'Document uploaded successfully');
                bootstrap.Modal.getInstance(document.getElementById('uploadDocModal')).hide();
                form.reset();
                setTimeout(() => location.reload(), 900);
            } else {
                showDocToast('error', data.message || 'Upload failed');
            }
        } catch (err) {
            showDocToast('error', 'Network error: ' + err.message);
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    });
}

// ── Delete ───────────────────────────────────────────────────────────────────

async function deleteDocument(code, docName) {
    if (!confirm(`Are you sure you want to delete "${docName}"?\nThis action cannot be undone.`)) return;

    try {
        const resp = await fetch(`/api/documents/${code}`, { method: 'DELETE' });
        const data = await resp.json();

        if (data.status === 'success') {
            showDocToast('success', 'Document deleted successfully');
            setTimeout(() => location.reload(), 900);
        } else {
            showDocToast('error', data.message || 'Delete failed');
        }
    } catch (err) {
        showDocToast('error', 'Network error: ' + err.message);
    }
}

// ── PDF Viewer / Download ─────────────────────────────────────────────────

function openDocument(filePath, ext) {
    if (!ext) {
        window.open('/file/download/' + filePath, '_blank');
        return;
    }

    if (ext.toLowerCase() === '.pdf') {
        // Show inline PDF viewer modal
        const frame = document.getElementById('pdfViewerFrame');
        if (frame) {
            frame.src = '/file/' + filePath;
            const modal = new bootstrap.Modal(document.getElementById('pdfViewerModal'));
            modal.show();

            // Clear iframe src when modal is closed to stop loading
            document.getElementById('pdfViewerModal').addEventListener('hidden.bs.modal', function () {
                frame.src = '';
            }, { once: true });
        }
    } else {
        // Force download for non-PDF types
        window.open('/file/download/' + filePath, '_blank');
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function showDocToast(type, message) {
    // Try SweetAlert if available
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            icon: type === 'success' ? 'success' : 'error',
            title: type === 'success' ? 'Success' : 'Error',
            text: message,
            timer: 2500,
            showConfirmButton: false
        });
        return;
    }

    // Fallback: simple Bootstrap toast or alert
    const container = document.getElementById('docToastContainer');
    if (container) {
        const id = 'toast_' + Date.now();
        const bg = type === 'success' ? 'bg-success' : 'bg-danger';
        container.insertAdjacentHTML('beforeend', `
            <div id="${id}" class="toast align-items-center text-white ${bg} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>`);
        const toastEl = document.getElementById(id);
        new bootstrap.Toast(toastEl, { delay: 3000 }).show();
        toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
    } else {
        alert(message);
    }
}

// ── Table button delegation ───────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', function () {
    document.addEventListener('click', function (e) {
        const viewBtn = e.target.closest('.btn-view-doc');
        if (viewBtn) {
            openDocument(viewBtn.dataset.filepath, viewBtn.dataset.ext);
            return;
        }
        const dlBtn = e.target.closest('.btn-download-doc');
        if (dlBtn) {
            window.open('/file/download/' + dlBtn.dataset.filepath, '_blank');
            return;
        }
        const delBtn = e.target.closest('.btn-delete-doc');
        if (delBtn) {
            deleteDocument(delBtn.dataset.code, delBtn.dataset.docname);
        }
    });
});
