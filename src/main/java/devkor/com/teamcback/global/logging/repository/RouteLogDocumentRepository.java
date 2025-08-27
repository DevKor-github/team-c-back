package devkor.com.teamcback.global.logging.repository;

import devkor.com.teamcback.global.logging.document.RouteLogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RouteLogDocumentRepository extends ElasticsearchRepository<RouteLogDocument, String> {
}
