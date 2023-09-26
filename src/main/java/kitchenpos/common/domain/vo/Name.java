package kitchenpos.common.domain.vo;

import kitchenpos.common.domain.client.PurgomalumClient;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Name {
    @Column(name = "name", nullable = false)
    private String name;

    protected Name() {
    }

    private Name(final String name) {
        this.name = name;
    }

    public static Name of(final String name, PurgomalumClient menuPurgomalumClient) {
        validateNullOrEmpty(name);
        validateProfanity(name, menuPurgomalumClient);
        return new Name(name);
    }

    public static Name of(final String name) {
        validateNullOrEmpty(name);
        return new Name(name);
    }

    private static void validateNullOrEmpty(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("메뉴명이 없습니다.");
        }
    }

    private static void validateProfanity(String name, PurgomalumClient menuPurgomalumClient) {
        if (menuPurgomalumClient.containsProfanity(name)) {
            throw new IllegalArgumentException("메뉴명에 비속어가 포함되었습니다.");
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Name that = (Name) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
