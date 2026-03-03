package jpabook.jpashop_2.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

// JPA의 어떤 내장 타입이라는 것이기 때문에 @Embeddable 하면 된다. -> 어딘가에 내장이 될 수 있다.
@Embeddable
@Getter
public class Address {

    private String city;
    private String street;
    private String zipcode;

    protected Address() {
    }

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
