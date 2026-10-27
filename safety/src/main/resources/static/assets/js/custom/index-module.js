/**
 * IndexModule - shared helpers for list pages: filter and sort URLs, header-click sorting and
 * the show / hide columns menu.
 *
 * Ported from the maintenance project's index-module.js (only the functions the list pages use).
 * Differences from the original:
 *  - page numbers start at 0 (Spring's PageRequest), the original started at 1
 *  - the column toggle ignores "no records" rows that have a single colspan cell
 *  - rows with a colspan cell are never moved by header sorting
 */
const IndexModule = (function () {

    // ── URL UTILS ──────────────────────────────────────────────────────
    function findGetParam(name) {
        const m = new RegExp('[?&]' + name + '=([^&#]*)').exec(location.search);
        return m ? decodeURIComponent(m[1].replace(/\+/g, ' ')) : '';
    }

    // ── CLIENT SIDE SORTING (click a column header; sorts the rows on the current page) ──
    function sortTable(tableId, field, direction, datetimeFieldSet, dateFieldSet) {
        const table = document.getElementById(tableId);
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr')).filter(r => !r.querySelector('td[colspan]'));

        const isDatetime = datetimeFieldSet.has(field.toLowerCase());
        const isDate = dateFieldSet.has(field.toLowerCase());

        rows.sort((a, b) => {
            const aValue = a.querySelector(`[data-field="${field}"]`)?.textContent.trim() || '';
            const bValue = b.querySelector(`[data-field="${field}"]`)?.textContent.trim() || '';

            let comparison = 0;

            if (isDatetime || isDate) {
                // "YYYY-MM-DD" and "YYYY-MM-DD HH:mm"; empty values go last
                const aDate = aValue ? new Date(aValue.replace(' ', 'T')) : null;
                const bDate = bValue ? new Date(bValue.replace(' ', 'T')) : null;
                const aOk = aDate && !isNaN(aDate);
                const bOk = bDate && !isNaN(bDate);

                if (aOk && bOk) comparison = aDate - bDate;
                else if (aOk) comparison = -1;
                else if (bOk) comparison = 1;
            } else {
                const aNum = parseFloat(aValue.replace(/,/g, ''));
                const bNum = parseFloat(bValue.replace(/,/g, ''));

                if (!isNaN(aNum) && !isNaN(bNum)) {
                    comparison = aNum - bNum;
                } else {
                    comparison = aValue.localeCompare(bValue, undefined, { numeric: true, sensitivity: 'base' });
                }
            }

            return direction === 'asc' ? comparison : -comparison;
        });

        rows.forEach(row => tbody.appendChild(row));
    }

    function enableClientSideSorting({ tableId, datetimeFields = [], dateFields = [] }) {
        const table = document.getElementById(tableId);
        if (!table) return;
        const headers = table.querySelectorAll('th[data-sort]');
        let currentSort = { field: null, direction: 'asc' };

        const datetimeFieldSet = new Set(datetimeFields.map(f => f.toLowerCase()));
        const dateFieldSet = new Set(dateFields.map(f => f.toLowerCase()));

        headers.forEach(header => {
            header.addEventListener('click', () => {
                const field = header.dataset.sort;
                const isAsc = currentSort.field === field && currentSort.direction === 'asc';
                const direction = isAsc ? 'desc' : 'asc';

                headers.forEach(h => {
                    const icon = h.querySelector('.sort-indicator');
                    if (icon) icon.className = 'fas fa-sort text-white-50 ml-1 sort-indicator';
                });

                const activeIcon = header.querySelector('.sort-indicator');
                if (activeIcon) {
                    activeIcon.className = `fas fa-sort-${direction === 'asc' ? 'up' : 'down'} text-white ml-1 sort-indicator`;
                }

                currentSort = { field, direction };
                sortTable(tableId, field, direction, datetimeFieldSet, dateFieldSet);
            });
        });
    }

    // ── URL BUILDERS (filter and sort reload the page; the server does the real work) ──
    function buildFilterUrl({ basePath, filterFields, specialFields = {}, multiValueFields = {}, containerId = null, firstPage = 0 }) {
        const container = (containerId && document.getElementById(containerId)) || document;
        const url = new URL(window.location);

        filterFields.forEach(k => {
            const el = container.querySelector(`[name="${k}"]`);
            url.searchParams.set(k, el?.value?.trim() || '');
        });

        for (const [paramName, getter] of Object.entries(specialFields)) {
            url.searchParams.set(paramName, getter());
        }

        for (const [paramName, tagListId] of Object.entries(multiValueFields)) {
            const tagList = container.querySelector(`#${tagListId}`);
            if (!tagList) continue;
            const values = Array.from(tagList.querySelectorAll('.tag'))
                .map(tag => tag.dataset.value)
                .filter(v => v?.trim() !== '');
            url.searchParams.set(paramName, values.join(','));
        }

        url.searchParams.set('page', String(firstPage));
        const hidden = findGetParam('hiddenColumns');
        if (hidden) url.searchParams.set('hiddenColumns', hidden);
        url.pathname = basePath;
        return url.toString();
    }

    function buildSortUrl({ basePath, paramFields, sortByValue }) {
        const params = [];
        paramFields.forEach(k => {
            const val = findGetParam(k);
            if (val) params.push(`${k}=${encodeURIComponent(val)}`);
        });

        const hidden = findGetParam('hiddenColumns');
        if (hidden) params.push(`hiddenColumns=${encodeURIComponent(hidden)}`);

        params.push(`sortBy=${encodeURIComponent(sortByValue)}`);
        params.push(`asc=${encodeURIComponent(document.getElementById('ascInput')?.value || 'true')}`);

        return `${basePath}?${params.join('&')}`;
    }

    // ── SHOW / HIDE COLUMNS ────────────────────────────────────────────
    // columnSettings must be in the same order as the table's <th> cells.
    // The hidden columns are kept in the address (?hiddenColumns=a,b) so a reload or a shared link keeps them.
    function initColumnToggle({ tableId, containerId, columnSettings }) {
        const table = document.getElementById(tableId);
        const container = document.getElementById(containerId);
        if (!table || !container) return;

        const headerRow = table.querySelector('thead tr');
        const dataRows = Array.from(table.querySelectorAll('tbody tr'));

        const urlParams = new URLSearchParams(window.location.search);
        const hiddenColumnsParam = urlParams.get('hiddenColumns');
        const hiddenSet = hiddenColumnsParam ? new Set(hiddenColumnsParam.split(',')) : null;

        function cellsOf(index) {
            return dataRows
                .map(row => row.children[index])
                .filter(td => td && !td.hasAttribute('colspan'));
        }

        columnSettings.forEach((col, index) => {
            let isVisible = col.visible;
            if (hiddenSet !== null) isVisible = !hiddenSet.has(col.key);

            const div = document.createElement('div');
            div.className = 'px-3 py-1';
            div.style.cursor = 'pointer';
            div.style.userSelect = 'none';
            div.innerHTML = `
                <div class="d-flex align-items-center">
                    <span class="column-checkbox" data-index="${index}"
                        style="display: inline-block; width: 18px; height: 18px; border: 2px solid #adb5bd; border-radius: 4px; margin-right: 8px; text-align: center; line-height: 16px; font-size: 12px; transition: 0.2s background;
                        ${isVisible ? 'background-color: #007bff; border-color: #007bff;' : ''}">
                        ${isVisible ? '<i class="fas fa-check" style="color: white;"></i>' : ''}
                    </span>
                    <span style="font-size: 0.95rem; color: #212529;"></span>
                </div>`;
            div.querySelector('span:last-child').textContent = col.label;

            const checkboxSpan = div.querySelector('.column-checkbox');
            const th = headerRow.children[index];
            const tds = cellsOf(index);

            if (!isVisible) {
                if (th) th.style.display = 'none';
                tds.forEach(td => td.style.display = 'none');
            }

            div.addEventListener('click', e => {
                e.stopPropagation();
                const wasVisible = checkboxSpan.innerHTML.trim() !== '';
                const newVisible = !wasVisible;

                if (newVisible) {
                    checkboxSpan.style.backgroundColor = '#007bff';
                    checkboxSpan.style.borderColor = '#007bff';
                    checkboxSpan.innerHTML = '<i class="fas fa-check" style="color: white;"></i>';
                    if (th) th.style.display = '';
                    tds.forEach(td => td.style.display = '');
                } else {
                    checkboxSpan.style.backgroundColor = '';
                    checkboxSpan.innerHTML = '';
                    if (th) th.style.display = 'none';
                    tds.forEach(td => td.style.display = 'none');
                }

                const currentlyHidden = [];
                columnSettings.forEach((c, idx) => {
                    const thEl = headerRow.children[idx];
                    if (thEl && thEl.style.display === 'none') currentlyHidden.push(c.key);
                });

                // "none" means every column is shown (an empty value would fall back to the defaults)
                const hiddenValue = currentlyHidden.length > 0 ? currentlyHidden.join(',') : 'none';

                const url = new URL(window.location);
                url.searchParams.set('hiddenColumns', hiddenValue);
                window.history.replaceState({}, '', url);

                // Keep the choice when moving to another page
                document.querySelectorAll('.pagination .page-item a.page-link').forEach(link => {
                    try {
                        const linkUrl = new URL(link.href, window.location.origin);
                        linkUrl.searchParams.set('hiddenColumns', hiddenValue);
                        link.href = linkUrl.toString();
                    } catch (err) { /* ignore links that are not URLs */ }
                });
            });

            container.appendChild(div);
        });
    }

    // ── WHOLE LIST PAGE ────────────────────────────────────────────────
    // Wires up a list page the way initIndexPage() does in work-report-index.js:
    // header sorting, show / hide columns, Apply / Reset filters, the server-side sort menu,
    // the page-size selector, clickable rows and export links that carry the current filters.
    //
    // config:
    //   basePath        page address, e.g. '/incident'
    //   tableId         id of the <table>
    //   filterMenuId    id of the filter drop-down (filter fields are looked up inside it only,
    //                   because the add / edit forms on the same page use the same field names)
    //   filterFields    names of the filter inputs; they are also the query string parameters
    //   columnSettings  [{ key, label, visible }] in the same order as the <th> cells
    //   sortFieldLabels { sortName: 'Label' } for the sort menu (names the server accepts)
    //   rowAttribute    attribute on each row holding its code, e.g. 'data-incident-code'
    //   rowBasePath     where a clicked row goes: rowBasePath + '/' + code (default basePath)
    //   datetimeFields / dateFields   columns to sort as dates when a header is clicked
    //   exportLinkIds   ids of export links that should receive the current filters and sort
    function initListPage(config) {
        const {
            basePath, tableId, filterMenuId, filterFields, columnSettings, sortFieldLabels, rowAttribute,
            rowBasePath = basePath, datetimeFields = [], dateFields = [], exportLinkIds = []
        } = config;
        const sortParamFields = [...filterFields, 'size'];

        function run() {
            // Table enhancements
            enableClientSideSorting({ tableId, datetimeFields, dateFields });
            initColumnToggle({ tableId, containerId: 'column-toggle-list', columnSettings });

            // Open a record by clicking its row
            document.querySelectorAll('.clickable-row').forEach(row => {
                row.addEventListener('click', function (e) {
                    if (e.target.closest('a, button')) return;
                    const code = this.getAttribute(rowAttribute);
                    if (code) window.location.href = rowBasePath + '/' + encodeURIComponent(code);
                });
            });

            // Apply / Reset filters
            const filterDropdown = document.getElementById('filterDropdownBtn');
            filterDropdown?.nextElementSibling?.addEventListener('click', e => e.stopPropagation());

            document.getElementById('applyFilters')?.addEventListener('click', () => {
                window.location.href = buildFilterUrl({ basePath, filterFields, containerId: filterMenuId });
            });

            document.getElementById('resetFilters')?.addEventListener('click', () => {
                // Clear the filters and the sort; keep the page size and the chosen columns
                const params = new URLSearchParams();
                const size = findGetParam('size');
                const hidden = findGetParam('hiddenColumns');
                if (size) params.set('size', size);
                if (hidden) params.set('hiddenColumns', hidden);
                const query = params.toString();
                window.location.href = basePath + (query ? '?' + query : '');
            });

            // Press Enter in the search box to apply
            document.querySelector(`#${filterMenuId} [name="keyword"]`)?.addEventListener('keydown', e => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    document.getElementById('applyFilters')?.click();
                }
            });

            // Sorting menu (server side: reload with sortBy and asc, filters are kept)
            const container = document.getElementById('sort-column-container');
            if (container) {
                const currentSortBy = document.getElementById('sortByInput')?.value;
                for (const [field, label] of Object.entries(sortFieldLabels)) {
                    const a = document.createElement('a');
                    a.className = 'dropdown-item';
                    a.textContent = label;
                    a.href = buildSortUrl({ basePath, paramFields: sortParamFields, sortByValue: field });
                    if (field === currentSortBy) a.classList.add('active');
                    container.appendChild(a);
                }
            }

            document.querySelectorAll('.dropdown-item[data-asc]').forEach(item => {
                item.addEventListener('click', function (e) {
                    e.preventDefault();
                    const input = document.getElementById('ascInput');
                    if (input) input.value = this.dataset.asc;

                    document.querySelectorAll('.dropdown-item[data-asc]').forEach(el => el.classList.remove('active'));
                    this.classList.add('active');

                    const sortBy = document.getElementById('sortByInput')?.value || Object.keys(sortFieldLabels)[0];
                    window.location.href = buildSortUrl({ basePath, paramFields: sortParamFields, sortByValue: sortBy });
                });
            });

            // Page size control: back to the first page, everything else is kept
            document.getElementById('pageSizeSelect')?.addEventListener('change', function () {
                const url = new URL(window.location);
                url.searchParams.set('size', this.value);
                url.searchParams.set('page', '0');
                window.location.href = url.toString();
            });

            // Export what is on screen: pass the current filters and sort to the export links
            exportLinkIds.forEach(id => {
                const link = document.getElementById(id);
                if (!link) return;
                const url = new URL(link.href, window.location.origin);
                const current = new URLSearchParams(window.location.search);
                [...filterFields, 'sortBy', 'asc'].forEach(k => {
                    if (current.get(k)) url.searchParams.set(k, current.get(k));
                });
                link.href = url.pathname + url.search;
            });
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', run);
        } else {
            run();
        }
    }

    return {
        findGetParam,
        sortTable,
        enableClientSideSorting,
        buildFilterUrl,
        buildSortUrl,
        initColumnToggle,
        initListPage
    };
})();

window.IndexModule = IndexModule;
