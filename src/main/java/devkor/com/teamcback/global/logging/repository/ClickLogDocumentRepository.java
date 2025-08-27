package devkor.com.teamcback.global.logging.repository;

import devkor.com.teamcback.global.logging.document.ClickLogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ClickLogDocumentRepository extends ElasticsearchRepository<ClickLogDocument, String> {
}
