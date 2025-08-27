package devkor.com.teamcback.global.logging.repository;

import devkor.com.teamcback.global.logging.document.SearchLogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SearchLogDocumentRepository extends ElasticsearchRepository<SearchLogDocument, String> {
}
