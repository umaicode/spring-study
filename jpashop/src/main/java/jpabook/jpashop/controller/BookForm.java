package jpabook.jpashop.controller;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookForm {

    // 상품 수정을 위해서는 id가 필요하다.
    // 아래 4개는 상품의 공통 특성
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    // 아래 2개는 책과 관련된 특별한 속성
    private String author;
    private String isbn;
}
