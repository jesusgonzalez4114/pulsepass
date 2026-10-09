package com.pulsepass.platform.service;

import com.pulsepass.platform.dto.request.PurchaseTicketRequest;
import com.pulsepass.platform.dto.response.TicketResponse;

import java.util.List;

public interface TicketService {
    TicketResponse purchase(PurchaseTicketRequest request);
    TicketResponse findByCode(String ticketCode);
    List<TicketResponse> findByUserEmail(String email);
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);
    TicketResponse cancel(String ticketCode);
    TicketResponse markAsUsed(String ticketCode);
}
