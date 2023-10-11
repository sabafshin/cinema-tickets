package uk.gov.dwp.uc.pairtest.domain;

import java.util.Map;

public class TicketTypePrices {
    private static final Map<TicketTypeRequest.Type, Integer> ticketPrices = Map.of(
            TicketTypeRequest.Type.ADULT, 20,
            TicketTypeRequest.Type.CHILD, 10,
            TicketTypeRequest.Type.INFANT, 0);

    public static int getPrice(final TicketTypeRequest.Type type) {
        return ticketPrices.getOrDefault(type, 9999);
    }
}
