package kitchenpos.eatin_orders;

import kitchenpos.eatinorders.domain.orders.*;
import kitchenpos.eatinorders.domain.ordertables.NumberOfGuests;
import kitchenpos.eatinorders.domain.ordertables.OrderTable;
import kitchenpos.eatinorders.domain.ordertables.OrderTableName;
import kitchenpos.menus.tobe.domain.menu.Menu;
import kitchenpos.products.tobe.domain.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static kitchenpos.Fixtures.menu;

public class EatInOrderFixtures {

    public static EatInOrder waitingOrder(
            final OrderTable orderTable,
            final Menu menu,
            final MenuClient menuClient,
            final EatInOrderPolicy eatInOrderPolicy
    ) {
        return EatInOrder.waitingOrder(
                UUID.randomUUID(),
                LocalDateTime.of(2020, 1, 1, 12, 0),
                EatInOrderLineItems.from(List.of(eatInOrderLineItemMaterial(menu.getId())), menuClient),
                orderTable.getId(),
                eatInOrderPolicy
        );
    }

    public static EatInOrder acceptedOrder(
            final OrderTable orderTable,
            final Menu menu,
            final MenuClient menuClient,
            final EatInOrderPolicy eatInOrderPolicy
    ) {
        final EatInOrder order = waitingOrder(
                orderTable,
                menu,
                menuClient,
                eatInOrderPolicy
        );
        order.accept();
        return order;
    }

    public static EatInOrder servedOrder(
            final OrderTable orderTable,
            final Menu menu,
            final MenuClient menuClient,
            final EatInOrderPolicy eatInOrderPolicy
    ) {
        EatInOrder order = waitingOrder(
                orderTable,
                menu,
                menuClient,
                eatInOrderPolicy
        );
        order.accept();
        order.served();
        return order;
    }

    public static EatInOrder completedOrder(
            final OrderTable orderTable,
            final Menu menu,
            final MenuClient menuClient,
            final EatInOrderPolicy eatInOrderPolicy
    ) {
        EatInOrder order = waitingOrder(
                orderTable,
                menu,
                menuClient,
                eatInOrderPolicy
        );
        order.accept();
        order.served();
        order.complete(eatInOrderPolicy);
        return order;
    }

    public static EatInOrderLineItem eatInOrderLineItem(final MenuClient menuClient, final ProductRepository productRepository) {
        final Menu menu = menu(productRepository);
        OrderedMenus orderedMenus = menuClient.getOrderedMenuByMenuIds(List.of(menu.getId()));
        return EatInOrderLineItem.from(eatInOrderLineItemMaterial(menu.getId()), orderedMenus);
    }

    public static EatInOrderLineItemMaterial eatInOrderLineItemMaterial(UUID menuId) {
        return new EatInOrderLineItemMaterial(menuId, 1L);
    }

    public static EatInOrderLineItemMaterial eatInOrderLineItemMaterial(UUID menuId, long quantity) {
        return new EatInOrderLineItemMaterial(menuId, quantity);
    }

    public static OrderTable orderTable() {
        return orderTable(false, 0);
    }

    public static OrderTable orderTable(final boolean occupied, final int numberOfGuests) {
        return new OrderTable(UUID.randomUUID(), new OrderTableName("1번"), new NumberOfGuests(numberOfGuests), occupied);
    }
}
