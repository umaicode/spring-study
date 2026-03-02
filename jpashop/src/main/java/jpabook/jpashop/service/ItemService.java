package jpabook.jpashop.service;

import jpabook.jpashop.domain.Item.Book;
import jpabook.jpashop.domain.Item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

    // 1. 변경 감지 기능 사용
    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQuantity) {
        // id를 기반으로 실제 DB에 있는 영속상태 엔티티 찾아오기
        // 이렇게 라인이 끝나면 뭐 작성해야할 코드 필요 없이 스프링의 트랜잭셔널에 의해서 트랜잭션이 커밋이 된다.
        // 커밋이 되면 JPA는 플러시를 날리고 영속성 컨텐츠 중 변경된 걸 다 찾고 업데이트 쿼리를 DB에 날려서 업데이트 친다.
        // 조금 귀찮더라도 merge보다는 내가 직접 이걸 조회해서 내가 업데이트 칠 필드들만 이렇게 set, set, set해서 반환해야 한다.
        // 실무는 되게 복잡해서 merge로 깔끔하게 할 수 있는 경우가 없다. 제약이 반드시 생긴다. 10개 다 수정이 아니라 4,5개만 수정해야 한다든지.
        // 머리 속에 merge를 쓰지 않는다로 가져가는게 좋다.
        // 근데 보통 업데이트는 단발성으로 하면 안된다. 예제니깐 이렇게 한거지
        // findItem.change(price, name, stockQuantity);
        // findItem.addStock();
        // 등등의 의미있는 메서드를 만들어야지 이렇게 set, set, set하는 것도 안좋다.
        // 이렇게 해야 변경 지점이 엔티티로 다 간다.
        // 이렇게 막 set, set, set 하면 사람들이 도대체 어디서 바꾸는거야 한참 뒤져야 한다.
        Item findItem = itemRepository.findOne(itemId);
        findItem.setName(name);
        findItem.setPrice(price);
        findItem.setStockQuantity(stockQuantity);
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }
}
