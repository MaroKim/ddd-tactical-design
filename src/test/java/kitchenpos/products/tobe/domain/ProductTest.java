package kitchenpos.products.tobe.domain;

import kitchenpos.products.application.FakeProductNameProfanities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ProductTest {

    private ProductNameProfanities productNameProfanities;
    private ProductNamePolicy productNamePolicy;

    @BeforeEach
    void setUp() {
        productNameProfanities = new FakeProductNameProfanities();
        productNamePolicy = new ProductNamePolicy(productNameProfanities);
    }

    @DisplayName("Product을 생성할 수 있다.")
    @Test
    void create() {
        final Product product = new Product(
                UUID.randomUUID(),
                Name.from("후라이드", productNamePolicy),
                Price.from(BigDecimal.valueOf(16_000L)));
        assertAll(
                () -> assertThat(product.getId()).isNotNull(),
                () -> assertThat(product.getProductName()).isEqualTo(Name.from("후라이드", productNamePolicy)),
                () -> assertThat(product.getPrice()).isEqualTo(Price.from(BigDecimal.valueOf(16_000L)))
        );
    }

    @DisplayName("Product에 임의의 ID를 부여할 수 있다.")
    @Test
    void giveId() {
        final Product product = new Product(
                Name.from("후라이드", productNamePolicy),
                Price.from(BigDecimal.valueOf(16_000L)));
        final Product result = product.giveId();
        assertThat(result.getId()).isNotNull();
    }

    @DisplayName("Product의 Price를 변경할 수 있다.")
    @Test
    void changePrice() {
        final Product product = new Product(
                UUID.randomUUID(),
                Name.from("후라이드", productNamePolicy),
                Price.from(BigDecimal.valueOf(16_000L)));
        product.changePrice(BigDecimal.valueOf(17_000L));
        assertThat(product.getPriceValue()).isEqualTo(BigDecimal.valueOf(17_000L));
    }
}
