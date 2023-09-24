package kitchenpos.eatinorders.tobe.infra;

import kitchenpos.eatinorders.tobe.domain.EatInOrder;
import kitchenpos.eatinorders.tobe.domain.EatInOrderRepository;
import kitchenpos.sharedkernel.OrderStatus;

import java.util.*;

public class InMemoryEatInOrderRepository implements EatInOrderRepository {

    private final Map<UUID, EatInOrder> orders = new HashMap<>();

    @Override
    public EatInOrder save(final EatInOrder order) {
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<EatInOrder> findById(final UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<EatInOrder> findAll() {
        return new ArrayList<>(orders.values());
    }

    @Override
    public boolean existsByOrderTableIdAndStatusNot(UUID orderTableId, OrderStatus status) {
        return orders.values()
            .stream()
            .anyMatch(order -> order.getOrderTableId().equals(orderTableId) && order.getStatus() != status);
    }
}
