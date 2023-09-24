package kitchenpos.deliveryorders.tobe.application.dto;

import kitchenpos.deliveryorders.tobe.domain.DeliveryOrderLineItem;

import java.util.UUID;

public class OrderLineItemRequest {
    private long quantity;

    private UUID menuId;

    private long price;

    public OrderLineItemRequest(
        final long quantity,
        final UUID menuId,
        final long price
    ) {
        this.quantity = quantity;
        this.menuId = menuId;
        this.price = price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public UUID getMenuId() {
        return menuId;
    }

    public void setMenuId(UUID menuId) {
        this.menuId = menuId;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public DeliveryOrderLineItem toOrderLineItem() {
        return DeliveryOrderLineItem.create(quantity, menuId, price);
    }
}
