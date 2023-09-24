package kitchenpos.eatin_orders.application;

import kitchenpos.eatin_orders.infrastructure.InMemoryEatInOrderRepository;
import kitchenpos.eatin_orders.infrastructure.InMemoryOrderTableRepository;
import kitchenpos.eatinorders.application.orders.EatInOrderService;
import kitchenpos.eatinorders.application.orders.dto.EatInOrderCreateRequest;
import kitchenpos.eatinorders.domain.orders.*;
import kitchenpos.eatinorders.domain.ordertables.NumberOfGuests;
import kitchenpos.eatinorders.domain.ordertables.OrderTable;
import kitchenpos.eatinorders.domain.ordertables.OrderTableRepository;
import kitchenpos.eatinorders.infrastructure.MenuClientImpl;
import kitchenpos.menus.application.InMemoryMenuRepository;
import kitchenpos.menus.tobe.domain.menu.Menu;
import kitchenpos.menus.tobe.domain.menu.MenuRepository;
import kitchenpos.products.application.InMemoryProductRepository;
import kitchenpos.products.tobe.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static kitchenpos.Fixtures.*;
import static kitchenpos.eatin_orders.EatInOrderFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EatInOrderServiceTest {
    private EatInOrderRepository eatInOrderRepository;
    private ProductRepository productRepository;
    private MenuRepository menuRepository;
    private MenuClient menuClient;
    private EatInOrderPolicy eatInOrderPolicy;
    private OrderTableRepository orderTableRepository;
    private EatInOrderService eatInOrderService;

    @BeforeEach
    void setUp() {
        eatInOrderRepository = new InMemoryEatInOrderRepository();
        orderTableRepository = new InMemoryOrderTableRepository();
        menuRepository = new InMemoryMenuRepository();
        menuClient = new MenuClientImpl(menuRepository);
        eatInOrderPolicy = new EatInOrderPolicy(orderTableRepository);
        productRepository = new InMemoryProductRepository();
        eatInOrderService = new EatInOrderService(eatInOrderRepository, menuClient, eatInOrderPolicy);
    }

    @DisplayName("1개 이상의 EatInOrderLineItem으로 EatInOrder를 등록할 수 있다.")
    @Test
    void createEatInOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, productRepository)).getId();
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 4)).getId();
        final EatInOrderCreateRequest expected = createOrderRequest(orderTableId, eatInOrderLineItemMaterial(menuId));
        final EatInOrder actual = eatInOrderService.create(expected);
        assertThat(actual).isNotNull();
        assertAll(
                () -> assertThat(actual.getId()).isNotNull(),
                () -> assertThat(actual.getStatus()).isEqualTo(OrderStatus.WAITING),
                () -> assertThat(actual.getOrderDateTime()).isNotNull(),
                () -> assertThat(actual.getOrderLineItems().getOrderLineItems()).hasSize(1),
                () -> assertThat(actual.getOrderTableId()).isEqualTo(expected.getOrderTableId())
        );
    }

    @DisplayName("EatInOrderLineItem가 없으면 EatInOrder는 등록할 수 없다.")
    @MethodSource("orderLineItems")
    @ParameterizedTest
    void create(final List<EatInOrderLineItemMaterial> orderLineItems) {
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 4)).getId();
        final EatInOrderCreateRequest expected = createOrderRequest(orderTableId, orderLineItems);
        assertThatThrownBy(() -> eatInOrderService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Arguments> orderLineItems() {
        return Arrays.asList(
                null,
                Arguments.of(Collections.emptyList()),
                Arguments.of(Arrays.asList(eatInOrderLineItemMaterial(INVALID_ID)))
        );
    }

    @DisplayName("EatInOrder는 EatInOrderLineItem의 수량은 0 미만일 수 있다.")
    @ValueSource(longs = -1L)
    @ParameterizedTest
    void createEatInOrder(final long quantity) {
        final UUID menuId = menuRepository.save(menu(19_000L, true, productRepository)).getId();
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 4)).getId();
        final EatInOrderCreateRequest expected = createOrderRequest(
                orderTableId, eatInOrderLineItemMaterial(menuId, quantity)
        );
        assertDoesNotThrow(() -> eatInOrderService.create(expected));
    }

    @DisplayName("Clear된 OrderTable에는 EatInOrder를 등록할 수 없다.")
    @Test
    void createClearTableEatInOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, productRepository)).getId();
        final UUID orderTableId = orderTableRepository.save(orderTable(false, 0)).getId();
        final EatInOrderCreateRequest expected = createOrderRequest(
                orderTableId, eatInOrderLineItemMaterial(menuId, 3L)
        );
        assertThatThrownBy(() -> eatInOrderService.create(expected))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("Not Displayed인 Menu를 포함한 EatInOrder는 등록할 수 없다.")
    @Test
    void createNotDisplayedMenuOrder() {
        final UUID menuId = menuRepository.save(menu(19_000L, false, productRepository)).getId();
        final UUID orderTableId = orderTableRepository.save(orderTable(true, 4)).getId();
        final EatInOrderCreateRequest expected = createOrderRequest(orderTableId, eatInOrderLineItemMaterial(menuId));
        assertThatThrownBy(() -> eatInOrderService.create(expected))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("EatInOrder을 Accept한다.")
    @Test
    void accept() {
        final Menu menu = menuRepository.save(menu(true, productRepository));
        final OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        final UUID orderId = eatInOrderRepository.save(waitingOrder(orderTable, menu, menuClient, eatInOrderPolicy)).getId();
        final EatInOrder actual = eatInOrderService.accept(orderId);
        assertThat(actual.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @DisplayName("Waiting인 EatInOrder만 Accept할 수 있다.")
    @Test
    void acceptFromOnlyWaitingOrder() {
        ordersOtherThanWaitingOrder().forEach(order -> {
            final UUID orderId = eatInOrderRepository.save(order).getId();
            assertThatThrownBy(() -> eatInOrderService.accept(orderId))
                    .isInstanceOf(IllegalStateException.class);
        });
    }

    private List<EatInOrder> ordersOtherThanWaitingOrder() {
        final Menu menu = menuRepository.save(menu(productRepository));
        OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        return Arrays.asList(
                acceptedOrder(orderTable, menu, menuClient, eatInOrderPolicy),
                servedOrder(orderTable, menu, menuClient, eatInOrderPolicy),
                completedOrder(orderTable, menu, menuClient, eatInOrderPolicy)
        );
    }

    @DisplayName("EatInOrder을 Serve한다.")
    @Test
    void serve() {
        final Menu menu = menuRepository.save(menu(productRepository));
        OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        final UUID orderId = eatInOrderRepository.save(acceptedOrder(orderTable, menu, menuClient, eatInOrderPolicy)).getId();
        final EatInOrder actual = eatInOrderService.serve(orderId);
        assertThat(actual.getStatus()).isEqualTo(OrderStatus.SERVED);
    }

    @DisplayName("Accepted인 EatInOrder만 Serve할 수 있다.")
    @Test
    void serveFromOnlyAcceptOrder() {
        ordersOtherThanAcceptedOrder().forEach(order -> {
            final UUID orderId = eatInOrderRepository.save(order).getId();
            assertThatThrownBy(() -> eatInOrderService.serve(orderId))
                    .isInstanceOf(IllegalStateException.class);
        });
    }

    private List<EatInOrder> ordersOtherThanAcceptedOrder() {
        final Menu menu = menuRepository.save(menu(productRepository));
        OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        return Arrays.asList(
                waitingOrder(orderTable, menu, menuClient, eatInOrderPolicy),
                servedOrder(orderTable, menu, menuClient, eatInOrderPolicy),
                completedOrder(orderTable, menu, menuClient, eatInOrderPolicy)
        );
    }

    @DisplayName("EatInOrder을 Complete한다.")
    @Test
    void complete() {
        final Menu menu = menuRepository.save(menu(productRepository));
        OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        final EatInOrder expected = eatInOrderRepository.save(servedOrder(orderTable, menu, menuClient, eatInOrderPolicy));
        final EatInOrder actual = eatInOrderService.complete(expected.getId());
        assertThat(actual.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @DisplayName("Served인 EatInOrder만 Complete할 수 있다.")
    @Test
    void completeTakeoutAndEatInOrder() {
        ordersOtherThanServedOrder().forEach(order -> {
            final UUID orderId = eatInOrderRepository.save(order).getId();
            assertThatThrownBy(() -> eatInOrderService.complete(orderId))
                    .isInstanceOf(IllegalStateException.class);
        });
    }

    private List<EatInOrder> ordersOtherThanServedOrder() {
        final Menu menu = menuRepository.save(menu(productRepository));
        OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        return Arrays.asList(
                waitingOrder(orderTable, menu, menuClient, eatInOrderPolicy),
                acceptedOrder(orderTable, menu, menuClient, eatInOrderPolicy),
                completedOrder(orderTable, menu, menuClient, eatInOrderPolicy)
        );
    }

    @DisplayName("EatInOrder가 Completed되면 OrderTable을 Clear한다.")
    @Test
    void completeEatInOrder() {
        final Menu menu = menuRepository.save(menu(productRepository));
        final OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        final EatInOrder expected = eatInOrderRepository.save(servedOrder(orderTable, menu, menuClient, eatInOrderPolicy));
        final EatInOrder actual = eatInOrderService.complete(expected.getId());
        assertAll(
                () -> assertThat(actual.getStatus()).isEqualTo(OrderStatus.COMPLETED),
                () -> assertThat(orderTableRepository.findById(orderTable.getId()).get().isOccupied()).isFalse(),
                () -> assertThat(orderTableRepository.findById(orderTable.getId()).get().getNumberOfGuests()).isEqualTo(NumberOfGuests.ZERO)
        );
    }

    @DisplayName("Complete가 아닌 EatInOrder가 있는 OrderTable은 Clear하지 않는다.")
    @Test
    void completeNotTable() {
        final Menu menu = menuRepository.save(menu(productRepository));
        final OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        final EatInOrder order = eatInOrderRepository.save(acceptedOrder(orderTable, menu, menuClient, eatInOrderPolicy));
        order.served();
        assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.SERVED),
                () -> assertThat(orderTableRepository.findById(orderTable.getId()).get().isOccupied()).isTrue(),
                () -> assertThat(orderTableRepository.findById(orderTable.getId()).get().getNumberOfGuests()).isEqualTo(new NumberOfGuests(4))
        );
    }

    @DisplayName("EatInOrder의 목록을 조회할 수 있다.")
    @Test
    void findAll() {
        final Menu menu = menuRepository.save(menu(productRepository));
        final OrderTable orderTable = orderTableRepository.save(orderTable(true, 4));
        eatInOrderRepository.save(waitingOrder(orderTable, menu, menuClient, eatInOrderPolicy));
        eatInOrderRepository.save(waitingOrder(orderTable, menu, menuClient, eatInOrderPolicy));
        final List<EatInOrder> actual = eatInOrderService.findAll();
        assertThat(actual).hasSize(2);
    }

    private EatInOrderCreateRequest createOrderRequest(final UUID orderTableId, final List<EatInOrderLineItemMaterial> orderLineItems) {
        return new EatInOrderCreateRequest(
                orderTableId,
                orderLineItems
        );
    }

    private EatInOrderCreateRequest createOrderRequest(
            final UUID orderTableId,
            final EatInOrderLineItemMaterial... orderLineItems
    ) {
        return new EatInOrderCreateRequest(
                orderTableId,
                Arrays.asList(orderLineItems)
        );
    }
}
