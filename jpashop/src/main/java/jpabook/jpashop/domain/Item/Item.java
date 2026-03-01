package jpabook.jpashop.domain.Item;

import jakarta.persistence.*;
import jpabook.jpashop.domain.Category;
import jpabook.jpashop.exception.NotEnoughStockException;
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

    // === 비즈니스 로직 === //
    // 객체 지향적으로 생각을 해보면 데이터를 가지고 있는 쪽에 비즈니스 메서드를 있는게 가장 좋다.
    // stockQuantity를 item entity가 가지고 있기 때문에 이곳에 있는게 가장 관리하기 좋다.
    // 편의를 위해서 @Setter를 넣었는데 stockQuantity를 변경해야 될 일이 있으면 이런 식으로 핵심 비즈니스 메서드를 가지고 변경을 해야 되는 것이다.
    // Setter를 가지고 stockQuantity를 가져와서 여기서 어떻게 바깥에서 계산해서 넣는게 아니라 이 안에서 메서드 돌리고 검증하면 된다.
    // -> 이것이 객체 지향적인 것이다.
    /**
     * stock 증가
     */
    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    /**
     * stock 감소
     */
    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }
}
