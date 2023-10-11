package uk.gov.dwp.uc.pairtest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypePrices;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private SeatReservationServiceImpl seatReservationService = new SeatReservationServiceImpl();
    private TicketPaymentServiceImpl ticketPaymentService = new TicketPaymentServiceImpl();

    @Override
    public void purchaseTickets(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        final Map<TicketTypeRequest.Type, Integer> ticketRequestsMapped = validateAndMapTicketRequests(accountId,
                ticketTypeRequests);
        callSeatReserveService(accountId, ticketRequestsMapped);
        callPaymentService(accountId, ticketRequestsMapped);
    }

    private void callSeatReserveService(final Long accountId,
            final Map<TicketTypeRequest.Type, Integer> ticketRequestsMapped)
            throws InvalidPurchaseException {
        final int totalSeatstoAllocate = ticketRequestsMapped.entrySet().stream()
                .filter(entry -> entry.getKey() == TicketTypeRequest.Type.ADULT
                        || entry.getKey() == TicketTypeRequest.Type.CHILD)
                .mapToInt(Map.Entry::getValue)
                .sum();
        seatReservationService.reserveSeat(accountId, totalSeatstoAllocate);
    }

    private void callPaymentService(final Long accountId,
            final Map<TicketTypeRequest.Type, Integer> ticketRequestsMapped)
            throws InvalidPurchaseException {
        final AtomicInteger totalAmountToPay = new AtomicInteger(0);
        ticketRequestsMapped.entrySet().stream()
                .filter(entry -> TicketTypePrices.getPrice(entry.getKey()) != 0)
                .forEach(entry -> totalAmountToPay
                        .addAndGet(TicketTypePrices.getPrice(entry.getKey()) * entry.getValue()));
        ticketPaymentService.makePayment(accountId, totalAmountToPay.get());
    }

    private Map<Type, Integer> validateAndMapTicketRequests(final Long accountId,
            final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        final List<String> exceptionMessages = new ArrayList<>();
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Expected a valid account ID but got " + accountId);
        }
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("Expected at least one ticket type request");
        }
        final AtomicInteger index = new AtomicInteger(1);
        Arrays.stream(ticketTypeRequests)
                .forEach(ticketTypeRequest -> {
                    final int i = index.getAndIncrement();
                    if (ticketTypeRequest.getNoOfTickets() < 0) {
                        exceptionMessages.add("Request #" + i + " has invalid number of tickets: "
                                + ticketTypeRequest.getNoOfTickets());
                    }
                    if (ticketTypeRequest.getTicketType() == null) {
                        exceptionMessages.add("Request #" + i + " has NULL type");
                    }
                    if (TicketTypePrices.getPrice(ticketTypeRequest.getTicketType()) == 9999) {
                        exceptionMessages.add("Request #" + i + " has invalid ticket type");
                    }
                });

        if (!exceptionMessages.isEmpty()) {
            final StringJoiner joiner = new StringJoiner("\n");
            joiner.add("Expected ticket type requests to be valid but got the following errors:");
            exceptionMessages.forEach(joiner::add);
            throw new InvalidPurchaseException(joiner.toString());
        }

        final Map<TicketTypeRequest.Type, Integer> ticketTypeCounts = Arrays.stream(ticketTypeRequests)
                .collect(Collectors.groupingBy(TicketTypeRequest::getTicketType,
                        Collectors.summingInt(TicketTypeRequest::getNoOfTickets)));

        final int requestedAdultTickets = ticketTypeCounts.getOrDefault(TicketTypeRequest.Type.ADULT, 0),
                requestedChildTickets = ticketTypeCounts.getOrDefault(TicketTypeRequest.Type.CHILD, 0),
                requestedInfantTickets = ticketTypeCounts.getOrDefault(TicketTypeRequest.Type.INFANT, 0),
                requestTotalTickets = requestedAdultTickets + requestedChildTickets + requestedInfantTickets;

        if (requestTotalTickets > 20 || requestTotalTickets <= 0) {
            throw new InvalidPurchaseException("Expected total number of tickets to be between 1 and 20");
        } else if (!(requestedAdultTickets > 0)) {
            throw new InvalidPurchaseException("Expected at least one adult ticket to be requested");
        } else if (requestedInfantTickets > requestedAdultTickets) {
            throw new InvalidPurchaseException(
                    "Expected number of infant tickets to be less than or equal to the number of adult tickets requested");
        }

        return ticketTypeCounts;
    }

}
