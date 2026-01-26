package devkor.com.teamcback.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tb_user_location")
@NoArgsConstructor
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = LocationEncryptionConverter.class)
    private Double latitude; // 위도

    @Convert(converter = LocationEncryptionConverter.class)
    private Double longitude; // 경도

    @Convert(converter = LocationEncryptionConverter.class)
    private Double altitude; // 고도

    @LastModifiedDate
    private LocalDateTime updatedAt; // 마지막 수집 일시

    @OneToOne
    @JoinColumn(name = "user_id") // DB에 user_id FK가 생성됨
    private User user;

}
