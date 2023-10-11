package uk.gov.dwp.uc.pairtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private static final Map<TicketTypeRequest.Type, Integer> ticketPrices = Map.of(
            TicketTypeRequest.Type.ADULT, 20,
            TicketTypeRequest.Type.CHILD, 10,
            TicketTypeRequest.Type.INFANT, 0);
    private final SeatReservationServiceImpl seatReservationService = new SeatReservationServiceImpl();
    private final TicketPaymentServiceImpl ticketPaymentService = new TicketPaymentServiceImpl();
    private Map<TicketTypeRequest.Type, Integer> ticketTypeCounts = new HashMap<>();

    @Override
    public void purchaseTickets(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        validatePurchase(accountId, ticketTypeRequests);
        reserveSeats(accountId, ticketTypeRequests);
        makePayment(accountId, ticketTypeRequests);
    }

    private void reserveSeats(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        final int totalSeatstoAllocate = this.ticketTypeCounts.entrySet().stream()
                .filter(entry -> entry.getKey() == TicketTypeRequest.Type.ADULT
                        || entry.getKey() == TicketTypeRequest.Type.CHILD)
                .mapToInt(Map.Entry::getValue)
                .sum();
        seatReservationService.reserveSeat(accountId, totalSeatstoAllocate);
    }

    private void makePayment(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        final AtomicInteger totalAmountToPay = new AtomicInteger(0);
        this.ticketTypeCounts.entrySet().stream()
                .filter(entry -> ticketPrices.containsKey(entry.getKey()))
                .forEach(entry -> totalAmountToPay.addAndGet(ticketPrices.get(entry.getKey()) * entry.getValue()));
        ticketPaymentService.makePayment(accountId, totalAmountToPay.get());
    }

    private void validatePurchase(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        final List<String> exceptionMessages = new ArrayList<>();
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Expected a valid account ID but got " + accountId);
        }
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("Expected at least one ticket type request");
        }
        for (int i = 0; i < ticketTypeRequests.length; i++) {
            final TicketTypeRequest ticketTypeRequest = ticketTypeRequests[i];
            if (ticketTypeRequest.getNoOfTickets() < 0) {
                exceptionMessages.add("Request #" + (i + 1) + " has invalid number of tickets: "
                        + ticketTypeRequest.getNoOfTickets());
            }
            if (ticketTypeRequest.getTicketType() == null) {
                exceptionMessages.add("Request #" + (i + 1) + " has NULL type");
            }
            if (!ticketPrices.containsKey(ticketTypeRequest.getTicketType())) {
                exceptionMessages.add("Request #" + (i + 1) + " has invalid ticket type");
            }
        }

        if (!exceptionMessages.isEmpty()) {
            final StringJoiner joiner = new StringJoiner("\n");
            joiner.add("Expected ticket type requests to be valid but got the following errors:");
            exceptionMessages.forEach(joiner::add);
            throw new InvalidPurchaseException(joiner.toString());
        }

        this.ticketTypeCounts = Arrays.stream(ticketTypeRequests)
                .collect(Collectors.groupingBy(TicketTypeRequest::getTicketType,
                        Collectors.summingInt(TicketTypeRequest::getNoOfTickets)));

        final int requestedAdultTickets = this.ticketTypeCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0),
                requestedChildTickets = this.ticketTypeCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0),
                requestedInfantTickets = this.ticketTypeCounts.getOrDefault(TicketTypeRequest.Type.INFANT, 0),
                requestTotalTickets = requestedAdultTickets + requestedChildTickets + requestedInfantTickets;

        if (requestTotalTickets > 20 || requestTotalTickets <= 0) {
            throw new InvalidPurchaseException("Expected total number of tickets to be between 1 and 20");
        } else if (!(requestedAdultTickets > 0)) {
            throw new InvalidPurchaseException("Expected at least one adult ticket to be requested");
        } else if (requestedInfantTickets > requestedAdultTickets) {
            throw new InvalidPurchaseException(
                    "Expected number of infant tickets to be less than or equal to the number of adult tickets requested");
        }

    }

}
