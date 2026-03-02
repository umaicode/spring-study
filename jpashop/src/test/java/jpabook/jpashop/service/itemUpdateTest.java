package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Item.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class itemUpdateTest {

    @Autowired
    EntityManager em;

    @Test
    public void updateTest() throws Exception {
        Book book = em.find(Book.class, 1L);

        // TX
        book.setName("asdfasdf");

        // 변경감지 == dirty checking
        // 우리가 cancel() 로직에서 엔티티의 값을 바꿔놓으면 그냥 JPA가 트랜잭션 커밋 시점에 바뀐데 찾아가지고 db 업데이트 문 날리고 커밋한다.
        // 즉, 플러시 할 때 dirty checking이 일어난다.
        // 근데 문제는 준영속 시점에서 나타난다.
        // 우리의 item 수정할 때 나타난다.
        // book이 새로운 객체이긴 한데 JPA 한번 들어갔다 온 친구라는 것이다.
        // 이렇게 데이터베이스에 정확하게 갔다 온 상태로 식별자가 정확하게 데이터베이스에 있으면 이런 거를 준영속성 상태의 객체라고 한다.
        // 왜? -> 얘는 JPA가 식별할 수 있는 아이디를 가지고 있으니깐. 과거에 그 식별자로 JPA가 관리했으니까.
        // 그럼 이 준영속 엔티티의 문제가 뭐냐?
        // -> JPA가 관리를 안한다. JPA가 관리하는 엔티티는 변경 감지가 일어나는데 updateItem의 book은 new로 만들어졌고 JPA가 기본적으로 관리하지 않는다.
        // -> 내가 아무리 updateItem에서 값을 북에다가 바꿔치기를 해도 DB에 업데이트가 일어나지 않는다. (itemSerice.saveItem(book)이 없을 때)
        // 그럼 이 준영속 상태의 엔티티는 어떻게 변경할 수 있을까?
        // 1. 변경 감지 기능 사용(dirty checking)
        // 2. 병함(merge) 사용
        // 병합의 경우 Item merge = em.merge(item);이 될텐데 여기서 Item merge가 영속성 컨텍스트에서 관리되는 객체이고 merge(item)의 item은 영속 상태로 변하지 않는다. 그냥 다른 애다.
        // 나중에 쓸일 있으면 Item merge를 써야 한다.
        // TX commit
    }
}
