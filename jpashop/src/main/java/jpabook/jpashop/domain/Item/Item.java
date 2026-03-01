package jpabook.jpashop.domain.Item;

import jakarta.persistence.*;
import jpabook.jpashop.domain.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// 추상클래스로 만드는 이유: 구현체를 가지고 갈 것이기 때문에
// InheritanceType
// - joined: 가장 정교화된 스타일
// - Single_Table: 한 테이블에 다 때려박기
// - Table_Per_Class: 북, 무비, 앨범 이렇게 3개의 테이블만 나오는 전략
// => 우리는 싱글 테이블 전략을 선택할 것이다.
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Getter @Setter
public abstract class Item {

    @Id @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<>();
}
