package kitchenpos.takeout_orders.application;

import kitchenpos.menus.application.InMemoryMenuRepository;
import kitchenpos.menus.tobe.domain.menu.MenuRepository;
import kitchenpos.products.application.InMemoryProductRepository;
import kitchenpos.products.tobe.domain.ProductRepository;
import kitchenpos.takeout_orders.infrastructure.InMemoryTakeoutOrderRepository;
import kitchenpos.takeoutorders.application.TakeoutOrderService;
import kitchenpos.takeoutorders.domain.OrderStatus;
import kitchenpos.takeoutorders.domain.TakeoutOrder;
import kitchenpos.takeoutorders.domain.TakeoutOrderLineItem;
import kitchenpos.takeoutorders.domain.TakeoutOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.*;

import static kitchenpos.Fixtures.*;
import static kitchenpos.takeout_orders.TakeoutOrderFixtures.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class TakeoutOrderServiceTest {
    private TakeoutOrderRepository orderRepository;
    private ProductRepository productRepository;
    private MenuRepository menuRepository;
    private TakeoutOrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryTakeoutOrderRepository();
        productRepository = new InMemoryProductRepository();
        menuRepository = new InMemoryMenuRepository();
        orderService = new TakeoutOrderService(orderRepository, menuRepository);
    }

    @DisplayName("1개 이상의 TakeoutOrderLineItem으로 TakeoutOrder를 등록할 수 있다.")
    @Test
    void createTakeoutOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, productRepository)).getId();
        final TakeoutOrder expected = createOrderRequest(createOrderLineItemRequest(menuId, 19_000L, 3L));
        final TakeoutOrder actual = orderService.create(expected);
        assertThat(actual).isNotNull();
        assertAll(
                () -> assertThat(actual.getId()).isNotNull(),
                () -> assertThat(actual.getStatus()).isEqualTo(OrderStatus.WAITING),
                () -> assertThat(actual.getOrderDateTime()).isNotNull(),
                () -> assertThat(actual.getOrderLineItems()).hasSize(1)
        );
    }

    @DisplayName("TakeoutOrderLineItem이 없으면 TakeoutOrder는 등록할 수 없다.")
    @MethodSource("orderLineItems")
    @ParameterizedTest
    void create(final List<TakeoutOrderLineItem> orderLineItems) {
        final TakeoutOrder expected = createOrderRequest(orderLineItems);
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Arguments> orderLineItems() {
        return Arrays.asList(
                null,
                Arguments.of(Collections.emptyList()),
                Arguments.of(Arrays.asList(createOrderLineItemRequest(INVALID_ID, 19_000L, 3L)))
        );
    }

    @DisplayName("TakeoutOrder의 TakeoutOrderLineItem 수량은 0개 이상이어야 한다.")
    @ValueSource(longs = -1L)
    @ParameterizedTest
    void createWithoutEatInOrder(final long quantity) {
        final UUID menuId = menuRepository.save(menu(19_000L, true, productRepository)).getId();
        final TakeoutOrder expected = createOrderRequest(
                createOrderLineItemRequest(menuId, 19_000L, quantity)
        );
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Hide된 Menu를 포함한 TakeoutOrderLineItem은 주문할 수 없다.")
    @Test
    void createNotDisplayedMenuOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, false, productRepository)).getId();
        final TakeoutOrder expected = createOrderRequest(createOrderLineItemRequest(menuId, 19_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("TakeoutOrderLineItem의 Price은 주문 당시 Menu의 Price과 일치해야 한다.")
    @Test
    void createNotMatchedMenuPriceOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, productRepository)).getId();
        final TakeoutOrder expected = createOrderRequest(createOrderLineItemRequest(menuId, 16_000L, 3L));
        assertThatThrownBy(() -> orderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("TakeoutOrder을 Accept한다.")
    @Test
    void accept() {
        final UUID orderId = orderRepository.save(order(OrderStatus.WAITING, productRepository)).getId();
        final TakeoutOrder actual = orderService.accept(orderId);
        assertThat(actual.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @DisplayName("Waiting인 TakeoutOrder만 Accept할 수 있다.")
    @EnumSource(value = OrderStatus.class, names = "WAITING", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void accept(final OrderStatus status) {
        final UUID orderId = orderRepository.save(order(status, productRepository)).getId();
        assertThatThrownBy(() -> orderService.accept(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("TakeoutOrder을 Serve한다.")
    @Test
    void serve() {
        final UUID orderId = orderRepository.save(order(OrderStatus.ACCEPTED, productRepository)).getId();
        final TakeoutOrder actual = orderService.serve(orderId);
        assertThat(actual.getStatus()).isEqualTo(OrderStatus.SERVED);
    }

    @DisplayName("Accepted된 TakeoutOrder만 Serve할 수 있다.")
    @EnumSource(value = OrderStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void serve(final OrderStatus status) {
        final UUID orderId = orderRepository.save(order(status, productRepository)).getId();
        assertThatThrownBy(() -> orderService.serve(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("TakeoutOrder을 Complete한다.")
    @Test
    void complete() {
        final TakeoutOrder expected = orderRepository.save(order(OrderStatus.SERVED, productRepository));
        final TakeoutOrder actual = orderService.complete(expected.getId());
        assertThat(actual.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @DisplayName("Served된 TakeoutOrder만 Complete할 수 있다.")
    @EnumSource(value = OrderStatus.class, names = "SERVED", mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest
    void completeTakeoutAndEatInOrder(final OrderStatus status) {
        final UUID orderId = orderRepository.save(order(status, productRepository)).getId();
        assertThatThrownBy(() -> orderService.complete(orderId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("TakeoutOrder의 목록을 조회할 수 있다.")
    @Test
    void findAll() {
        orderRepository.save(order(OrderStatus.SERVED, productRepository));
        orderRepository.save(order(OrderStatus.COMPLETED, productRepository));
        final List<TakeoutOrder> actual = orderService.findAll();
        assertThat(actual).hasSize(2);
    }

    private TakeoutOrder createOrderRequest(final TakeoutOrderLineItem... orderLineItems) {
        return createOrderRequest(Arrays.asList(orderLineItems));
    }

    private TakeoutOrder createOrderRequest(final List<TakeoutOrderLineItem> orderLineItems) {
        final TakeoutOrder order = new TakeoutOrder();
        order.setOrderLineItems(orderLineItems);
        return order;
    }

    private static TakeoutOrderLineItem createOrderLineItemRequest(final UUID menuId, final long price, final long quantity) {
        final TakeoutOrderLineItem orderLineItem = new TakeoutOrderLineItem();
        orderLineItem.setSeq(new Random().nextLong());
        orderLineItem.setMenuId(menuId);
        orderLineItem.setPrice(BigDecimal.valueOf(price));
        orderLineItem.setQuantity(quantity);
        return orderLineItem;
    }
}
