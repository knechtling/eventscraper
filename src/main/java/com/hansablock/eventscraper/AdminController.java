package com.hansablock.eventscraper;

import com.hansablock.eventscraper.scraper.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final ScrapeRunRepository scrapeRunRepository;
    private final ScraperService scraperService;

    @Autowired
    public AdminController(ScrapeRunRepository scrapeRunRepository, ScraperService scraperService) {
        this.scrapeRunRepository = scrapeRunRepository;
        this.scraperService = scraperService;
    }

@GetMapping("/admin/scraper-health")
    public String scraperHealth(Model model) {
        var names = scraperService.getScraperNames();
        java.util.List<java.util.Map<String,Object>> statuses = new java.util.ArrayList<>();
        java.time.Instant now = java.time.Instant.now();
        for (String name : names) {
            ScrapeRun last = scrapeRunRepository.findTopByScraperNameOrderByStartedAtDesc(name);
            String status = "STALE";
            if (last != null) {
                boolean running = last.getFinishedAt() == null && last.getStartedAt() != null && java.time.Duration.between(last.getStartedAt(), now).toMinutes() < 15;
                if (running) {
                    status = "RUNNING";
                } else if (last.getErrors() > 0) {
                    status = "ERROR";
                } else if (last.getFinishedAt() != null && java.time.Duration.between(last.getFinishedAt(), now).toHours() <= 24) {
                    status = "OK";
                } else {
                    status = "STALE";
                }
            }
            java.util.Map<String,Object> row = new java.util.HashMap<>();
            row.put("name", name);
            row.put("status", status);
            row.put("last", last);
            statuses.add(row);
        }
        model.addAttribute("scrapers", statuses);
        model.addAttribute("runs", scrapeRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, 50)));
        return "scrapers";
    }

    // Backwards-compat alias for old link
    @GetMapping("/admin/scrapers")
    public String scraperHealthAlias() {
        return "redirect:/admin/scraper-health";
    }

    @GetMapping("/admin/scraper-health/run")
    public String runScrapers(@RequestParam(value = "name", required = false) String name, RedirectAttributes ra) {
        try {
            if (name == null || name.isBlank()) {
                scraperService.runAllNow();
                ra.addFlashAttribute("msg", "Triggered all scrapers");
            } else {
                scraperService.runOne(name);
                ra.addFlashAttribute("msg", "Triggered " + name);
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("msg", "Error: " + ex.getMessage());
        }
        return "redirect:/admin/scraper-health";
    }

    // Backwards-compat alias for old run link
    @GetMapping("/admin/scrapers/run")
    public String runScrapersAlias(@RequestParam(value = "name", required = false) String name) {
        if (name == null || name.isBlank()) {
            return "redirect:/admin/scraper-health/run";
        }
        return "redirect:/admin/scraper-health/run?name=" + org.springframework.web.util.UriUtils.encode(name, java.nio.charset.StandardCharsets.UTF_8);
    }
}
