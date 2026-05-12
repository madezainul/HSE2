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

	// Extract observation code from URL (e.g., /observation/OBS000001)
	const pathParts = window.location.pathname.split('/');
	const observationCode = pathParts[2]; // /observation/OBS000001
	
	console.log('Observation Code from URL:', observationCode);
	console.log('Comment Content:', content);
	
	if (!observationCode) {
		alert('Error: Could not determine observation ID');
		return false;
	}

	const formData = new FormData();
	formData.append('content', content);

	const url = `/observation/${observationCode}/comments`;
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
			
			// Redirect to the observation detail page after a brief delay
			setTimeout(() => {
				const redirectUrl = `/observation/${observationCode}`;
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
 * Delete an observation with confirmation
 * @param {Event} event - The click event
 */
function deleteObservation(event) {
	event.preventDefault();
	
	const button = event.target.closest('button');
	const observationCode = button.getAttribute('data-observation-code');
	
	if (!observationCode) {
		alert('Error: Observation code not found');
		return;
	}
	
	// Show confirmation dialog
	if (!confirm('Are you sure you want to delete this observation? This action cannot be undone.')) {
		return;
	}
	
	console.log('Deleting observation:', observationCode);
	
	// Call DELETE API
	fetch(`/api/observation/${observationCode}`, {
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
			alert('Observation deleted successfully');
			// Redirect to observation list
			window.location = '/observation';
		} else {
			alert('Error: ' + (data.message || 'Failed to delete observation'));
		}
	})
	.catch(error => {
		console.error('Delete error:', error);
		alert('Error deleting observation: ' + error.message);
	});
}
