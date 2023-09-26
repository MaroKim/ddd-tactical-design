package kitchenpos.products.application;

import kitchenpos.common.domain.vo.Name;
import kitchenpos.common.domain.vo.Price;
import kitchenpos.common.domain.client.PurgomalumClient;
import kitchenpos.menus.application.InMemoryMenuRepository;
import kitchenpos.menus.domain.menu.Menu;
import kitchenpos.menus.domain.menu.MenuRepository;
import kitchenpos.menus.domain.menu.ProductClient;
import kitchenpos.menus.infra.DefaultProductClient;
import kitchenpos.products.application.dto.ProductRequest;
import kitchenpos.products.application.dto.ProductResponse;
import kitchenpos.products.domain.Product;
import kitchenpos.products.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static kitchenpos.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class ProductServiceTest {
    private ProductRepository productRepository;
    private MenuRepository menuRepository;
    private ProductService productService;
    private ProductClient productClient;
    private PurgomalumClient purgomalumClient;
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        menuRepository = new InMemoryMenuRepository();
        productClient = new DefaultProductClient(productRepository);
        purgomalumClient = new FakeProductPurgomalumClient();
        applicationEventPublisher = new FakeApplicationEventPublisher(menuRepository, productClient);
        productService = new ProductService(productRepository, purgomalumClient, applicationEventPublisher);
    }

    @DisplayName("상품을 등록할 수 있다.")
    @Test
    void create() {
        final ProductRequest expected = createProductRequest("후라이드", 16_000L);
        final ProductResponse actual = productService.create(expected);
        assertThat(actual).isNotNull();
        assertAll(
                () -> assertThat(actual.getProductId()).isNotNull(),
                () -> assertThat(actual.getName()).isEqualTo(expected.getName()),
                () -> assertThat(actual.getPrice()).isEqualTo(expected.getPrice())
        );
    }

    @DisplayName("상품의 가격이 올바르지 않으면 등록할 수 없다.")
    @ValueSource(strings = "-1000")
    @NullSource
    @ParameterizedTest
    void create(final BigDecimal price) {
        final ProductRequest expected = createProductRequest("후라이드", price);
        assertThatThrownBy(() -> productService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품의 이름이 올바르지 않으면 등록할 수 없다.")
    @ValueSource(strings = {"비속어", "욕설이 포함된 이름"})
    @NullSource
    @ParameterizedTest
    void create(final String name) {
        final ProductRequest expected = createProductRequest(name, 16_000L);
        assertThatThrownBy(() -> productService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품의 가격을 변경할 수 있다.")
    @Test
    void changePrice() {
        final UUID productId = productRepository.save(product("후라이드", 16_000L)).getId();
        final ProductRequest expected = changePriceRequest(15_000L);
        final ProductResponse actual = productService.changePrice(productId, expected);
        assertThat(actual.getPrice()).isEqualTo(expected.getPrice());
    }

    @DisplayName("상품의 가격이 올바르지 않으면 변경할 수 없다.")
    @ValueSource(strings = "-1000")
    @NullSource
    @ParameterizedTest
    void changePrice(final BigDecimal price) {
        final UUID productId = productRepository.save(product("후라이드", 16_000L)).getId();
        final ProductRequest expected = changePriceRequest(price);
        assertThatThrownBy(() -> productService.changePrice(productId, expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품의 가격이 변경될 때 메뉴의 가격이 메뉴에 속한 상품 금액의 합보다 크면 메뉴가 숨겨진다.")
    @Test
    void changePriceInMenu() {
        final Product product = productRepository.save(product("후라이드", 16_000L));
        final Menu menu = menuRepository.save(menu(32_000L, true, menuProduct(product, 2L)));
        productService.changePrice(product.getId(), changePriceRequest(15_000L));
        assertThat(menuRepository.findById(menu.getId()).get().isDisplayed()).isFalse();
    }

    @DisplayName("상품의 목록을 조회할 수 있다.")
    @Test
    void findAll() {
        productRepository.save(product("후라이드", 16_000L));
        productRepository.save(product("양념치킨", 16_000L));
        final List<ProductResponse> actual = productService.findAll();
        assertThat(actual).hasSize(2);
    }

    @DisplayName("Price VO를 정상 생성한다.")
    @ValueSource(strings = "16000")
    @ParameterizedTest
    void createPriceVo(final BigDecimal price) {
        final Price actual = Price.of(price);
        assertThat(actual.getPrice()).isEqualTo(price);
    }

    @DisplayName("Price VO 생성시 가격이 비어 있거나 0원 미만이면 예외가 발생한다.")
    @ValueSource(strings = "-10000")
    @NullSource
    @ParameterizedTest
    void throwExceptionOfPrice(final BigDecimal price) {
        assertThatThrownBy(() -> Price.of(price))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Name VO를 정상 생성한다.")
    @ValueSource(strings = "간장치킨")
    @ParameterizedTest
    void createNameVo(final String name) {
        final Name actual = Name.of(name, purgomalumClient);
        assertThat(actual.getName()).isEqualTo(name);
    }

    @DisplayName("Name VO 생성시 이름이 비어 있거나 비속어가 포함되어 있으면 예외가 발생한다.")
    @ValueSource(strings = "비속어")
    @NullSource
    @ParameterizedTest
    void throwExceptionOfNameVo(final String name) {
        assertThatThrownBy(() -> Name.of(name, purgomalumClient))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ProductRequest createProductRequest(final String name, final long price) {
        return createProductRequest(name, BigDecimal.valueOf(price));
    }

    private ProductRequest createProductRequest(final String name, final BigDecimal price) {
        return new ProductRequest(name, price);
    }

    private ProductRequest changePriceRequest(final long price) {
        return changePriceRequest(BigDecimal.valueOf(price));
    }

    private ProductRequest changePriceRequest(final BigDecimal price) {
        return new ProductRequest(null, price);
    }
}
