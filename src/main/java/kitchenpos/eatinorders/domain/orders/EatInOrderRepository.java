package kitchenpos.eatinorders.domain.orders;

import kitchenpos.eatinorders.domain.ordertables.OrderTable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EatInOrderRepository {
    EatInOrder save(EatInOrder order);

    Optional<EatInOrder> findById(UUID id);

    List<EatInOrder> findAll();
}
