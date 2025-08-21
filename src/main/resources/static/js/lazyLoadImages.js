(function(){
  const items=document.querySelectorAll('.event-thumbnail');
  const io=new IntersectionObserver((entries)=>{
    entries.forEach(e=>{
      if(e.isIntersecting){
        const el=e.target;const src=el.getAttribute('data-src');
        if(src){el.style.backgroundImage=`url(${src})`;el.removeAttribute('data-src');}
        io.unobserve(el);
      }
    })
  },{rootMargin:'200px'});
  items.forEach(el=>io.observe(el));
})();

document.addEventListener('DOMContentLoaded', function () {
    // Select all thumbnails with a "data-src" attribute
    const thumbnails = document.querySelectorAll('.event-thumbnail[data-src]');

    function lazyLoadThumbnails(entries, observer) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const thumbnailDiv = entry.target;
                const imgUrl = thumbnailDiv.getAttribute('data-src');
                if (imgUrl) {
                    thumbnailDiv.style.backgroundImage = `url('${imgUrl}')`;
                    thumbnailDiv.removeAttribute('data-src');
                    observer.unobserve(thumbnailDiv); // Stop observing once the image is loaded
                }
            }
        });
    }

    // Set up IntersectionObserver for lazy loading
    const observer = new IntersectionObserver(lazyLoadThumbnails, {
        root: null, // Observe in the viewport
        rootMargin: '0px',
        threshold: 0.1 // Trigger when 10% of the element is visible
    });

    // Observe each thumbnail
    thumbnails.forEach(thumbnail => observer.observe(thumbnail));
});
