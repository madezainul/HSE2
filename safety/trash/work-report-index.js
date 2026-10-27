(function () {

    // =============================================================================
    // CONFIGURATION: Shared constants for table, filters, sorting, export, etc.
    // =============================================================================

    const TABLE_ID = 'work-report-table';
    const BASE_PATH = '/work-reports';
    const DATETIME_FIELDS = ['reportDate', 'startTime', 'stopTime', 'createdAt', 'updatedAt'];
    const FILTER_FIELDS = ['keyword', 'reportDateFrom', 'reportDateTo', 'group', 'field', 'state', 'equipmentCode', 'technicianEmpId', 'zoneCode'];
    const SORT_PARAM_FIELDS = ['keyword', 'reportDateFrom', 'reportDateTo', 'group', 'field', 'state', 'equipmentCode', 'technicianEmpId', 'zoneCode', 'size'];

    const EXPORT_CONFIG = {
        singleIdFields: ['supervisor', 'area', 'issue', 'workOrder'],
        multiIdFields: ['technicians', 'equipments']
    };

    const COLUMN_SETTINGS = [
        { key: "code", label: "Code", visible: false },
        { key: "reportDate", label: "Report Date", visible: true },
        { key: "shift", label: "Shift", visible: false },
        { key: "area", label: "Area", visible: false },
        { key: "equipments", label: "Equipments", visible: true },
        { key: "category", label: "Category", visible: false },
        { key: "problem", label: "Problem", visible: true },
        { key: "solution", label: "Solution", visible: true },
        { key: "startTime", label: "Start Time", visible: false },
        { key: "stopTime", label: "Stop Time", visible: true },
        { key: "totalTimeMinutes", label: "Total Time", visible: true },
        { key: "technicians", label: "Technicians", visible: true },
        { key: "supervisor", label: "Supervisor", visible: false },
        { key: "status", label: "Status", visible: false },
        { key: "scope", label: "Scope", visible: false },
        { key: "issue", label: "Issue Code", visible: false },
        { key: "workOrder", label: "Work Order", visible: false },
        { key: "workType", label: "Work Type", visible: false },
        { key: "remark", label: "Remark", visible: false },
        { key: "createdAt", label: "Creation Time", visible: false },
        { key: "updatedAt", label: "Update Time", visible: false },
        { key: "createdBy", label: "Created By", visible: true },
        { key: "updatedBy", label: "Updated By", visible: false }
    ];

    const SORT_FIELD_LABELS = {
        code: 'Code',
        reportDate: 'Report Date',
        shift: 'Shift',
        area: 'Area',
        equipments: 'Equipment(s)',
        category: 'Category',
        problem: 'Problem',
        solution: 'Solution',
        startTime: 'Start Time',
        stopTime: 'Stop Time',
        totalTimeMinutes: 'Total Time',
        technicians: 'Technician(s)',
        supervisor: 'Supervisor',
        status: 'Status',
        scope: 'Scope',
        issue: 'Issue Code',
        workOrder: 'Work Order',
        workType: 'Work Type',
        remark: 'Remark',
        createdAt: 'Creation Time',
        updatedAt: 'Update Time',
        createdBy: 'Created By',
        updatedBy: 'Updated By'
    };

    const EXCEL_TO_DTO_MAPPER = {
        "Code": "code",
        "Report Date": "reportDate",
        "Shift": "shift",
        "Area": "area",
        "Equipment(s)": "equipments",
        "Category": "category",
        "Problem": "problem",
        "Solution": "solution",
        "Start Time": "startTime",
        "Stop Time": "stopTime",
        "Total Time": "totalTimeMinutes",
        "Technician(s)": "technicians",
        "Supervisor": "supervisor",
        "Status": "status",
        "Scope": "scope",
        "Issue Code": "issue",
        "Work Order": "workOrder",
        "Work Type": "workType",
        "Remark": "remark"
    };

    // =============================================================================
    // ROW HANDLERS: For edit/delete actions on table rows
    // =============================================================================

    function buildWorkReportDeleteUrl(id) {
        const redirectUrl = encodeURIComponent(window.location.href);
        return `/work-reports/delete/${encodeURIComponent(id)}?redirectUrl=${redirectUrl}`;
    }

    function mapWorkReportFields(row, id) {
        const setVal = (sel, val) => { const el = document.querySelector(sel); if (el) el.value = val; };
        const getText = field => row.querySelector(`[data-field="${field}"]`)?.textContent.trim() || '';
        const getEnumValue = field => row.querySelector(`[data-field="${field}"] .badge`)?.dataset.enumValue || '';
        const getBadgeText = field => row.querySelector(`[data-field="${field}"] .badge`)?.textContent.trim().toUpperCase() || '';
        const getDatetime = field => row.querySelector(`[data-field="${field}"]`)?.dataset.fullDatetime || '';
        const getCode = field => row.querySelector(`[data-field="${field}"]`)?.dataset.code || '';
        const getEmpId = field => row.querySelector(`[data-field="${field}"]`)?.dataset.employeeId || '';


        // Basic & enum fields
        setVal('#editId', id);
        setVal('#editReportDate', IndexModule.toIsoDate(getText('reportDate')));
        setVal('#editShift', getText('shift'));
        setVal('#editCategory', getEnumValue('category'));
        setVal('#editStatus', getBadgeText('status'));
        setVal('#editScope', getEnumValue('scope'));

        // Text content fields
        setVal('#editProblem', getText('problem'));
        setVal('#editSolution', getText('solution'));
        setVal('#editWorkType', getText('workType'));
        setVal('#editRemark', getText('remark'));

        // Datetime fields
        setVal('#editStartTime', IndexModule.toIsoDateTime(getDatetime('startTime')));
        setVal('#editStopTime', IndexModule.toIsoDateTime(getDatetime('stopTime')));

        // Total time (from text like "120 min")
        const totalTimeText = getText('totalTimeMinutes');
        const minutes = totalTimeText.match(/\d+/);
        setVal('#editTotalTime', minutes ? minutes[0] : '');
        if (typeof calculateEditTotalTime === 'function') calculateEditTotalTime();

        // Area, Equipment, Supervisor (display + code/id)
        const areaName = getText('area');
        const areaCode = getCode('area');
        setVal('#editAreaInput', areaName && areaCode ? `${areaName} (${areaCode})` : '');
        setVal('#editAreaHidden', areaCode);

        const equipName = getText('equipment');
        const equipCode = getCode('equipment');
        setVal('#editEquipmentInput', equipName && equipCode ? `${equipName} (${equipCode})` : '');
        setVal('#editEquipmentHidden', equipCode);

        const supName = getText('supervisor');
        const supId = getEmpId('supervisor');
        setVal('#editSupervisorInput', supName && supId ? `${supName} (${supId})` : '');
        setVal('#editSupervisorHidden', supId);

        const issueName = getText('issue');
        const issueCode = getCode('issue');
        setVal('#editIssueInput', issueName && issueCode ? `${issueName} (${issueCode})` : '');
        setVal('#editIssueHidden', issueCode);

        const woCode = getCode('workOrder');
        setVal('#editWorkOrderTagSearch', woCode);

        const select = document.getElementById('editWorkOrderSelect');
        if (select && woCode) {
            select.innerHTML = `<option value="${woCode}" selected>${woCode}</option>`;
        }


        // Technicians (from badge elements)
        const equipCell = row.querySelector('[data-field="equipments"]');
        IndexModule.restoreTagInputFromTable({
            cell: equipCell,
            tagListId: 'editEquipmentTagList',
            hiddenSelectId: 'editEquipmentSelect'
        });

        // Technicians (from badge elements)
        const techCell = row.querySelector('[data-field="technicians"]');
        IndexModule.restoreTagInputFromTable({
            cell: techCell,
            tagListId: 'editTechnicianTagList',
            hiddenSelectId: 'editTechnicianSelect'
        });

        IndexModule.loadExistingLinkedReports({
            row,
            tableId: 'editLinkedReports',
            containerId: 'editLinkedReportsHiddenInputs'
        });
    }

    // =============================================================================
    // INDEX PAGE: Main table UI, filters, sorting, export, etc.
    // =============================================================================

    function initIndexPage() {
        // Table enhancements
        IndexModule.enableClientSideSorting({ tableId: TABLE_ID, datetimeFields: DATETIME_FIELDS });
        IndexModule.initColumnToggle({ tableId: TABLE_ID, containerId: 'column-toggle-list', columnSettings: COLUMN_SETTINGS });
        IndexModule.initDeleteHandlers({ deleteUrlBuilder: buildWorkReportDeleteUrl });
        IndexModule.initEditHandlers({ fieldMapper: mapWorkReportFields, modalId: 'editRowModal', buttonSelector: '.edit-row' });

        // Apply / Reset filters
        const filterDropdown = document.getElementById('filterDropdownBtn');
        const dropdownMenu = filterDropdown.nextElementSibling;
        dropdownMenu.addEventListener('click', e => e.stopPropagation());

        document.getElementById('applyFilters')?.addEventListener('click', () => {
            window.location.href = IndexModule.buildFilterUrl({
                basePath: BASE_PATH,
                filterFields: FILTER_FIELDS,
                multiValueFields: {
                    equipmentCode: 'filterEquipmentTagList',
                    technicianEmpId: 'filterTechnicianTagList',
                    zoneCode: 'filterZoneTagList',
                    group: 'filterCategoryTagList'
                }
            });
        });

        document.getElementById('resetFilters')?.addEventListener('click', () => {
            FILTER_FIELDS.forEach(name => {
                const el = document.querySelector(`[name="${name}"]`);
                if (el) el.value = '';
            });
            const tagList = document.getElementById('filterEquipmentTagList');
            if (tagList) tagList.innerHTML = '';
            const equipmentSelect = document.getElementById('filterEquipmentSelect');
            if (equipmentSelect) equipmentSelect.innerHTML = '';
            const searchInput = document.getElementById('filterEquipmentTagSearch');
            if (searchInput) searchInput.value = '';

            window.location.href = IndexModule.buildFilterUrl({ basePath: BASE_PATH, filterFields: FILTER_FIELDS });
        });

        // Sorting UI
        const container = document.getElementById('sort-column-container');
        if (container) {
            for (const [field, label] of Object.entries(SORT_FIELD_LABELS)) {
                const a = document.createElement('a');
                a.className = 'dropdown-item';
                a.textContent = label;
                a.href = IndexModule.buildSortUrl({ basePath: BASE_PATH, paramFields: SORT_PARAM_FIELDS, sortByValue: field });
                if (field === document.getElementById('sortByInput')?.value) a.classList.add('active');
                container.appendChild(a);
            }
        }

        document.querySelectorAll('.dropdown-item[data-asc]').forEach(item => {
            item.addEventListener('click', function (e) {
                e.preventDefault();
                const asc = this.dataset.asc;
                const input = document.getElementById('ascInput');
                if (input) input.value = asc;

                document.querySelectorAll('.dropdown-item[data-asc]').forEach(el => el.classList.remove('active'));
                this.classList.add('active');

                const sortBy = document.getElementById('sortByInput')?.value || 'code';
                window.location.href = IndexModule.buildSortUrl({ basePath: BASE_PATH, paramFields: SORT_PARAM_FIELDS, sortByValue: sortBy });
            });
        });

        // Page size control
        document.getElementById('pageSizeSelect')?.addEventListener('change', function () {
            const url = new URL(window.location);
            url.searchParams.set('size', this.value);
            const hidden = IndexModule.findGetParam('hiddenColumns');
            if (hidden) url.searchParams.set('hiddenColumns', hidden);
            url.pathname = BASE_PATH;
            window.location.href = url.toString();
        });

        // Import / Export
        new ImportWizard({
            likelyFields: Object.keys(EXCEL_TO_DTO_MAPPER),
            fieldMapping: EXCEL_TO_DTO_MAPPER,
            formAction: '/work-reports/import',
            onError: (err) => alert("Import Error: " + err.message)
        });

        const exportWizard = new ExportWizard(TABLE_ID, EXPORT_CONFIG);
        document.getElementById('exportExcel')?.addEventListener('click', e => {
            e.preventDefault(); exportWizard?.exportToExcel('Work Reports List');
        });
        document.getElementById('exportPdf')?.addEventListener('click', e => {
            e.preventDefault(); exportWizard?.exportToPdf('Work Reports List');
        });
    }

    // =============================================================================
    // MODAL UI: Add/Edit form enhancements (live search, duration, etc.)
    // =============================================================================

    function initModals() {

        function setupAreaOrEquipmentValidation() {
            ['formAdd', 'formEdit'].forEach(formId => {
                const form = document.getElementById(formId);
                if (!form) return;

                const helpText = form.querySelector('#areaOrEquipmentHelp');
                if (!helpText) return;

                const isEdit = formId === 'formEdit';
                const areaHiddenId = isEdit ? 'editAreaHidden' : 'areaHidden';
                const equipmentSelectId = isEdit ? 'editEquipmentSelect' : 'equipmentSelect';
                const technicianSelectId = isEdit ? 'editTechnicianSelect' : 'technicianSelect';

                const validate = () => {
                    const areaCode = document.getElementById(areaHiddenId)?.value?.trim();
                    const equipSelect = document.getElementById(equipmentSelectId);
                    const techSelect = document.getElementById(technicianSelectId);

                    const hasEquipment = equipSelect && equipSelect.selectedOptions.length > 0;
                    const hasTechnician = techSelect && techSelect.selectedOptions.length > 0;

                    const areaOrEquipmentValid = !!areaCode || hasEquipment;
                    const technicianValid = hasTechnician;

                    const isValid = areaOrEquipmentValid && technicianValid;

                    // Update help text: only show red/bold if area/equipment is missing
                    if (areaOrEquipmentValid) {
                        helpText.style.color = '';
                        helpText.style.fontWeight = '';
                        helpText.classList.remove('text-danger');
                    } else {
                        helpText.style.color = '#dc3545';
                        helpText.style.fontWeight = 'bold';
                        helpText.classList.add('text-danger');
                    }

                    // Also prevent submit if technician missing (but don’t change help text — it’s about area/equipment)
                    return isValid;
                };

                form.addEventListener('submit', function (e) {
                    if (!validate()) {
                        e.preventDefault();
                        // Force focus/red on technician if missing (but keep help text for area/equipment)
                        const techSelect = document.getElementById(isEdit ? 'editTechnicianSelect' : 'technicianSelect');
                        if (techSelect && techSelect.selectedOptions.length === 0) {
                            // Optional: highlight technician field
                            const techInput = document.querySelector(isEdit ? '#editTechnicianInput' : '#technicianInput');
                            if (techInput) {
                                techInput.style.borderColor = '#dc3545';
                                techInput.style.boxShadow = '0 0 0 0.2rem rgba(220, 53, 69, 0.25)';
                                setTimeout(() => {
                                    techInput.style.borderColor = '';
                                    techInput.style.boxShadow = '';
                                }, 2000);
                            }
                        }
                        helpText.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }
                });

                // Re-validate on changes
                [areaHiddenId, equipmentSelectId, technicianSelectId].forEach(id => {
                    const el = document.getElementById(id);
                    el?.addEventListener('change', validate);
                });

                // Watch for dynamic tag changes
                [equipmentSelectId, technicianSelectId].forEach(id => {
                    const select = document.getElementById(id);
                    if (select) {
                        new MutationObserver(validate).observe(select, { childList: true });
                    }
                });

                validate(); // initial
            });
        }

        // Run after DOM load
        setupAreaOrEquipmentValidation();

        const equipmentDetailsCache = new Map();

        function syncEquipmentData() {
            // Get codes from visible tags (most reliable)
            const tags = document.querySelectorAll('#equipmentTagList .tag');
            const selectedCodes = Array.from(tags)
                .map(tag => tag.dataset.value)  // tag.dataset.value = equipment code
                .filter(code => equipmentDetailsCache.has(code));

            if (selectedCodes.length === 0) return;

            const selectedEqs = selectedCodes.map(code => equipmentDetailsCache.get(code));
            const firstEq = selectedEqs[0];

            // ✅ Area — only if empty
            const areaHidden = document.getElementById('areaHidden');
            if (!areaHidden.value) {
                document.getElementById('areaInput').value = `${firstEq.area?.name || '-'} (${firstEq.area?.code || '-'})`;
                areaHidden.value = firstEq.area?.code || '';
            }

            // ✅ Supervisor — only if empty
            const supHidden = document.getElementById('supervisorHidden');
            if (!supHidden.value) {
                document.getElementById('supervisorInput').value = `${firstEq.supervisor?.name || '-'} (${firstEq.supervisor?.employeeId || '-'})`;
                supHidden.value = firstEq.supervisor?.employeeId || '';
            }

            // ✅ Technicians — union
            const techSelect = document.getElementById('technicianSelect');
            const techTagList = document.getElementById('technicianTagList');
            if (techSelect && techTagList) {
                const allTechs = [];
                const seen = new Set();
                selectedEqs.forEach(eq => {
                    (eq.technicians || []).forEach(t => {
                        if (!seen.has(t.employeeId)) {
                            seen.add(t.employeeId);
                            allTechs.push(t);
                        }
                    });
                });

                techSelect.innerHTML = '';
                techTagList.innerHTML = '';
                allTechs.forEach(t => {
                    const opt = document.createElement('option');
                    opt.value = t.employeeId;
                    opt.selected = true;
                    techSelect.appendChild(opt);

                    const tag = document.createElement('div');
                    tag.className = 'tag';
                    tag.dataset.value = t.employeeId;
                    tag.innerHTML = `<span>${t.name} (${t.employeeId})</span><span class="remove-tag" onclick="window.removeTag(this, 'technicianSelect', 'technicianTagList')">×</span>`;
                    techTagList.appendChild(tag);
                });
            }

            // ✅ Warnings
            const warnDiv = document.getElementById('equipmentAreaWarning');
            if (warnDiv) warnDiv.remove();

            const mismatches = [];
            selectedEqs.forEach(eq => {
                if (eq.area?.code !== firstEq.area?.code) mismatches.push(`area: ${eq.code}`);
                if ((eq.supervisor?.employeeId || '') !== (firstEq.supervisor?.employeeId || '')) mismatches.push(`supervisor: ${eq.code}`);
            });

            if (mismatches.length) {
                const help = document.getElementById('areaOrEquipmentHelp');
                const div = document.createElement('div');
                div.id = 'equipmentAreaWarning';
                div.className = 'text-danger small mt-1';
                div.innerHTML = `<i class="fas fa-exclamation-triangle"></i> Mixed sources: ${mismatches.join(', ')}`;
                help.parentNode.insertBefore(div, help.nextSibling);
            }
        }

        IndexModule.datalistLoader({
            datalistId: 'equipmentOptions',
            apiUrl: '/api/equipments/dropdown',
            idField: 'code',
            setupCallbacks: [
                // ✅ Cache full equipment objects
                () => fetch('/api/equipments/dropdown')
                    .then(r => r.json())
                    .then(eqs => eqs.forEach(eq => eq.code && equipmentDetailsCache.set(eq.code, eq)))
                    .catch(console.error),

                // ✅ Wrap setupFilterTagInput to auto-sync after tags change
                () => {
                    const original = IndexModule.setupFilterTagInput;
                    IndexModule.setupFilterTagInput = function (config) {
                        const result = original.call(this, config);
                        if (config.selectId === 'equipmentSelect') {
                            // Observe tag list for changes (covers range/search/rm)
                            const tagList = document.getElementById(config.tagListId);
                            if (tagList) {
                                new MutationObserver(syncEquipmentData)
                                    .observe(tagList, { childList: true });
                            }
                        }
                        return result;
                    };
                },
                () => IndexModule.setupFilterInput({
                    inputId: 'equipmentInput',
                    hiddenId: 'equipmentHidden',
                    optionsContainerId: 'equipmentOptions',
                    idAttr: 'data-code'
                }),
                () => IndexModule.setupFilterInput({
                    inputId: 'editEquipmentInput',
                    hiddenId: 'editEquipmentHidden',
                    optionsContainerId: 'equipmentOptions',
                    idAttr: 'data-code'
                }),

                // it should be filterEquipmentInput
                () => IndexModule.setupFilterTagInput({
                    inputId: 'equipmentInput',
                    tagListId: 'equipmentTagList',
                    tagSearchId: 'equipmentTagSearch',
                    selectId: 'equipmentSelect',
                    optionsContainerId: 'equipmentOptions'
                }),
                () => IndexModule.setupFilterTagInput({
                    inputId: 'editEquipmentInput',
                    tagListId: 'editEquipmentTagList',
                    tagSearchId: 'editEquipmentTagSearch',
                    selectId: 'editEquipmentSelect',
                    optionsContainerId: 'equipmentOptions'
                }),
                () => IndexModule.setupFilterTagInput({
                    inputId: 'filterEquipmentInput',
                    tagListId: 'filterEquipmentTagList',
                    tagSearchId: 'filterEquipmentTagSearch',
                    selectId: 'filterEquipmentSelect',
                    optionsContainerId: 'equipmentOptions'
                }),
                () => IndexModule.restoreFilterTagInputFromUrl({
                    tagListId: 'filterEquipmentTagList',
                    selectId: 'filterEquipmentSelect',
                    dataSourceId: 'equipmentOptions',
                    paramName: 'equipmentCode',
                    idAttr: 'code'
                }),
                () => IndexModule.setupEquipmentRangeTab({
                    selectId: 'equipmentSelect',
                    tagListId: 'equipmentTagList',
                    optionsContainerId: 'equipmentOptions'
                }),
                () => IndexModule.setupEquipmentRangeTab({
                    tabSearchId: 'editTabSearch',
                    tabRangeId: 'editTabRange',
                    rangeContainerId: 'editRangeInputContainer',
                    rangeInputId: 'editRangeInput',
                    applyBtnId: 'editApplyRange',
                    statusId: 'editRangeStatus',
                    selectId: 'editEquipmentSelect',      // ← edit IDs
                    tagListId: 'editEquipmentTagList',
                    optionsContainerId: 'equipmentOptions'
                })
            ]
        });

        // Live search datalist loaders and setup
        IndexModule.datalistLoader({
            datalistId: 'areaOptions',
            apiUrl: '/api/areas/dropdown',
            idField: 'code',
            setupCallbacks: [

                // Single-select live search inputs
                () => IndexModule.setupFilterInput({
                    inputId: 'areaInput',
                    hiddenId: 'areaHidden',
                    optionsContainerId: 'areaOptions',
                    idAttr: 'data-code'
                }),
                () => IndexModule.setupFilterInput({
                    inputId: 'editAreaInput',
                    hiddenId: 'editAreaHidden',
                    optionsContainerId: 'areaOptions',
                    idAttr: 'data-code'
                })
            ]
        });



        IndexModule.datalistLoader({
            datalistId: 'userOptions',
            apiUrl: '/api/users/maintenance-dropdown',
            idField: 'employeeId',
            setupCallbacks: [
                () => IndexModule.setupFilterInput({
                    inputId: 'supervisorInput',
                    hiddenId: 'supervisorHidden',
                    optionsContainerId: 'userOptions',
                    idAttr: 'data-employee-id'
                }),
                () => IndexModule.setupFilterInput({
                    inputId: 'editSupervisorInput',
                    hiddenId: 'editSupervisorHidden',
                    optionsContainerId: 'userOptions',
                    idAttr: 'data-employee-id'
                }),

                // Multi-select live search inputs
                () => IndexModule.setupFilterTagInput({
                    inputId: 'technicianInput',
                    tagListId: 'technicianTagList',
                    tagSearchId: 'technicianTagSearch',
                    selectId: 'technicianSelect',
                    optionsContainerId: 'userOptions'
                }),
                () => IndexModule.setupFilterTagInput({
                    inputId: 'editTechnicianInput',
                    tagListId: 'editTechnicianTagList',
                    tagSearchId: 'editTechnicianTagSearch',
                    selectId: 'editTechnicianSelect',
                    optionsContainerId: 'userOptions'
                }),

                () => IndexModule.setupFilterTagInput({
                    inputId: 'filterTechnicianInput',
                    tagListId: 'filterTechnicianTagList',
                    tagSearchId: 'filterTechnicianTagSearch',
                    selectId: 'filterTechnicianSelect',
                    optionsContainerId: 'userOptions'
                }),
                () => IndexModule.restoreFilterTagInputFromUrl({
                    tagListId: 'filterTechnicianTagList',
                    selectId: 'filterTechnicianSelect',
                    dataSourceId: 'userOptions',
                    paramName: 'technicianEmpId',
                    idAttr: 'employeeId'
                }),
            ]
        });

        // Trigger on change
        document.getElementById('equipmentSelect')?.addEventListener('change', syncEquipmentData);

        IndexModule.datalistLoader({
            datalistId: 'zoneOptions',
            apiUrl: '/api/zones/dropdown',
            idField: 'code',
            setupCallbacks: [
                () => IndexModule.setupFilterTagInput({
                    inputId: 'filterZoneInput',
                    tagListId: 'filterZoneTagList',
                    tagSearchId: 'filterZoneTagSearch',
                    selectId: 'filterZoneSelect',
                    optionsContainerId: 'zoneOptions'
                }),
                () => IndexModule.restoreFilterTagInputFromUrl({
                    tagListId: 'filterZoneTagList',
                    selectId: 'filterZoneSelect',
                    dataSourceId: 'zoneOptions',
                    paramName: 'zoneCode',
                    idAttr: 'code'
                })
            ]
        });

        // Category filter - use datalistLoader like Equipment
        IndexModule.datalistLoader({
            datalistId: 'categoryOptions',
            apiUrl: '/api/work-reports/categories',
            idField: 'value',
            setupCallbacks: [
                () => IndexModule.setupFilterTagInput({
                    inputId: 'filterCategoryInput',
                    tagListId: 'filterCategoryTagList',
                    tagSearchId: 'filterCategoryTagSearch',
                    selectId: 'filterCategorySelect',
                    optionsContainerId: 'categoryOptions'
                }),
                () => IndexModule.restoreFilterTagInputFromUrl({
                    tagListId: 'filterCategoryTagList',
                    selectId: 'filterCategorySelect',
                    dataSourceId: 'categoryOptions',
                    paramName: 'group',
                    idAttr: 'value'
                })
            ]
        });

        IndexModule.datalistLoader({
            datalistId: 'issueOptions',
            apiUrl: '/api/issues/dropdown',
            idField: 'code',
            setupCallbacks: [

                // Single-select live search inputs
                () => IndexModule.setupFilterInput({
                    inputId: 'issueInput',
                    hiddenId: 'issueHidden',
                    optionsContainerId: 'issueOptions',
                    idAttr: 'data-code',
                    onSelection: (text, code, optionEl) => {
                        const name = text.split(' (')[0];
                        const problemField = document.getElementById('problem');
                        if (problemField && name) {
                            problemField.value = problemField.value.trim()
                                ? `${problemField.value}\n${name}`
                                : name;
                        }
                    }
                }),
                () => IndexModule.setupFilterInput({
                    inputId: 'editIssueInput',
                    hiddenId: 'editIssueHidden',
                    optionsContainerId: 'issueOptions',
                    idAttr: 'data-code'
                })
            ]
        });

        // ✅ Work Order: auto-fill report fields
        IndexModule.searchCodeSingleInput({
            inputId: 'workOrderTagSearch',
            selectId: 'workOrderSelect',
            apiUrl: '/api/work-orders/search',
            onSelection: (code, wo) => {
                const dateInput = document.querySelector('input[name="reportDate"]');
                if (dateInput && wo.scheduledDate) {
                    dateInput.value = wo.scheduledDate;
                }

                const shiftSelect = document.querySelector('select[name="shift"]');
                if (shiftSelect && wo.shift) {
                    shiftSelect.value = wo.shift;
                }

                if (wo.areaCode && wo.areaName) {
                    const areaInput = document.getElementById('areaInput');
                    const areaHidden = document.getElementById('areaHidden');
                    if (areaInput && areaHidden) {
                        areaInput.value = `${wo.areaName} (${wo.areaCode})`;
                        areaHidden.value = wo.areaCode;
                    }
                }

                const eqSelect = document.getElementById('equipmentSelect');
                const eqTagList = document.getElementById('equipmentTagList');
                if (eqSelect && eqTagList && wo.equipmentCodes) {
                    eqSelect.innerHTML = '';
                    eqTagList.innerHTML = '';
                    const codes = wo.equipmentCodes.split(',');
                    const names = (wo.equipmentNames || '').split(',');

                    codes.forEach((code, i) => {
                        if (!code.trim()) return;
                        // Add to <select>
                        const opt = document.createElement('option');
                        opt.value = code;
                        opt.selected = true;
                        eqSelect.appendChild(opt);

                        // Add visible tag
                        const tag = document.createElement('div');
                        tag.className = 'tag';
                        tag.dataset.value = code;
                        const name = names[i] || code;
                        tag.innerHTML = `
                            <span>${name} (${code})</span>
                            <span class="remove-tag" onclick="removeTagFromTagList(this, 'equipmentSelect', 'equipmentTagList')">X</span>`;
                        eqTagList.appendChild(tag);
                    });
                }

                const techSelect = document.getElementById('technicianSelect');
                const techTagList = document.getElementById('technicianTagList');
                if (techSelect && techTagList && wo.assigneeEmpIds) {
                    techSelect.innerHTML = '';
                    techTagList.innerHTML = '';
                    const ids = wo.assigneeEmpIds.split(',');
                    const names = (wo.assigneeNames || '').split(',');

                    ids.forEach((id, i) => {
                        if (!id.trim()) return;
                        // Add to <select>
                        const opt = document.createElement('option');
                        opt.value = id;
                        opt.selected = true;
                        techSelect.appendChild(opt);

                        // Add visible tag
                        const tag = document.createElement('div');
                        tag.className = 'tag';
                        tag.dataset.value = id;
                        const name = names[i] || id;
                        tag.innerHTML = `
                            <span>${name} (${id})</span>
                            <span class="remove-tag" onclick="removeTagFromTagList(this, 'technicianSelect', 'technicianTagList')">X</span>`;
                        techTagList.appendChild(tag);
                    });
                }

                const prioMap = { 'LOW': 'LOW PRIORITY', 'MEDIUM': 'MEDIUM PRIORITY', 'HIGH': 'HIGH PRIORITY' };
                const prioSelect = document.querySelector('select[name="priority"]');
                if (prioSelect && wo.priority) {
                    prioSelect.value = prioMap[wo.priority] || 'MEDIUM PRIORITY';
                }

                // Replace the current description block with this:
                const desc = document.getElementById('problem');
                if (desc && wo.description && wo.description.trim()) {
                    desc.value = desc.value.trim()
                        ? `${desc.value}\n${wo.description}`
                        : wo.description;
                }
            }
        });

        IndexModule.searchCodeSingleInput({
            inputId: 'editWorkOrderTagSearch',
            selectId: 'editWorkOrderSelect',
            apiUrl: '/api/work-orders/search'
        });

        IndexModule.searchCodeTagInput({
            inputId: 'linkedReportInput',
            tagListId: 'linkedReportTagList',
            tagSearchId: 'linkedReportTagSearch',
            selectId: 'linkedReportSelect',
            apiUrl: '/api/work-reports/search',
        });

        IndexModule.searchCodeTagInput({
            inputId: 'editLinkedReportInput',
            tagListId: 'editLinkedReportTagList',
            tagSearchId: 'editLinkedReportTagSearch',
            selectId: 'editLinkedReportSelect',
            apiUrl: '/api/work-reports/search',
        });

        IndexModule.setupSuggestedReportsButton({
            buttonId: 'showPendingReportsBtn',
            containerId: 'suggestedLinkedReportsContainer',
            areaHiddenId: 'areaHidden',
            equipmentHiddenId: 'equipmentHidden',
            tableBodyId: 'suggestedLinkedReports',
            onAdd: (code, problem, reportDate) => {
                const tableBody = document.getElementById('linkedReports');
                if (!tableBody) return;

                if (tableBody.querySelector(`tr[data-code="${code}"]`)) return;

                const type = 'CAUSES';
                const isReverse = true;

                const setVal = (sel, val) => { const el = document.querySelector(sel); if (el) el.value = val; };
                setVal('#problem', problem);

                const row = document.createElement('tr');
                row.dataset.code = code;
                row.dataset.linkType = type;
                row.dataset.isReverse = isReverse;
                row.innerHTML = `
                <td>${reportDate || '-'}</td>
                <td>CAUSED BY</td>
                <td>${code}</td>
                <td>${problem || '-'}</td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-danger remove-linked-report">×</button>
                </td>`;
                tableBody.appendChild(row);
                IndexModule.syncLinkedReportsToHiddenInputs('linkedReports', 'linkedReportsHiddenInputs');
                IndexModule.toggleLinkedReportsTable('linkedReports', 'linkedReportsContainer');
            }
        });

        IndexModule.setupSuggestedReportsButton({
            buttonId: 'editShowPendingReportsBtn',
            containerId: 'editSuggestedLinkedReportsContainer',
            areaHiddenId: 'editAreaHidden',
            equipmentHiddenId: 'editEquipmentHidden',
            tableBodyId: 'editSuggestedLinkedReports',
            onAdd: (code, problem, reportDate) => {
                const tableBody = document.getElementById('editLinkedReports');
                if (!tableBody) return;

                if (tableBody.querySelector(`tr[data-code="${code}"]`)) return;

                const type = 'CAUSES';
                const isReverse = true;

                const row = document.createElement('tr');
                row.dataset.code = code;
                row.dataset.linkType = type;
                row.dataset.isReverse = isReverse;
                row.innerHTML = `
                <td>${reportDate || '-'}</td>
                <td>CAUSED BY</td>
                <td>${code}</td>
                <td>${problem || '-'}</td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-danger remove-linked-report">×</button>
                </td>`;
                tableBody.appendChild(row);
                IndexModule.syncLinkedReportsToHiddenInputs('editLinkedReports', 'editLinkedReportsHiddenInputs');
                IndexModule.toggleLinkedReportsTable('editLinkedReports', 'editLinkedReportsContainer');
            }
        });

        IndexModule.setupAddLinkedReportsButton({
            addButtonSelector: '#linkedReportsBtn',
            linkTypeSelectSelector: '.link-type-select',
            linkedReportSelectId: 'linkedReportSelect',
            linkedReportsTableId: 'linkedReports',
            tagListId: 'linkedReportTagList',
            selectId: 'linkedReportSelect',
            hiddenInputsContainerId: 'linkedReportsHiddenInputs'

        });

        IndexModule.setupAddLinkedReportsButton({
            addButtonSelector: '#editLinkedReportsBtn',
            linkTypeSelectSelector: '.link-type-select',
            linkedReportSelectId: 'editLinkedReportSelect',
            linkedReportsTableId: 'editLinkedReports',
            tagListId: 'editLinkedReportTagList',
            selectId: 'editLinkedReportSelect',
            hiddenInputsContainerId: 'editLinkedReportsHiddenInputs'
        });


        // Duration calculators
        IndexModule.setupDurationCalculator({
            startInputId: 'addStartTime',
            stopInputId: 'addStopTime',
            hiddenTotalId: 'addTotalTime',
            displayTotalId: 'addTotalTimeDisplay',
            modalId: 'addRowModal'
        });

        IndexModule.setupDurationCalculator({
            startInputId: 'editStartTime',
            stopInputId: 'editStopTime',
            hiddenTotalId: 'editTotalTime',
            displayTotalId: 'editTotalTimeDisplay',
            modalId: 'editRowModal'
        });

        // Initialize both forms
        IndexModule.setupTimeFields({
            formId: 'formAdd',
            dateSelector: 'input[name="reportDate"]',
            startSelector: 'input[name="startTime"]',
            stopSelector: 'input[name="stopTime"]',
            shiftSelector: 'select[name="shift"]'
        });

        IndexModule.setupTimeFields({
            formId: 'formEdit',
            dateSelector: '#editReportDate',
            startSelector: '#editStartTime',
            stopSelector: '#editStopTime',
            shiftSelector: '#editShift',
            helperId: 'editTimeHelper',
            insertAfterSelector: '[for="editStatus"]'
        });
    }

    // =============================================================================
    // INITIALIZATION
    // =============================================================================

    function init() {
        initIndexPage();
        initModals();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();