package jpabook.jpashop_2.service;

import jakarta.persistence.EntityManager;
import jpabook.jpashop_2.domain.Item.Book;
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

        // TX commit
    }
}
