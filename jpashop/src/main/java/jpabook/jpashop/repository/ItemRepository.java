package jpabook.jpashop.repository;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Item.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    public void save(Item item) {
        // item은 처음에는 id가 없다. 데이터를 저장할 때는. 그래서 JPA가 제공하는 persist를 쓸 것이다.
        // 그렇지 않으면 em.merge()를 쓴다. (update 비슷함)
        // id 값이 없다? -> 완전히 새로 생성한 객체라는 의미
        // em.merge() -> DB에 한 번 등록된 것을 가져오는 것
        // 자세한 건 웹 어플리케이션에서 설명
        // 지금은 그냥 save 또는 update라고 이해하자.
        if (item.getId() == null) {
            em.persist(item);
        } else {
            em.merge(item);
        }
    }

    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i", Item.class)
                .getResultList();
    }
}
