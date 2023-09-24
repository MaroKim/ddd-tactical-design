package kitchenpos.menus.application;

import kitchenpos.menus.application.dto.MenuCreateRequest;
import kitchenpos.menus.application.dto.MenuPriceChangeRequest;
import kitchenpos.menus.application.dto.MenuProductCreateRequest;
import kitchenpos.menus.tobe.domain.menu.*;
import kitchenpos.menus.tobe.domain.menugroup.MenuGroupRepository;
import kitchenpos.menus.tobe.intrastructure.ProductClientImpl;
import kitchenpos.products.application.FakeProductEventPublisher;
import kitchenpos.products.application.InMemoryProductRepository;
import kitchenpos.products.application.ProductService;
import kitchenpos.products.application.dto.ProductChangePriceRequest;
import kitchenpos.products.tobe.domain.Product;
import kitchenpos.products.tobe.domain.ProductNamePolicy;
import kitchenpos.products.tobe.domain.ProductNameProfanities;
import kitchenpos.products.tobe.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.*;

import static kitchenpos.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class MenuServiceTest {
    private MenuRepository menuRepository;
    private MenuGroupRepository menuGroupRepository;
    private ProductRepository productRepository;
    private MenuNameProfanities menuNameProfanities;
    private ProductNameProfanities productNameProfanities;
    private MenuNamePolicy menuNamePolicy;
    private MenuService menuService;
    private ProductClient productClient;
    private ProductService productService;
    private UUID menuGroupId;
    private Product product;

    @BeforeEach
    void setUp() {
        menuRepository = new InMemoryMenuRepository();
        menuGroupRepository = new InMemoryMenuGroupRepository();
        productRepository = new InMemoryProductRepository();
        menuNameProfanities = new FakeMenuNameProfanities();
        productClient = new ProductClientImpl(productRepository);
        menuNamePolicy = new MenuNamePolicy(menuNameProfanities);
        menuService = new MenuService(menuRepository, menuGroupRepository, productClient, menuNamePolicy);
        productService = new ProductService(productRepository, new ProductNamePolicy(productNameProfanities), new FakeProductEventPublisher(productRepository, menuRepository));
        menuGroupId = menuGroupRepository.save(menuGroup()).getId();
        product = productRepository.save(product("후라이드", 16_000L));
    }

    @DisplayName("1개 이상의 등록된 Product로 Menu를 등록할 수 있다.")
    @Test
    void create() {
        final MenuCreateRequest expected = createMenuRequest(
                "후라이드+후라이드", 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
        );
        final Menu actual = menuService.create(expected);
        assertThat(actual).isNotNull();
        assertAll(
                () -> assertThat(actual.getId()).isNotNull(),
                () -> assertThat(actual.getNameValue()).isEqualTo(expected.getName(), menuNamePolicy),
                () -> assertThat(actual.getPriceValue()).isEqualTo(expected.getPrice()),
                () -> assertThat(actual.getMenuGroup().getId()).isEqualTo(expected.getMenuGroupId()),
                () -> assertThat(actual.isDisplayed()).isEqualTo(expected.isDisplayed()),
                () -> assertThat(actual.getMenuProducts().getMenuProducts()).hasSize(1)
        );
    }

    @DisplayName("MenuProduct가 비어있거나 Product가 존재하지 않으면 Menu를등록할 수 없다.")
    @MethodSource("menuProducts")
    @ParameterizedTest
    void create(final List<MenuProductCreateRequest> menuProducts) {
        final MenuCreateRequest expected = createMenuRequest("후라이드+후라이드", 19_000L, menuGroupId, true, menuProducts);
        assertThatThrownBy(() -> menuService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Arguments> menuProducts() {
        return Arrays.asList(
                null,
                Arguments.of(Collections.emptyList()),
                Arguments.of(Arrays.asList(createMenuProductRequest(INVALID_ID, 2L)))
        );
    }

    @DisplayName("Menu에 MenuProduct의 Quantity는 1개 이상이어야 한다.")
    @Test
    void createNegativeQuantity() {
        final MenuCreateRequest expected = createMenuRequest(
                "후라이드+후라이드", 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 0)
        );
        assertThatThrownBy(() -> menuService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Menu의 Price가 음수라면 등록할 수 없다.")
    @ValueSource(strings = "-1")
    @NullSource
    @ParameterizedTest
    void create(final BigDecimal price) {
        final MenuCreateRequest expected = createMenuRequest(
                "후라이드+후라이드", price, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
        );
        assertThatThrownBy(() -> menuService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("MenuProduct의 Total Amount는 Menu의 Price보다 크거나 같아야 한다.")
    @Test
    void createExpensiveMenu() {
        final MenuCreateRequest expected = createMenuRequest(
                "후라이드+후라이드", 33_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
        );
        assertThatThrownBy(() -> menuService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Menu는 특정 MenuGroup에 속해야 한다.")
    @NullSource
    @ParameterizedTest
    void create(final UUID menuGroupId) {
        final MenuCreateRequest expected = createMenuRequest(
                "후라이드+후라이드", 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
        );
        assertThatThrownBy(() -> menuService.create(expected))
                .isInstanceOf(NoSuchElementException.class);
    }

    @DisplayName("Menu의 Name에 Profanity가 포함되면 등록할 수 없다.")
    @ValueSource(strings = {"비속어", "욕설이 포함된 이름"})
    @NullSource
    @ParameterizedTest
    void create(final String name) {
        final MenuCreateRequest expected = createMenuRequest(
                name, 19_000L, menuGroupId, true, createMenuProductRequest(product.getId(), 2L)
        );
        assertThatThrownBy(() -> menuService.create(expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Menu의 Price를 변경할 수 있다.")
    @Test
    void changePrice() {
        final UUID menuId = menuRepository.save(menu(19_000L, productRepository, menuProductMaterial(product.getId(), 2L))).getId();
        final MenuPriceChangeRequest expected = changePriceRequest(16_000L);
        final Menu actual = menuService.changePrice(menuId, expected);
        assertThat(actual.getPriceValue()).isEqualTo(expected.getPrice());
    }

    @DisplayName("Menu의 Price는 0미만의 음수로 변경할 수 없다.")
    @ValueSource(strings = "-1")
    @NullSource
    @ParameterizedTest
    void changePrice(final BigDecimal price) {
        final UUID menuId = menuRepository.save(menu(19_000L, productRepository, menuProductMaterial(product.getId(), 2L))).getId();
        final MenuPriceChangeRequest expected = changePriceRequest(price);
        assertThatThrownBy(() -> menuService.changePrice(menuId, expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Menu의 Price 변경 시 MenuProduct의 Total Amount는 Menu의 Price보다 크거나 같아야 한다.")
    @Test
    void changePriceToExpensive() {
        final UUID menuId = menuRepository.save(menu(19_000L, productRepository, menuProductMaterial(product.getId(), 2L))).getId();
        final MenuPriceChangeRequest expected = changePriceRequest(33_000L);
        assertThatThrownBy(() -> menuService.changePrice(menuId, expected))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Menu를 Display할 수 있다.")
    @Test
    void display() {
        final UUID menuId = menuRepository.save(menu(19_000L, false, productRepository, menuProductMaterial(product.getId(), 2L))).getId();
        final Menu actual = menuService.display(menuId);
        assertThat(actual.isDisplayed()).isTrue();
    }

    @DisplayName("Menu의 Price가 MenuProduct의 Total Amount보다 비싼 경우 Display할 수 없다.")
    @Test
    void displayExpensiveMenu() {
        final UUID menuId = menuRepository.save(menu(32_000L, false, productRepository, menuProductMaterial(product.getId(), 2L))).getId();
        productService.changePrice(product.getId(), new ProductChangePriceRequest(product.getId(), product.getNameValue(), BigDecimal.valueOf(15_000L)));
        assertThatThrownBy(() -> menuService.display(menuId))
                .isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("Menu를 Hide할 수 있다.")
    @Test
    void hide() {
        final UUID menuId = menuRepository.save(menu(19_000L, true, productRepository, menuProductMaterial(product.getId(), 2L))).getId();
        final Menu actual = menuService.hide(menuId);
        assertThat(actual.isDisplayed()).isFalse();
    }

    @DisplayName("Menu의 목록을 조회할 수 있다.")
    @Test
    void findAll() {
        menuRepository.save(menu(19_000L, true, productRepository, menuProductMaterial(product.getId(), 2L)));
        final List<Menu> actual = menuService.findAll();
        assertThat(actual).hasSize(1);
    }

    private MenuCreateRequest createMenuRequest(
            final String name,
            final long price,
            final UUID menuGroupId,
            final boolean displayed,
            final MenuProductCreateRequest... menuProducts
    ) {
        return createMenuRequest(name, BigDecimal.valueOf(price), menuGroupId, displayed, menuProducts);
    }

    private MenuCreateRequest createMenuRequest(
            final String name,
            final BigDecimal price,
            final UUID menuGroupId,
            final boolean displayed,
            final MenuProductCreateRequest... menuProducts
    ) {
        return createMenuRequest(name, price, menuGroupId, displayed, Arrays.asList(menuProducts));
    }

    private MenuCreateRequest createMenuRequest(
            final String name,
            final long price,
            final UUID menuGroupId,
            final boolean displayed,
            final List<MenuProductCreateRequest> menuProducts
    ) {
        return createMenuRequest(name, BigDecimal.valueOf(price), menuGroupId, displayed, menuProducts);
    }

    private MenuCreateRequest createMenuRequest(
            final String name,
            final BigDecimal price,
            final UUID menuGroupId,
            final boolean displayed,
            final List<MenuProductCreateRequest> menuProducts
    ) {
        return new MenuCreateRequest(name, price, displayed, menuGroupId, menuProducts);
    }

    private static MenuProductCreateRequest createMenuProductRequest(final UUID productId, final long quantity) {
        return new MenuProductCreateRequest(productId, quantity);
    }

    private MenuPriceChangeRequest changePriceRequest(final long price) {
        return changePriceRequest(BigDecimal.valueOf(price));
    }

    private MenuPriceChangeRequest changePriceRequest(final BigDecimal price) {
        return new MenuPriceChangeRequest(price);
    }
}
