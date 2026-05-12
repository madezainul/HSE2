// Observation Detail Page - Comment and Image Zoom Functionality

/**
 * Submit a comment on the observation detail page
 * Handles form validation, AJAX submission, and page redirect
 */
function submitComment() {
	console.log('submitComment function called');
	
	const content = document.getElementById('commentContent').value.trim();
	
	if (!content) {
		alert('Please enter a comment');
		return false;
	}

	// Extract incident code from URL (e.g., /incident/INC000001)
	const pathParts = window.location.pathname.split('/');
	const incidentCode = pathParts[2]; // /incident/INC000001
	
	console.log('Incident Code from URL:', incidentCode);
	console.log('Comment Content:', content);
	
	if (!incidentCode) {
		alert('Error: Could not determine incident ID');
		return false;
	}

	const formData = new FormData();
	formData.append('content', content);

	const url = `/incident/${incidentCode}/comments`;
	console.log('Posting to:', url);

	fetch(url, {
		method: 'POST',
		body: formData
	})
	.then(response => {
		console.log('Response status:', response.status);
		return response.json();
	})
	.then(data => {
		console.log('Response data:', data);
		if (data.success) {
			// Clear form and show success message
			document.getElementById('commentContent').value = '';
			const messageSpan = document.getElementById('commentMessage');
			messageSpan.innerText = 'Comment posted successfully!';
			messageSpan.style.display = 'inline';
			
			// Redirect to the incident detail page after a brief delay
			setTimeout(() => {
				const redirectUrl = `/incident/${incidentCode}`;
				console.log('Redirecting to:', redirectUrl);
				window.location = redirectUrl;
			}, 500);
		} else {
			alert('Error posting comment: ' + (data.message || 'Unknown error'));
		}
	})
	.catch(error => {
		console.error('Fetch error:', error);
		alert('Error posting comment: ' + error.message);
	});
	
	return false;
}

/**
 * Open a fullscreen modal to display a zoomed image
 * @param {HTMLImageElement} img - The image element to zoom
 */
window.openZoomModal = function (img) {
	const src = img.dataset.src || img.src;
	if (!src || (!src.startsWith('data:') && !src.includes('/file/'))) return;
	document.getElementById('zoomedImage').src = src;
	new bootstrap.Modal(document.getElementById('imageZoomModal')).show();
};

/**
 * Delete an incident with confirmation
 * @param {Event} event - The click event
 */
function deleteIncident(event) {
	event.preventDefault();
	
	const button = event.target.closest('button');
	const incidentCode = button.getAttribute('data-incident-code');
	
	if (!incidentCode) {
		alert('Error: Incident code not found');
		return;
	}
	
	// Show confirmation dialog
	if (!confirm('Are you sure you want to delete this incident? This action cannot be undone.')) {
		return;
	}
	
	console.log('Deleting incident:', incidentCode);
	
	// Call DELETE API
	fetch(`/api/incident/${incidentCode}`, {
		method: 'DELETE'
	})
	.then(response => {
		console.log('Delete response status:', response.status);
		if (!response.ok) {
			throw new Error(`HTTP error! status: ${response.status}`);
		}
		return response.json();
	})
	.then(data => {
		console.log('Delete response data:', data);
		if (data.status === 'success') {
			alert('Incident deleted successfully');
			// Redirect to incident list
			window.location = '/incident';
		} else {
			alert('Error: ' + (data.message || 'Failed to delete incident'));
		}
	})
	.catch(error => {
		console.error('Delete error:', error);
		alert('Error deleting incident: ' + error.message);
	});
}
