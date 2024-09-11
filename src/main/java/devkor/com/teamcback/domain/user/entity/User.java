package devkor.com.teamcback.domain.user.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.user.dto.request.LoginUserReq;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_user")
@NoArgsConstructor
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(nullable = false)
    private Long score;

    public User(String username, String email, Role role, Provider provider, Long score) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.provider = provider;
        this.score = score;
    }

    public User(String username, String email, Role role, Provider provider) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.provider = provider;
        this.score = 0L;
    }


    public void update(String username) {
        this.username = username;
    }
}
