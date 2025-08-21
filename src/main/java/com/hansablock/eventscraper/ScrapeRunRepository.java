package com.hansablock.eventscraper;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
  List<ScrapeRun> findAllByOrderByStartedAtDesc(Pageable pageable);

  ScrapeRun findTopByScraperNameOrderByStartedAtDesc(String scraperName);
}
