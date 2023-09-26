package kitchenpos.eatinorders.domain.ordertable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class GuestNumber {

    @Column(name = "number_of_guests", nullable = false)
    private int numberOfGuests;

    protected GuestNumber() {
    }

    private GuestNumber(final int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public static GuestNumber of(final int numberOfGuests) {
        validateNumber(numberOfGuests);
        return new GuestNumber(numberOfGuests);
    }

    private static void validateNumber(final int numberOfGuests) {
        if (numberOfGuests < 0) {
            throw new IllegalArgumentException("손님 수는 0명 이상이어야 합니다.");
        }
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuestNumber that = (GuestNumber) o;
        return numberOfGuests == that.numberOfGuests;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfGuests);
    }
}
