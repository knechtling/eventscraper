document.addEventListener('DOMContentLoaded', function () {
  const dateRangePicker = window.flatpickr && flatpickr('#dateRangePicker', {
    mode: 'range',
    dateFormat: 'd.m.Y',
    locale: 'de',
    onChange: filterEvents
  });

  const locationFilter = document.getElementById('locationFilter');
  if (locationFilter) {
    locationFilter.addEventListener('change', filterEvents);
  }

  function filterEvents() {
    if (!dateRangePicker) return;
    const selectedLocation = (locationFilter && locationFilter.value || '').toLowerCase();
    const [startDate, endDate] = dateRangePicker.selectedDates || [];

    document.querySelectorAll('.event-card').forEach(card => {
      const locationText = (card.getAttribute('data-location') || '').toLowerCase();
      const isoDate = card.getAttribute('data-date');
      let eventDate = null;
      if (isoDate) {
        const [y, m, d] = isoDate.split('-').map(Number);
        eventDate = new Date(y, m - 1, d);
      }

      const matchesLocation = !selectedLocation || locationText === selectedLocation;
      const matchesDate = (!startDate || (eventDate && eventDate >= startDate)) && (!endDate || (eventDate && eventDate <= endDate));

      card.style.display = ((!selectedLocation && !startDate && !endDate) || (matchesLocation && matchesDate)) ? '' : 'none';
    });
  }
});

