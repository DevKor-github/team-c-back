package devkor.com.teamcback.domain.user.entity;

import devkor.com.teamcback.domain.common.BaseEntity;
import devkor.com.teamcback.domain.user.dto.request.LoginUserReq;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private String profileUrl;

    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(nullable = false)
    private Long score;

    public User(String username, String email, String profileUrl, Role role, Provider provider, Long score) {
        this.username = username;
        this.email = email;
        this.profileUrl = profileUrl;
        this.role = role;
        this.provider = provider;
        this.score = score;
    }

    public User(String username, String email, Role role, Provider provider) {
        this.username = username;
        this.email = email;
        profileUrl = "";
        this.role = role;
        this.provider = provider;
        this.score = 0L;
    }

}
