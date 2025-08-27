package devkor.com.teamcback.global.logging.document;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.routes.entity.Conditions;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Document(indexName = "route-log")
public class RouteLogDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String startBuildingName;

    @Field(type = FieldType.Keyword)
    private String startPlaceName;

    @Field(type = FieldType.Keyword)
    private String endBuildingName;

    @Field(type = FieldType.Keyword)
    private String endPlaceName;

    @Field(type = FieldType.Keyword)
    private List<String> conditions;

    @Field(type = FieldType.Date, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSZZ")
    private ZonedDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String traceId;

    public RouteLogDocument(Building startBuilding, Place startPlace, Building endBuilding, Place endPlace, List<Conditions> conditions) {
        this.startBuildingName = (startBuilding != null) ? startBuilding.getName() : startPlace.getBuilding().getName();
        this.endBuildingName = (endBuilding != null) ? endBuilding.getName() : endPlace.getBuilding().getName();
        if(startPlace != null) this.startPlaceName = startPlace.getName();
        if(endPlace != null) this.endPlaceName = endPlace.getName();
        this.conditions = conditions.stream().map(Enum::name).toList();
        this.createdAt = ZonedDateTime.now();
        this.traceId = MDC.get("traceId");
    }
}
