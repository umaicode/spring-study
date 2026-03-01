package jpabook.jpashop.domain;

import jakarta.persistence.*;
import jpabook.jpashop.domain.Item.Item;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Category {

    @Id @GeneratedValue
    @Column(name = "category_id")
    private Long id;

    private String name;

    // 카테고리도 리스트로 아이템을 가지고 아이템도 리스트로 카테고리를 가진다. -> ManyToMany
    // 다대다 관계도 연관관계의 주인을 정해야 한다.
    // JoinTable이 필요하다.
    // 카테고리 아이템 그 중간 테이블 맵핑을 해줘야 한다.
    // 객체는 다 컬렉션, 컬렉션이 있어서 다대다 관계가 가능한데 관계형 DB는 컬렉션 관계를 양쪽에 가질 수 있는게 아니기 때문에
    // 일대다 다대일로 풀어내는 중간 테이블이 있어야 가능하다.
    // [회원 테이블 분석] 에서 CATEGORY, ITEM 사이에 CATEGORY_ITEM이 존재한다!
    // 근데 왜 실전에서 쓰지 말라할까?
    // -> 더 필드를 추가하는 것이 불가능하다. [회원 테이블 분석]에 그림까지만 가능하다.
    // -> 실무에서는 거의 못쓴다. 실무에서는 단순하게 맵핑하고 끝나는 경우가 없다.
    // JoinColumn은 뭐냐? -> 중간 테이블에 있는 category_id
    // InverseJoinColumn은 또 뭐냐? -> category_item 테이블에 아이템 쪽으로 들어가는 번호를 맵핑
    // Q. foreign key 꼭 필요한가요?
    // A. 상황마다 다르다.
    // A. 실시간 트래픽이 엄청 중요하고 정합성보다는 잘 서비스되는게 좀 더 유연한게 중요하면 foreign key 빼고 인덱스만 잘 잡아주면 된다.
    // A. 데이터가 항상 맞아야 되라고 한다면 foreign key를 거는 것에 대해서 진지하게 고민해 볼 필요가 있다.
    @ManyToMany
    @JoinTable(name = "category_item",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id"))
    private List<Item> items = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    // 셀프로 양방향 연관관계를 건다?
    @OneToMany(mappedBy = "parent")
    private List<Category> child = new ArrayList<>();
}
