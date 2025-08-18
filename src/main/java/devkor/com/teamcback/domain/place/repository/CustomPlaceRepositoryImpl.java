package devkor.com.teamcback.domain.place.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static devkor.com.teamcback.domain.place.entity.QPlace.place;

@Repository
@RequiredArgsConstructor
public class CustomPlaceRepositoryImpl implements CustomPlaceRepository{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Place> getFacilitiesByBuildingAndTypesWithPage(Long buildingId, List<PlaceType> mainFacilityTypes, Place lastPlace, int size) {
        return jpaQueryFactory
            .selectFrom(place)
            .where(
                place.building.id.eq(buildingId),
                place.type.in(mainFacilityTypes),
                gtPlaceCursor(lastPlace)
            )
            .orderBy(
                place.floor.asc(),
                place.id.asc()
            )
            .limit(size)
            .fetch();
    }

    private BooleanExpression gtPlaceCursor(Place lastPlace) {
        if (lastPlace== null) return null;
        return place.floor.gt(lastPlace.getFloor())
            .or(place.floor.eq(lastPlace.getFloor()).and(place.id.gt(lastPlace.getId())));
    }
}
