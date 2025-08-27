package devkor.com.teamcback.global.logging.document;

import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;

@Getter
@Document(indexName = "search-log")
public class SearchLogDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String searchKeyword;

    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private ZonedDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String traceId;

    public SearchLogDocument(String searchKeyword) {
        this.searchKeyword = searchKeyword;
        this.createdAt = ZonedDateTime.now();
        this.traceId = MDC.get("traceId");
    }
}
