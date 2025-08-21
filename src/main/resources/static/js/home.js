document.addEventListener('DOMContentLoaded', function () {
  const form = document.getElementById('filterForm');
  const startHidden = document.getElementById('startDate');
  const endHidden = document.getElementById('endDate');
  const pickerInput = document.getElementById('dateRangePicker');
  const locationFilter = document.getElementById('locationFilter');

  const fp = window.flatpickr && flatpickr('#dateRangePicker', {
    mode: 'range',
    dateFormat: 'd.m.Y',
    locale: 'de',
    defaultDate: (function(){
      const s = pickerInput && pickerInput.dataset.start;
      const e = pickerInput && pickerInput.dataset.end;
      const out = [];
      if (s) out.push(s);
      if (e) out.push(e);
      return out;
    })(),
    onChange: function(selectedDates, dateStr, instance) {
      syncDatesAndSubmit(selectedDates);
    }
  });

  if (locationFilter) {
    locationFilter.addEventListener('change', function(){
      if (!form) return;
      form.submit();
    });
  }

  function syncDatesAndSubmit(selectedDates){
    if (!form) return;
    const fmt = (d) => {
      const dd = String(d.getDate()).padStart(2,'0');
      const mm = String(d.getMonth()+1).padStart(2,'0');
      const yyyy = d.getFullYear();
      return `${dd}.${mm}.${yyyy}`;
    };
    const start = selectedDates && selectedDates[0] ? fmt(selectedDates[0]) : '';
    const end = selectedDates && selectedDates[1] ? fmt(selectedDates[1]) : '';
    if (startHidden) startHidden.value = start;
    if (endHidden) endHidden.value = end;
    form.submit();
  }
});

