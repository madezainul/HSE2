/**
 * Observation list page: filter, sort and show / hide columns.
 * The behaviour lives in IndexModule.initListPage (index-module.js), which follows
 * initIndexPage() in work-report-index.js. This file only holds this page's settings.
 */
(function () {
    IndexModule.initListPage({
        basePath: '/observation',
        tableId: 'observation-table',
        filterMenuId: 'observationFilterMenu',
        rowAttribute: 'data-observation-code',
        datetimeFields: ['observationDate', 'createdAt', 'updatedAt'],
        exportLinkIds: ['exportExcel', 'exportPdf'],

        // Names of the filter inputs = query string parameters read by ObservationController.index
        filterFields: ['keyword', 'observationDateFrom', 'observationDateTo', 'category', 'type', 'status', 'mitigation', 'areaCode'],

        // Same order as the <th> cells in observation/index.html
        columnSettings: [
            { key: 'code', label: 'Code', visible: false },
            { key: 'observationDate', label: 'Observation Date', visible: true },
            { key: 'area', label: 'Area', visible: true },
            { key: 'description', label: 'Description', visible: true },
            { key: 'category', label: 'Category', visible: true },
            { key: 'type', label: 'Type', visible: true },
            { key: 'status', label: 'Status', visible: true },
            { key: 'observer', label: 'Observer', visible: true },
            { key: 'responsible', label: 'Responsible', visible: false },
            { key: 'mitigation', label: 'Mitigation', visible: false },
            { key: 'correctiveAction', label: 'Corrective Action', visible: false },
            { key: 'remarks', label: 'Remarks', visible: false },
            { key: 'createdAt', label: 'Creation Time', visible: false },
            { key: 'updatedAt', label: 'Update Time', visible: false }
        ],

        // Columns the server can sort by (must match ObservationService.SORTABLE_FIELDS)
        sortFieldLabels: {
            // code: 'Code',
            observationDate: 'Observation Date',
            area: 'Area',
            description: 'Description',
            category: 'Category',
            type: 'Type',
            status: 'Status',
            mitigation: 'Mitigation',
            observer: 'Observer',
            responsible: 'Responsible',
            createdAt: 'Creation Time',
            updatedAt: 'Update Time'
        }
    });
})();
