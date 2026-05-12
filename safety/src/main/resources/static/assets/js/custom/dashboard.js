document.addEventListener('DOMContentLoaded', function () {
	var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

	var chartData = window.dashboardChartData || {};
	var incidentData = chartData.incident || {};
	var observationData = chartData.observation || {};

	// Incident Horizontal Bar Chart
	var incidentCtx = document.getElementById('incidentBarChart').getContext('2d');
	new Chart(incidentCtx, {
		type: 'bar',
		data: {
			labels: months,
			datasets: [
				{
					label: 'Near Miss',
					data: incidentData.nearMiss && incidentData.nearMiss.length ? incidentData.nearMiss : [12, 19, 14, 17, 22, 18, 25, 21, 16, 20, 14, 11],
					backgroundColor: 'rgba(243, 84, 93, 0.85)',
					borderRadius: 4,
					borderSkipped: false
				},
				{
					label: 'Accident',
					data: incidentData.accident && incidentData.accident.length ? incidentData.accident : [8, 11, 9, 13, 15, 10, 14, 12, 9, 11, 8, 7],
					backgroundColor: 'rgba(255, 165, 52, 0.85)',
					borderRadius: 4,
					borderSkipped: false
				},
				{
					label: 'Unsafe Act',
					data: incidentData.unsafeAct && incidentData.unsafeAct.length ? incidentData.unsafeAct : [3, 5, 2, 4, 6, 3, 5, 4, 2, 3, 1, 2],
					backgroundColor: 'rgba(119, 93, 208, 0.85)',
					borderRadius: 4,
					borderSkipped: false
				}
			]
		},
		options: {
			indexAxis: 'y',
			responsive: true,
			maintainAspectRatio: false,
			plugins: {
				legend: {
					position: 'bottom',
					labels: { padding: 16, font: { size: 12 } }
				},
				tooltip: { mode: 'index', intersect: false }
			},
			scales: {
				x: {
					beginAtZero: true,
					grid: { color: 'rgba(0,0,0,0.05)' },
					ticks: { font: { size: 11 }, stepSize: 5 }
				},
				y: {
					grid: { display: false },
					ticks: { font: { size: 11 } }
				}
			}
		}
	});

	// Observation Horizontal Bar Chart
	var obsCtx = document.getElementById('observationBarChart').getContext('2d');
	new Chart(obsCtx, {
		type: 'bar',
		data: {
			labels: months,
			datasets: [
				{
					label: 'Open',
					data: observationData.open && observationData.open.length ? observationData.open : [28, 35, 30, 42, 38, 45, 50, 44, 37, 40, 33, 28],
					backgroundColor: 'rgba(255, 165, 52, 0.85)',
					borderRadius: 4,
					borderSkipped: false
				},
				{
					label: 'Mitigated',
					data: observationData.mitigated && observationData.mitigated.length ? observationData.mitigated : [15, 20, 18, 22, 25, 19, 28, 24, 18, 21, 16, 13],
					backgroundColor: 'rgba(35, 197, 200, 0.85)',
					borderRadius: 4,
					borderSkipped: false
				},
				{
					label: 'Closed',
					data: observationData.closed && observationData.closed.length ? observationData.closed : [22, 28, 25, 30, 33, 29, 35, 31, 24, 28, 22, 18],
					backgroundColor: 'rgba(40, 167, 69, 0.85)',
					borderRadius: 4,
					borderSkipped: false
				}
			]
		},
		options: {
			indexAxis: 'y',
			responsive: true,
			maintainAspectRatio: false,
			plugins: {
				legend: {
					position: 'bottom',
					labels: { padding: 16, font: { size: 12 } }
				},
				tooltip: { mode: 'index', intersect: false }
			},
			scales: {
				x: {
					beginAtZero: true,
					grid: { color: 'rgba(0,0,0,0.05)' },
					ticks: { font: { size: 11 }, stepSize: 10 }
				},
				y: {
					grid: { display: false },
					ticks: { font: { size: 11 } }
				}
			}
		}
	});
});
