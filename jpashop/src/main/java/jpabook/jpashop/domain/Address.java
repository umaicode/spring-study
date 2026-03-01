package jpabook.jpashop.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

// JPA의 어떤 내장 타입이라는 것이기 때문에 @Embeddable 하면 된다. -> 어딘가에 내장이 될 수 있다.
@Embeddable
@Getter
public class Address {

    // 값 타입은 기본적으로 값이라는 것 자체는 변경(immutable)하게 설계되면 안된다.
    // 좋은 설계는 생성할 때만 딱 값이 세팅이 되고 Setter를 아예 제공을 안하는 것이다.
    // -> 완전히 변경이 불가능해진다.
    // -> 만들려면 복사를 하거나 똑같은 객체를 만들어서 값을 무조건 새로 등록해야 된다.
    // JPA는 reflection이나 proxy같은 기술들을 써야 하는데 기본 생성자가 없으면 못한다.
    // -> 기본 생성자를 만들어줘야 한다.
    // -> 근데 public으로 하면 사람들이 많이 호출할거 아니야?
    // -> JPA는 protected까지 허용해준다.

    // Q. 실행하고 나온 생성된 테이블을 그대로 써도 되나요?
    // A. 그대로 쓰면 안된다.
    // A. DDL 스크립트를 참고해서 쓰는 건 괜찮은데 한번 다 보고 검증하고 다듬어야 한다.
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
