package test.java;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.gov.dwp.uc.pairtest.domain.TicketTypePrices;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;

public class TicketTypePricesTest {
    @Test
    public void testGetPriceAdult() {
        // When
        final int price = TicketTypePrices.getPrice(Type.ADULT);

        // Then
        assertEquals(20, price);
    }

    @Test
    public void testGetPriceChild() {
        // When
        final int price = TicketTypePrices.getPrice(Type.CHILD);

        // Then
        assertEquals(10, price);
    }

    @Test
    public void testGetPriceInfant() {
        // When
        final int price = TicketTypePrices.getPrice(Type.INFANT);

        // Then
        assertEquals(0, price);
    }

}
