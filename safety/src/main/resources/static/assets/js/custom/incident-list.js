/**
 * Incident list page: filter, sort and show / hide columns.
 * The behaviour lives in IndexModule.initListPage (index-module.js), which follows
 * initIndexPage() in work-report-index.js. This file only holds this page's settings.
 */
(function () {
    IndexModule.initListPage({
        basePath: '/incident',
        tableId: 'incident-table',
        filterMenuId: 'incidentFilterMenu',
        rowAttribute: 'data-incident-code',
        datetimeFields: ['reportDate', 'createdAt', 'updatedAt'],
        exportLinkIds: ['exportExcel', 'exportPdf'],

        // Names of the filter inputs = query string parameters read by IncidentController.index
        filterFields: ['keyword', 'reportDateFrom', 'reportDateTo', 'type', 'severity', 'status', 'areaCode'],

        // Same order as the <th> cells in incident/index.html
        columnSettings: [
            { key: 'code', label: 'Code', visible: true },
            { key: 'reportDate', label: 'Report Date', visible: true },
            { key: 'area', label: 'Area', visible: true },
            { key: 'description', label: 'Description', visible: true },
            { key: 'type', label: 'Type', visible: true },
            { key: 'severity', label: 'Severity', visible: true },
            { key: 'status', label: 'Status', visible: true },
            { key: 'reportedBy', label: 'Reported By', visible: true },
            { key: 'involvedPersonName', label: 'Involved Person', visible: false },
            { key: 'witnesses', label: 'Witnesses', visible: false },
            { key: 'immediateAction', label: 'Immediate Action', visible: false },
            { key: 'correctiveAction', label: 'Corrective Action', visible: false },
            { key: 'medicalAttentionRequired', label: 'Medical Attention', visible: false },
            { key: 'createdAt', label: 'Creation Time', visible: false },
            { key: 'updatedAt', label: 'Update Time', visible: false }
        ],

        // Columns the server can sort by (must match IncidentService.SORTABLE_FIELDS)
        sortFieldLabels: {
            code: 'Code',
            reportDate: 'Report Date',
            area: 'Area',
            description: 'Description',
            type: 'Type',
            severity: 'Severity',
            status: 'Status',
            reportedBy: 'Reported By',
            involvedPersonName: 'Involved Person',
            createdAt: 'Creation Time',
            updatedAt: 'Update Time'
        }
    });
})();
