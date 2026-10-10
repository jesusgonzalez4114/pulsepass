package com.pulsepass.platform.controller;

import com.pulsepass.platform.domain.TicketStatus;
import com.pulsepass.platform.domain.TicketType;
import com.pulsepass.platform.dto.request.PurchaseTicketRequest;
import com.pulsepass.platform.dto.response.TicketResponse;
import com.pulsepass.platform.exception.BusinessRuleException;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.service.TicketService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private static final String VALID_BODY = """
            {
              "userEmail": "andrea@mail.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;

    private TicketResponse ticket(TicketStatus status) {
        return new TicketResponse(1L, "TCK-1001", TicketType.GENERAL, new BigDecimal("100000"),
                status, LocalDateTime.of(2026, 10, 9, 10, 0),
                "andrea@mail.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // TEST-CTRL-TKT-001
    @Test
    void shouldReturn201WhenPurchaseIsValid() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-1001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value("andrea@mail.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    // TEST-CTRL-TKT-002
    @Test
    void shouldReturn400WhenPurchaseRequestIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "userEmail": "no-es-email"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email format is invalid"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    void shouldReturn400WhenTicketTypeIsNotValid() throws Exception {
        String body = VALID_BODY.replace("GENERAL", "GRATIS");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verify(ticketService, never()).purchase(any());
    }

    // TEST-CTRL-TKT-003
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@mail.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: andrea@mail.com"));
    }

    // TEST-CTRL-TKT-004
    @Test
    void shouldReturn409WhenPurchaseViolatesBusinessRule() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet minimum age for this event"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet minimum age for this event"));
    }

    // TEST-CTRL-TKT-005
    @Test
    void shouldReturn200WhenTicketExists() throws Exception {
        when(ticketService.findByCode("TCK-1001")).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TCK-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-1001"));

        verify(ticketService).findByCode("TCK-1001");
    }

    @Test
    void shouldReturn404WhenTicketDoesNotExist() throws Exception {
        when(ticketService.findByCode("TCK-000"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: TCK-000"));

        mockMvc.perform(get("/api/tickets/TCK-000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: TCK-000"));
    }

    // TEST-CTRL-TKT-006
    @Test
    void shouldReturn200WithTicketsOfUser() throws Exception {
        when(ticketService.findByUserEmail("andrea@mail.com")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@mail.com"));

        verify(ticketService).findByUserEmail("andrea@mail.com");
    }

    // TEST-CTRL-TKT-007
    @Test
    void shouldReturn200WithPaidTicketsOfEvent() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    // TEST-CTRL-TKT-008
    @Test
    void shouldReturn200WhenTicketIsCancelled() throws Exception {
        when(ticketService.cancel("TCK-1001")).thenReturn(ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TCK-1001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-1001");
    }

    // TEST-CTRL-TKT-009
    @Test
    void shouldReturn409WhenCancelIsNotAllowed() throws Exception {
        when(ticketService.cancel("TCK-1001"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be cancelled: TCK-1001"));

        mockMvc.perform(patch("/api/tickets/TCK-1001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be cancelled: TCK-1001"));
    }

    // TEST-CTRL-TKT-010
    @Test
    void shouldReturn200WhenTicketIsMarkedAsUsed() throws Exception {
        when(ticketService.markAsUsed("TCK-1001")).thenReturn(ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TCK-1001/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-1001");
    }

    // TEST-CTRL-TKT-011
    @Test
    void shouldReturn409WhenUseIsNotAllowed() throws Exception {
        when(ticketService.markAsUsed("TCK-1001"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be marked as used: TCK-1001"));

        mockMvc.perform(patch("/api/tickets/TCK-1001/use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be marked as used: TCK-1001"));
    }
}
