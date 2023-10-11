package test.java;

import static org.mockito.Mockito.calls;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceImplTest {

    @Mock
    private SeatReservationServiceImpl seatReservationService;

    @Mock
    private TicketPaymentServiceImpl ticketPaymentService;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testWithEmptyTicketTypeRequests() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected at least one ticket type request");
        ticketServiceImpl.purchaseTickets(1L, new TicketTypeRequest[] {});
    }

    @Test
    public void testWithNullTicketTypeRequests() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected at least one ticket type request");
        ticketServiceImpl.purchaseTickets(1L);
    }

    @Test
    public void testWithInvalidAccountId() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1) };

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected a valid account ID but got null");
        ticketServiceImpl.purchaseTickets(null, ticketTypeRequests);
    }

    @Test
    public void testWithZeroAccountId() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1) };

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected a valid account ID but got 0");
        ticketServiceImpl.purchaseTickets(0L, ticketTypeRequests);
    }

    @Test
    public void testWithInvalidTicketTypeRequest() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1),
                null };

        exceptionRule.expect(java.lang.NullPointerException.class);
        ticketServiceImpl.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test
    public void testWithInvalidChildTicketRequest() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1) };

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected at least one adult ticket to be requested");
        ticketServiceImpl.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test
    public void testWithZeroTotalNumberTicketRequest() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0) };

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected total number of tickets to be between 1 and 20");
        ticketServiceImpl.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test
    public void testWithNegativeTotalNumberTicketRequest() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1) };

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected ticket type requests to be valid but got the following errors:\n" + //
                "Request #1 has invalid number of tickets: -1");
        ticketServiceImpl.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test
    public void testWithTooManyTotalTicketCountRequest() {
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1) };

        exceptionRule.expect(InvalidPurchaseException.class);
        exceptionRule.expectMessage("Expected total number of tickets to be between 1 and 20");
        ticketServiceImpl.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test
    public void testWithAllTypesCorrectTicketsRequest()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        // Given
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final Field seatReservationServiceField = TicketServiceImpl.class.getDeclaredField("seatReservationService");
        if (seatReservationServiceField.trySetAccessible()) {
            seatReservationServiceField.set(ticketServiceImpl, seatReservationService);
        }
        final Field ticketPaymentServiceField = TicketServiceImpl.class.getDeclaredField("ticketPaymentService");
        if (ticketPaymentServiceField.trySetAccessible()) {
            ticketPaymentServiceField.set(ticketServiceImpl, ticketPaymentService);
        }
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 11),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1) };

        // When
        ticketServiceImpl.purchaseTickets(1L, ticketTypeRequests);

        // Then
        final InOrder inOrder = Mockito.inOrder(seatReservationService, ticketPaymentService);
        inOrder.verify(seatReservationService, calls(1)).reserveSeat(1L, 12);
        inOrder.verify(ticketPaymentService, calls(1)).makePayment(1L, 230);
    }

    @Test
    public void testWithAdultAndInfantOnlyTypesCorrectTicketsRequest()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        // Given
        final TicketServiceImpl ticketServiceImpl = new TicketServiceImpl();
        final Field seatReservationServiceField = TicketServiceImpl.class.getDeclaredField("seatReservationService");
        if (seatReservationServiceField.trySetAccessible()) {
            seatReservationServiceField.set(ticketServiceImpl, seatReservationService);
        }
        final Field ticketPaymentServiceField = TicketServiceImpl.class.getDeclaredField("ticketPaymentService");
        if (ticketPaymentServiceField.trySetAccessible()) {
            ticketPaymentServiceField.set(ticketServiceImpl, ticketPaymentService);
        }
        final TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[] {
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 10) };

        // When
        ticketServiceImpl.purchaseTickets(1L, ticketTypeRequests);

        // Then
        final InOrder inOrder = Mockito.inOrder(seatReservationService, ticketPaymentService);
        inOrder.verify(seatReservationService, calls(1)).reserveSeat(1L, 10);
        inOrder.verify(ticketPaymentService, calls(1)).makePayment(1L, 200);
    }
}
