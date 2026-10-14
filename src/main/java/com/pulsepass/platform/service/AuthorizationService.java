package com.pulsepass.platform.service;


public interface AuthorizationService {

    /** true si el principal autenticado es STAFF o ADMIN. */
    boolean hasStaffAccess();

    /** Propietario del ticket, o STAFF/ADMIN. Si el ticket no existe devuelve true para que el servicio responda 404. */
    boolean canViewTicket(String ticketCode);

    /** Propietario del ticket, o ADMIN. Si el ticket no existe devuelve true para que el servicio responda 404. */
    boolean canCancelTicket(String ticketCode);

    /** El correo debe pertenecer al principal autenticado. */
    boolean canPurchaseFor(String userEmail);

    /** El usuario consultado por correo es el propio principal, o el principal es ADMIN. */
    boolean canAccessUserByEmail(String email);

    /** El usuario consultado por username es el propio principal, o el principal es ADMIN. */
    boolean canAccessUserByUsername(String username);
}
