package devkor.com.teamcback.global.logging.document;

import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;

@Getter
@Document(indexName = "click-log")
public class ClickLogDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String buildingName;

    @Field(type = FieldType.Keyword)
    private String placeName;

    @Field(type = FieldType.Double)
    private Double floor;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private ZonedDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String traceId;

    public ClickLogDocument(String buildingName, String placeName, Double floor, String categoryName) {
        this.buildingName = buildingName;
        this.placeName = placeName;
        this.floor = floor;
        this.categoryName = categoryName;
        this.createdAt = ZonedDateTime.now();
        this.traceId = MDC.get("traceId");
    }
}
