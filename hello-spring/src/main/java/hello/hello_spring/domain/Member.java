package hello.hello_spring.domain;

import jakarta.persistence.*;

// jpa를 사용하기 위해서는 entity로 맵핑을 해야 한다.
// jpa는 간단하게 말하면 인터페이스다.
// 우리는 보통 jpa 인터페이스에 hibernate만 쓰게 된다.
@Entity
public class Member {

    // DB가 id를 자동으로 생성해주는 것을 identity라고 한다.
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 만약 DB의 column명이 username이라면?
    // @Column(name = "username")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
