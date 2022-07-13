package com.food.ordering.system.order.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.ProductId;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.create.OrderAddress;
import com.food.ordering.system.order.service.domain.dto.create.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Customer;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.ports.input.service.OrderApplicationService;
import com.food.ordering.system.order.service.domain.ports.output.repository.CustomerRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = OrderTestConfiguration.class)
class OrderApplicationServiceTest {

  @Autowired
  private OrderApplicationService orderApplicationService;
  @Autowired
  private OrderDataMapper orderDataMapper;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private CustomerRepository customerRepository;
  @Autowired
  private RestaurantRepository restaurantRepository;
  private CreateOrderCommand createOrderCommand;
  private CreateOrderCommand createOrderCommandWrongPrice;
  private CreateOrderCommand createOrderCommandWrongProductPrice;
  private final UUID CUSTOMER_ID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb41");
  private final UUID RESTAURANT_ID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb45");
  private final UUID PRODUCT_ID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb46");
  private final UUID ORDER_ID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb47");
  private final BigDecimal PRICE = new BigDecimal("200.00");

  @BeforeAll
  public void init() {
    createOrderCommand = CreateOrderCommand.builder().customerId(CUSTOMER_ID)
        .restaurantId(RESTAURANT_ID).address(
            OrderAddress.builder().street("street_1").postalCode("111000").city("Montevideo")
                .build()).price(PRICE).items(List.of(
            OrderItem.builder().productId(PRODUCT_ID).quantity(1).price(new BigDecimal("50.00"))
                .subTotal(new BigDecimal("50.00")).build(),
            OrderItem.builder().productId(PRODUCT_ID).quantity(3).price(new BigDecimal("50.00"))
                .subTotal(new BigDecimal("150.00")).build())).build();
    createOrderCommandWrongPrice = CreateOrderCommand.builder().customerId(CUSTOMER_ID)
        .restaurantId(RESTAURANT_ID).address(
            OrderAddress.builder().street("street_1").postalCode("111000").city("Montevideo")
                .build()).price(new BigDecimal("250.00")).items(List.of(
            OrderItem.builder().productId(PRODUCT_ID).quantity(1).price(new BigDecimal("50.00"))
                .subTotal(new BigDecimal("50.00")).build(),
            OrderItem.builder().productId(PRODUCT_ID).quantity(3).price(new BigDecimal("50.00"))
                .subTotal(new BigDecimal("150.00")).build())).build();
    createOrderCommandWrongProductPrice = CreateOrderCommand.builder().customerId(CUSTOMER_ID)
        .restaurantId(RESTAURANT_ID).address(
            OrderAddress.builder().street("street_1").postalCode("111000").city("Montevideo")
                .build()).price(new BigDecimal("210.00")).items(List.of(
            OrderItem.builder().productId(PRODUCT_ID).quantity(1).price(new BigDecimal("60.00"))
                .subTotal(new BigDecimal("60.00")).build(),
            OrderItem.builder().productId(PRODUCT_ID).quantity(3).price(new BigDecimal("50.00"))
                .subTotal(new BigDecimal("150.00")).build())).build();
    Customer customer = new Customer();
    customer.setId(new CustomerId(CUSTOMER_ID));
    Restaurant restaurantResponse = Restaurant.builder()
        .restaurantId(new RestaurantId(createOrderCommand.getRestaurantId()))
        .products(List.of(
            new Product(new ProductId(PRODUCT_ID), "product-1",
                new Money(new BigDecimal("50.00"))),
            new Product(new ProductId(PRODUCT_ID), "product-2",
                new Money(new BigDecimal("50.00")))))
        .active(true)
        .build();

    Order order = orderDataMapper.createOrderCommandToOrder(createOrderCommand);
    order.setId(new OrderId(ORDER_ID));

    when(customerRepository.findCustomer(CUSTOMER_ID)).thenReturn(Optional.of(customer));
    when(restaurantRepository.findRestaurantInformation(
        orderDataMapper.createOrderCommandToRestaurant(createOrderCommand)))
        .thenReturn(Optional.of(restaurantResponse));
    when(orderRepository.save(any(Order.class))).thenReturn(order);
  }

  @Test
  void testCreateOrder() {
    CreateOrderResponse createOrderResponse = orderApplicationService.createOrder(
        createOrderCommand);
    assertEquals(createOrderResponse.getOrderStatus(), OrderStatus.PENDING);
    assertEquals(createOrderResponse.getMessage(), "Order created successfully");
    assertNotNull(createOrderResponse.getOrderTrackingId());
  }

  @Test
  void testCreateOrderWithWrongTotalPrice() {
    OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
        () -> orderApplicationService.createOrder(createOrderCommandWrongPrice));
    assertEquals("Total price: 250.00 is not equals to Order items Total: 200.00!",
        orderDomainException.getMessage());
  }

  @Test
  void testCreateOrderWithWrongProductPrice() {
    OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
        () -> orderApplicationService.createOrder(createOrderCommandWrongProductPrice));
    assertEquals("Order item price: 60.00 is not valid for product " + PRODUCT_ID,
        orderDomainException.getMessage());
  }

  @Test
  void testCreateOrderWithPassiveRestaurant() {
    Restaurant restaurantPassiveResponse = Restaurant.builder()
        .restaurantId(new RestaurantId(createOrderCommand.getRestaurantId()))
        .products(List.of(
            new Product(new ProductId(PRODUCT_ID), "product-1",
                new Money(new BigDecimal("50.00"))),
            new Product(new ProductId(PRODUCT_ID), "product-2",
                new Money(new BigDecimal("50.00")))))
        .active(false)
        .build();

    when(restaurantRepository.findRestaurantInformation(
        orderDataMapper.createOrderCommandToRestaurant(createOrderCommand)))
        .thenReturn(Optional.of(restaurantPassiveResponse));
    OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
        () -> orderApplicationService.createOrder(createOrderCommand));
    assertEquals("Restaurant with id " + RESTAURANT_ID + " is currently not active!", orderDomainException.getMessage());
  }
}
