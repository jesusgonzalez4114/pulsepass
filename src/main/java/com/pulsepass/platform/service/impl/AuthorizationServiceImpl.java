package com.pulsepass.platform.service.impl;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.Ticket;
import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.repository.TicketRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.security.SecurityPrincipal;
import com.pulsepass.platform.service.AuthorizationService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service("authorizationService")
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AuthorizationServiceImpl(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public boolean hasStaffAccess() {
        return currentPrincipal()
                .map(p -> p.hasRole(Role.STAFF) || p.hasRole(Role.ADMIN))
                .orElse(false);
    }

    @Override
    public boolean canViewTicket(String ticketCode) {
        Optional<SecurityPrincipal> principal = currentPrincipal();
        if (principal.isEmpty()) {
            return false;
        }
        SecurityPrincipal p = principal.get();
        if (p.hasRole(Role.STAFF) || p.hasRole(Role.ADMIN)) {
            return true;
        }
        return isOwnerOrTicketMissing(p, ticketCode);
    }

    @Override
    public boolean canCancelTicket(String ticketCode) {
        Optional<SecurityPrincipal> principal = currentPrincipal();
        if (principal.isEmpty()) {
            return false;
        }
        SecurityPrincipal p = principal.get();
        if (p.hasRole(Role.ADMIN)) {
            return true;
        }
        return isOwnerOrTicketMissing(p, ticketCode);
    }

    @Override
    public boolean canPurchaseFor(String userEmail) {
        return currentPrincipal()
                .map(p -> isSameUser(p, userRepository.findByEmailIgnoreCase(userEmail)))
                .orElse(false);
    }

    @Override
    public boolean canAccessUserByEmail(String email) {
        return currentPrincipal()
                .map(p -> p.hasRole(Role.ADMIN) || isSameUser(p, userRepository.findByEmailIgnoreCase(email)))
                .orElse(false);
    }

    @Override
    public boolean canAccessUserByUsername(String username) {
        return currentPrincipal()
                .map(p -> p.hasRole(Role.ADMIN) || isSameUser(p, userRepository.findByUsername(username)))
                .orElse(false);
    }

    private boolean isOwnerOrTicketMissing(SecurityPrincipal principal, String ticketCode) {
        Optional<Ticket> ticket = ticketRepository.findByTicketCode(ticketCode);
        return ticket.isEmpty()
                || principal.getUserId().equals(ticket.get().getUser().getId());
    }

    private boolean isSameUser(SecurityPrincipal principal, Optional<User> requested) {
        return requested.isPresent() && principal.getUserId().equals(requested.get().getId());
    }

    private Optional<SecurityPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }
}
