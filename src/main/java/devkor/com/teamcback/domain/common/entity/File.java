package devkor.com.teamcback.domain.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_file")
@NoArgsConstructor
public class File extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileUuid;

    @Column
    private String fileOriginalName;    // 원본 이름

    @Column(nullable = false)
    private String fileSavedName;       // 저장 이름(url)

    @Column
    private String thumbSavedName;      // 썸네일 저장 이름(url)

    @Column
    private Long fileSize;      // 파일 사이즈

    @Column
    private String extension;   // 파일 확장자

    @Column(nullable = false)
    private Long sortNum;       // 파일 순서

    public File(String fileUuid, String fileOriginalName, String fileSavedName, Long fileSize, String extension, Long sortNum) {
        this.fileUuid = fileUuid;
        this.fileOriginalName = fileOriginalName;
        this.fileSavedName = fileSavedName;
        this.fileSize = fileSize;
        this.extension = extension;
        this.sortNum = sortNum;
    }
}
