package com.hansablock.eventscraper;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
    List<ScrapeRun> findAllByOrderByStartedAtDesc(Pageable pageable);
    ScrapeRun findTopByScraperNameOrderByStartedAtDesc(String scraperName);
}
