/**
 * Toolbox meeting list page: filter, sort and show / hide columns.
 * The behaviour lives in IndexModule.initListPage (index-module.js), which follows
 * initIndexPage() in work-report-index.js. This file only holds this page's settings.
 * (The Export button on this page is not connected yet, so no export links are passed.)
 */
(function () {
    IndexModule.initListPage({
        basePath: '/training/toolbox',
        tableId: 'toolbox-table',
        filterMenuId: 'toolboxFilterMenu',
        rowAttribute: 'data-toolbox-code',
        dateFields: ['meetingDate'],
        datetimeFields: ['createdAt', 'updatedAt'],

        // Names of the filter inputs = query string parameters read by ToolboxController.toolboxPage
        filterFields: ['keyword', 'meetingDateFrom', 'meetingDateTo', 'status'],

        // Same order as the <th> cells in toolbox/index.html
        columnSettings: [
            { key: 'code', label: 'Code', visible: true },
            { key: 'location', label: 'Location', visible: true },
            { key: 'meetingDate', label: 'Date', visible: true },
            { key: 'timeStarted', label: 'Time Started', visible: true },
            { key: 'timeEnd', label: 'Time End', visible: true },
            { key: 'supervisor', label: 'Supervisor', visible: true },
            { key: 'status', label: 'Status', visible: true },
            { key: 'supervisorEmployeeId', label: 'Supervisor ID', visible: false },
            { key: 'notes', label: 'Notes', visible: false },
            { key: 'createdAt', label: 'Creation Time', visible: false },
            { key: 'updatedAt', label: 'Update Time', visible: false }
        ],

        // Columns the server can sort by (must match ToolboxMeetingService.SORTABLE_FIELDS)
        sortFieldLabels: {
            code: 'Code',
            location: 'Location',
            meetingDate: 'Date',
            timeStarted: 'Time Started',
            timeEnd: 'Time End',
            supervisor: 'Supervisor',
            supervisorEmployeeId: 'Supervisor ID',
            status: 'Status',
            createdAt: 'Creation Time',
            updatedAt: 'Update Time'
        }
    });
})();
