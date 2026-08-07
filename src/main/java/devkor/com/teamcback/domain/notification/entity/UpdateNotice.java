package devkor.com.teamcback.domain.notification.entity;

import devkor.com.teamcback.domain.common.entity.BaseEntity;
import devkor.com.teamcback.domain.notification.entity.type.UpdateNoticeStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "tb_update_notice",
        indexes = {
                @Index(
                        name = "idx_update_notice_published_at",
                        columnList = "published_at"
                ),
                @Index(
                        name = "idx_update_notice_show_popup_published_at",
                        columnList = "show_popup, published_at"
                ),
                @Index(
                        name = "idx_update_notice_status_published_at",
                        columnList = "status, published_at"
                )
        }
)
public class UpdateNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "update_notice_id")
    private Long updateNoticeId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tb_update_notice_feature",
            joinColumns = @JoinColumn(name = "update_notice_id")
    )
    @OrderColumn(name = "display_order")
    @Column(name = "feature", nullable = false, length = 500)
    private List<String> features = new ArrayList<>();

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "app_version", nullable = false, length = 40)
    private String appVersion;

    @Column(name = "show_popup", nullable = false)
    private boolean show;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Column(name = "link_label", length = 100)
    private String linkLabel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'PUBLISHED'"
    )
    private UpdateNoticeStatus status = UpdateNoticeStatus.PUBLISHED;

    public UpdateNotice(
            String title,
            String description,
            List<String> features,
            LocalDateTime publishedAt,
            String appVersion,
            boolean show
    ) {
        this(title, description, features, publishedAt, appVersion, show, null, null);
    }

    public UpdateNotice(
            String title,
            String description,
            List<String> features,
            LocalDateTime publishedAt,
            String appVersion,
            boolean show,
            String linkUrl,
            String linkLabel
    ) {
        this.title = title;
        this.description = description;
        this.features = features == null ? new ArrayList<>() : new ArrayList<>(features);
        this.publishedAt = publishedAt;
        this.appVersion = appVersion;
        this.show = show;
        this.linkUrl = linkUrl;
        this.linkLabel = linkLabel;
        this.status = UpdateNoticeStatus.PUBLISHED;
    }

    public UpdateNotice(
            String title,
            String description,
            List<String> features,
            LocalDateTime publishedAt,
            String appVersion,
            boolean show,
            String linkUrl,
            String linkLabel,
            UpdateNoticeStatus status
    ) {
        this.title = title;
        this.description = description;
        this.features = features == null ? new ArrayList<>() : new ArrayList<>(features);
        this.publishedAt = publishedAt;
        this.appVersion = appVersion;
        this.show = show;
        this.linkUrl = linkUrl;
        this.linkLabel = linkLabel;
        this.status = status == null ? UpdateNoticeStatus.DRAFT : status;
    }

    public void update(
            String title,
            String description,
            List<String> features,
            LocalDateTime publishedAt,
            String appVersion,
            boolean show,
            String linkUrl,
            String linkLabel,
            UpdateNoticeStatus status
    ) {
        this.title = title;
        this.description = description;
        this.features.clear();
        if (features != null) {
            this.features.addAll(features);
        }
        this.publishedAt = publishedAt;
        this.appVersion = appVersion;
        this.show = show;
        this.linkUrl = linkUrl;
        this.linkLabel = linkLabel;
        this.status = status;
    }

    public void archive() {
        this.status = UpdateNoticeStatus.ARCHIVED;
        this.show = false;
    }
}
