package com.hansablock.eventscraper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventController.class)
class EventControllerFilterTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    EventService eventService;

    @MockBean
    EventRepository eventRepository; // needed for /event/details mapping in controller

    @Test
    void passes_filters_to_service_and_renders_model() throws Exception {
        Event e = new Event();
        e.setId(1L);
        e.setTitle("Rock Gala");
        e.setLocation("Scheune Dresden");
        e.setDate(LocalDate.now().plusDays(10));

        Mockito.when(eventService.searchWithFilters(anyString(), anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(e), (Pageable) inv.getArgument(4), 1));
        Mockito.when(eventService.getUniqueLocationsFromUpcoming()).thenReturn(List.of("Chemiefabrik", "Hanse 3", "Scheune Dresden"));

        mockMvc.perform(get("/")
                        .param("search", "rock")
                        .param("start", "01.09.2025")
                        .param("end", "30.09.2025")
                        .param("loc", "Scheune Dresden")
                        .param("page", "0")
                        .param("size", "24"))
                .andExpect(status().isOk())
                .andExpect(view().name("welcome"))
                .andExpect(model().attributeExists("eventsPage", "events", "locations", "search", "start", "end", "loc"))
                .andExpect(model().attribute("search", "rock"))
                .andExpect(model().attribute("loc", "Scheune Dresden"));

        Mockito.verify(eventService).searchWithFilters(eq("rock"), eq("01.09.2025"), eq("30.09.2025"), eq("Scheune Dresden"), any(Pageable.class));
    }
}
