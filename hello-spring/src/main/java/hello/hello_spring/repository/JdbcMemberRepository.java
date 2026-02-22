package hello.hello_spring.repository;

import hello.hello_spring.domain.Member;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 회원을 저장한다는 역할은 지금 MemberRepository가 그 역할을 하지만
// 구현을 메모리에 할거야 or 나는 DB랑 연동해서 JDBC로 할거야 차이!
public class JdbcMemberRepository implements MemberRepository {

    // DB에 붙으려면 데이터 소스라는 것이 필요하다.
    private final DataSource dataSource;

    // 나중에 주입받아야 한다 -> 어디서? -> 스프링에서!
    public JdbcMemberRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Member save(Member member) {
        // 변수보다 상수로 빼는 것이 낫다.
        String sql = "insert into member(name) values(?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        // ResultSet : 결과를 받는 것
        ResultSet rs = null;

        try {
            conn = getConnection();
            // RETURN_GENERATED_KEYS
            // DB에 인서트 하면 인서트를 해봐야 1번, 2번 등 id 값을 얻을 수 있다.
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // parameterIndex 1 -> values의 (?)랑 매칭이 된다.
            pstmt.setString(1, member.getName());

            // DB에 실제 쿼리가 이 때 날아간다.
            pstmt.executeUpdate();

            // getGeneratedKeys() -> Statement.RETURN_GENERATED_KEYS랑 매칭된다.
            rs = pstmt.getGeneratedKeys();

            if (rs.next()) {
                member.setId(rs.getLong(1));
            } else {
                throw new SQLException("id 조회 실패");
            }
            return member;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            // 이거 안하면 데이터베이스 커넥션 계속 쌓여서 대장애 발생할 수도 있다.
            close(conn, pstmt, rs);
        }
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "select * from member where id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, id);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                Member member = new Member();
                member.setId(rs.getLong("id"));
                member.setName(rs.getString("name"));
                return Optional.of(member);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            close(conn, pstmt, rs);
        }
    }

    @Override
    public List<Member> findAll() {
        String sql = "select * from member";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);

            rs = pstmt.executeQuery();
            List<Member> members = new ArrayList<>();
            while (rs.next()) {
                Member member = new Member();
                member.setId(rs.getLong("id"));
                member.setName(rs.getString("name"));
                members.add(member);
            }

            return members;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            close(conn, pstmt, rs);
        }
    }

    @Override
    public Optional<Member> findByName(String name) {
        String sql = "select * from member where name = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                Member member = new Member();
                member.setId(rs.getLong("id"));
                member.setName(rs.getString("name"));
                return Optional.of(member);
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            close(conn, pstmt, rs);
        }
    }

    private Connection getConnection() {
        // 원래 DataSourceUtils에서 이 커넥션을 획득해야 한다.
        // 이거 안하면 이전 트랜잭션에 걸릴 수도 있다.
        // 같은 데이터베이스 커넥션을 유지시켜 준다!
        // 스프링 프레임워크를 쓸 때는 꼭 이렇게 가져와야 한다.
        return DataSourceUtils.getConnection(dataSource);
    }

    private void close(Connection conn, PreparedStatement pstmt, ResultSet rs)
    {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (pstmt != null) {
                pstmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (conn != null) {
                close(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void close(Connection conn) throws SQLException {
        // 닫을 때도 커넥션은 DataSourceUtils를 통해서 release를 해야 한다.
        DataSourceUtils.releaseConnection(conn, dataSource);
    }
}