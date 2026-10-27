package com.project.ibm.retailmanagementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.ibm.retailmanagementsystem.dto.PlaceOrderRequestDTO;
import com.project.ibm.retailmanagementsystem.dto.PurchaseProduct;
import com.project.ibm.retailmanagementsystem.model.Customer;
import com.project.ibm.retailmanagementsystem.model.Inventory;
import com.project.ibm.retailmanagementsystem.model.OrderDetails;
import com.project.ibm.retailmanagementsystem.model.OrderItem;
import com.project.ibm.retailmanagementsystem.model.Product;
import com.project.ibm.retailmanagementsystem.model.Store;
import com.project.ibm.retailmanagementsystem.repository.CustomerRepository;
import com.project.ibm.retailmanagementsystem.repository.InventoryRepository;
import com.project.ibm.retailmanagementsystem.repository.OrderDetailsRepository;
import com.project.ibm.retailmanagementsystem.repository.OrderItemRepository;
import com.project.ibm.retailmanagementsystem.repository.ProductRepository;
import com.project.ibm.retailmanagementsystem.repository.StoreRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final long STORE_ID = 1L;
    private static final String EMAIL = "ada@example.com";

    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private OrderDetailsRepository orderDetailsRepository;
    @Mock private OrderItemRepository orderItemRepository;

    private OrderService orderService;
    private Store store;
    private Product mug;        // id 10, £2.50
    private Product kettle;     // id 20, £10.00
    private Inventory mugStock;
    private Inventory kettleStock;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(productRepository, inventoryRepository, customerRepository,
                storeRepository, orderDetailsRepository, orderItemRepository);

        store = new Store("1 High Street", "Swansea");
        store.setId(STORE_ID);
        mug = product(10L, "Mug", 2.50);
        kettle = product(20L, "Kettle", 10.00);
        mugStock = new Inventory(store, 10, mug);
        kettleStock = new Inventory(store, 5, kettle);

        lenient().when(storeRepository.findById(STORE_ID)).thenReturn(Optional.of(store));
        lenient().when(productRepository.findByid(10L)).thenReturn(mug);
        lenient().when(productRepository.findByid(20L)).thenReturn(kettle);
        lenient().when(inventoryRepository.findByProductIdandStoreId(10L, STORE_ID)).thenReturn(mugStock);
        lenient().when(inventoryRepository.findByProductIdandStoreId(20L, STORE_ID)).thenReturn(kettleStock);
        lenient().when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderDetailsRepository.save(any(OrderDetails.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void placesOrderDecrementsStockAndUsesCataloguePrices() {
        // Client-side prices are deliberately wrong: the server must use the catalogue price.
        PlaceOrderRequestDTO request = request(line(10L, 2, 999.0), line(20L, 1, 0.01));

        OrderDetails order = orderService.saveOrder(request);

        assertEquals(15.00, order.getTotalPrice(), 0.0001);
        assertEquals(8, mugStock.getStockLevel());
        assertEquals(4, kettleStock.getStockLevel());

        ArgumentCaptor<OrderItem> items = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(2)).save(items.capture());
        OrderItem first = items.getAllValues().get(0);
        assertSame(mug, first.getProduct());
        assertEquals(2, first.getQuantity());
        assertEquals(2.50, first.getPrice(), 0.0001);
    }

    @Test
    void createsCustomerWhenEmailIsNew() {
        when(customerRepository.findByEmail(EMAIL)).thenReturn(null);

        orderService.saveOrder(request(line(10L, 1, 2.50)));

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(saved.capture());
        assertEquals(EMAIL, saved.getValue().getEmail());
    }

    @Test
    void reusesExistingCustomer() {
        when(customerRepository.findByEmail(EMAIL)).thenReturn(new Customer("Ada", EMAIL, "0123"));

        orderService.saveOrder(request(line(10L, 1, 2.50)));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void rejectsInsufficientStockWithoutWritingAnything() {
        OrderException e = assertThrows(OrderException.class,
                () -> orderService.saveOrder(request(line(10L, 1, 2.50), line(20L, 6, 10.0))));

        assertTrue(e.getMessage().contains("Not enough stock for Kettle"), e.getMessage());
        assertEquals(10, mugStock.getStockLevel(), "earlier lines must not be decremented");
        assertNothingSaved();
    }

    @Test
    void combinesRepeatedLinesForTheSameProductBeforeCheckingStock() {
        OrderException e = assertThrows(OrderException.class,
                () -> orderService.saveOrder(request(line(20L, 3, 10.0), line(20L, 3, 10.0))));

        assertTrue(e.getMessage().contains("requested 6, available 5"), e.getMessage());
        assertNothingSaved();
    }

    @Test
    void rejectsProductNotStockedInStore() {
        Product teapot = product(30L, "Teapot", 15.0);
        when(productRepository.findByid(30L)).thenReturn(teapot);
        when(inventoryRepository.findByProductIdandStoreId(30L, STORE_ID)).thenReturn(null);

        OrderException e = assertThrows(OrderException.class,
                () -> orderService.saveOrder(request(line(30L, 1, 15.0))));

        assertTrue(e.getMessage().contains("not stocked"), e.getMessage());
        assertNothingSaved();
    }

    @Test
    void rejectsUnknownStore() {
        PlaceOrderRequestDTO request = request(line(10L, 1, 2.50));
        request.setStoreId(99L);
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderException.class, () -> orderService.saveOrder(request));
        assertNothingSaved();
    }

    @Test
    void rejectsEmptyOrderAndNonPositiveQuantities() {
        assertThrows(OrderException.class, () -> orderService.saveOrder(request()));
        assertThrows(OrderException.class, () -> orderService.saveOrder(request(line(10L, 0, 2.50))));
        assertThrows(OrderException.class, () -> orderService.saveOrder(request(line(10L, -1, 2.50))));
        assertNothingSaved();
    }

    private void assertNothingSaved() {
        verify(customerRepository, never()).save(any());
        verify(orderDetailsRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
    }

    private static Product product(long id, String name, double price) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        p.setCategory("Kitchen");
        p.setSku("SKU-" + id);
        return p;
    }

    private static PurchaseProduct line(long productId, int quantity, double price) {
        return new PurchaseProduct(productId, quantity, price);
    }

    private static PlaceOrderRequestDTO request(PurchaseProduct... lines) {
        return new PlaceOrderRequestDTO("Ada", EMAIL, "0123", STORE_ID, 0.0, List.of(lines));
    }
}
