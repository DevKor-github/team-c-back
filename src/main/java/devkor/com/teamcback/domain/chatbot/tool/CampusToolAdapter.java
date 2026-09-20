package devkor.com.teamcback.domain.chatbot.tool;

import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.AMBIGUOUS_LOCATION;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.INVALID_INPUT;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.NOT_FOUND;
import static devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode.TEMPORARILY_UNAVAILABLE;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_BUILDING;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_PLACE;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolError;
import devkor.com.teamcback.domain.chatbot.tool.dto.CampusToolErrorCode;
import devkor.com.teamcback.domain.chatbot.tool.dto.FacilityToolItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.FindFacilitiesToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.GetLocationDetailToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.LocationDetailToolData;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusItem;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolRequest;
import devkor.com.teamcback.domain.chatbot.tool.dto.SearchCampusToolResult;
import devkor.com.teamcback.domain.chatbot.tool.dto.ToolLocationType;
import devkor.com.teamcback.domain.common.LocationType;
import devkor.com.teamcback.domain.search.dto.response.GlobalSearchRes;
import devkor.com.teamcback.domain.search.dto.response.SearchBuildingDetailRes;
import devkor.com.teamcback.domain.search.dto.response.SearchFacilityRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceDetailRes;
import devkor.com.teamcback.domain.search.dto.response.SearchPlaceRes;
import devkor.com.teamcback.domain.search.dto.response.SearchRoomDetailRes;
import devkor.com.teamcback.domain.search.service.SearchService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class CampusToolAdapter {
    private final SearchService searchService;
    private final ChatbotProperties properties;

    public CampusToolAdapter(SearchService searchService, ChatbotProperties properties) {
        this.searchService = searchService;
        this.properties = properties;
    }

    public SearchCampusToolResult searchCampus(SearchCampusToolRequest request) {
        if (request == null || request.query() == null || request.query().trim().isEmpty()) {
            return new SearchCampusToolResult(List.of(), false, error(INVALID_INPUT));
        }
        Integer limit = resolveLimit(request.limit(), properties.tools().search());
        if (limit == null) {
            return new SearchCampusToolResult(List.of(), false, error(INVALID_INPUT));
        }

        try {
            List<GlobalSearchRes> actualLocations = searchService.globalSearch(request.query().trim(), null).getList()
                    .stream()
                    .filter(item -> item.getId() != null)
                    .filter(item -> item.getLocationType() == LocationType.BUILDING
                            || item.getLocationType() == LocationType.PLACE)
                    .toList();
            List<SearchCampusItem> candidates = actualLocations.stream()
                    .limit(limit)
                    .map(this::toSearchItem)
                    .toList();
            if (candidates.isEmpty()) {
                return new SearchCampusToolResult(List.of(), false, error(NOT_FOUND));
            }
            boolean ambiguous = actualLocations.size() > 1;
            return new SearchCampusToolResult(candidates, ambiguous,
                    ambiguous ? error(AMBIGUOUS_LOCATION) : null);
        } catch (GlobalException exception) {
            return new SearchCampusToolResult(List.of(), false, mapDomainError(exception));
        } catch (RuntimeException exception) {
            return new SearchCampusToolResult(List.of(), false, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    public GetLocationDetailToolResult getLocationDetail(GetLocationDetailToolRequest request) {
        if (request == null || request.locationType() == null
                || request.locationId() == null || request.locationId() <= 0) {
            return new GetLocationDetailToolResult(null, error(INVALID_INPUT));
        }
        try {
            LocationDetailToolData data = switch (request.locationType()) {
                case BUILDING -> toBuildingDetail(searchService.searchBuildingDetail(null, request.locationId()));
                case PLACE -> toPlaceDetail(searchService.searchPlaceDetail(null, request.locationId()));
            };
            return new GetLocationDetailToolResult(data, null);
        } catch (GlobalException exception) {
            return new GetLocationDetailToolResult(null, mapDomainError(exception));
        } catch (RuntimeException exception) {
            return new GetLocationDetailToolResult(null, error(TEMPORARILY_UNAVAILABLE));
        }
    }

    public FindFacilitiesToolResult findFacilities(FindFacilitiesToolRequest request) {
        if (!validFacilityRequest(request)) {
            return new FindFacilitiesToolResult(List.of(), error(INVALID_INPUT));
        }
        Integer limit = resolveLimit(request.limit(), properties.tools().facilities());
        if (limit == null) {
            return new FindFacilitiesToolResult(List.of(), error(INVALID_INPUT));
        }

        try {
            List<FacilityToolItem> facilities;
            if (request.facilityType() != null && request.buildingId() == null) {
                facilities = searchService.searchFacilitiesWithType(request.facilityType().toPlaceType())
                        .getFacilities().stream().map(this::toFacilityItem).toList();
            } else if (request.facilityType() != null) {
                facilities = flattenBuildingFacilities(request);
            } else {
                facilities = searchService.searchPlaceByBuildingFloor(request.buildingId(), request.floor())
                        .getRoomList().stream()
                        .map(item -> toFloorItem(item, request.buildingId(), request.floor()))
                        .toList();
            }
            facilities = facilities.stream().limit(limit).toList();
            if (facilities.isEmpty()) {
                return new FindFacilitiesToolResult(List.of(), error(NOT_FOUND));
            }
            return new FindFacilitiesToolResult(facilities, null);
        } catch (GlobalException exception) {
            return new FindFacilitiesToolResult(List.of(), mapDomainError(exception));
        } catch (RuntimeException exception) {
            return new FindFacilitiesToolResult(List.of(), error(TEMPORARILY_UNAVAILABLE));
        }
    }

    private boolean validFacilityRequest(FindFacilitiesToolRequest request) {
        if (request == null) {
            return false;
        }
        if (request.buildingId() != null && request.buildingId() <= 0) {
            return false;
        }
        if (request.floor() != null && request.buildingId() == null) {
            return false;
        }
        if (request.facilityType() == null && request.buildingId() == null) {
            return false;
        }
        return request.facilityType() != null || request.floor() != null;
    }

    private List<FacilityToolItem> flattenBuildingFacilities(FindFacilitiesToolRequest request) {
        Map<Double, List<SearchFacilityRes>> byFloor = searchService.searchBuildingFacilityByType(
                request.buildingId(), request.facilityType().toPlaceType()).getFacilities();
        return byFloor.entrySet().stream()
                .filter(entry -> request.floor() == null
                        || Double.compare(entry.getKey(), request.floor().doubleValue()) == 0)
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .flatMap(entry -> entry.getValue().stream())
                .map(this::toBuildingFacilityItem)
                .toList();
    }

    private SearchCampusItem toSearchItem(GlobalSearchRes item) {
        ToolLocationType type = item.getLocationType() == LocationType.BUILDING
                ? ToolLocationType.BUILDING : ToolLocationType.PLACE;
        Long buildingId = type == ToolLocationType.BUILDING ? item.getId() : item.getBuildingId();
        String buildingName = type == ToolLocationType.BUILDING ? item.getName() : null;
        return new SearchCampusItem(item.getId(), type, item.getName(), buildingId, buildingName,
                item.getFloor(), item.getPlaceType(), normalizeDetail(item.getDetail()));
    }

    private LocationDetailToolData toBuildingDetail(SearchBuildingDetailRes detail) {
        return new LocationDetailToolData(detail.getBuildingId(), ToolLocationType.BUILDING,
                detail.getName(), detail.getBuildingId(), detail.getName(), null, detail.getAddress(),
                normalizeDetail(detail.getDetails()), null, detail.isOperating(), detail.getNextBuildingTime(),
                detail.getWeekdayOperatingTime(), detail.getSaturdayOperatingTime(),
                detail.getSundayOperatingTime(), null, null, null, null);
    }

    private LocationDetailToolData toPlaceDetail(SearchPlaceDetailRes detail) {
        return new LocationDetailToolData(detail.getPlaceId(), ToolLocationType.PLACE, detail.getName(),
                detail.getBuildingId(), null, detail.getFloor(), null, normalizeDetail(detail.getDetail()),
                detail.getPlaceType(), detail.isOperating(), detail.getNextPlaceTime(),
                detail.getWeekdayOperatingTime(), detail.getSaturdayOperatingTime(),
                detail.getSundayOperatingTime(), detail.isAvailability(), detail.isPlugAvailability(),
                null, parseMeaningfulRating(detail.getStarAverage()));
    }

    private FacilityToolItem toFacilityItem(SearchPlaceRes item) {
        return new FacilityToolItem(item.getId(), item.getName(), item.getPlaceType(), item.getBuildingId(),
                item.getBuildingName(), toInteger(item.getFloor()), normalizeDetail(item.getDetail()),
                item.isOperating(), null, item.getPlugAvailability());
    }

    private FacilityToolItem toBuildingFacilityItem(SearchFacilityRes item) {
        return new FacilityToolItem(item.getId(), item.getName(), item.getPlaceType(), item.getBuildingId(),
                null, item.getFloor(), normalizeDetail(item.getDetail()), item.isOperating(),
                item.getAvailability(), null);
    }

    private FacilityToolItem toFloorItem(SearchRoomDetailRes item, Long buildingId, Integer floor) {
        return new FacilityToolItem(item.getId(), item.getName(), item.getPlaceType(), buildingId, null, floor,
                normalizeDetail(item.getDetail()), item.isOperating(), item.isAvailability(),
                item.isPlugAvailability());
    }

    private Integer resolveLimit(Integer requested, ChatbotProperties.Limits limits) {
        if (requested != null && requested <= 0) {
            return null;
        }
        int value = requested == null ? limits.defaultLimit() : requested;
        return Math.min(value, limits.maxLimit());
    }

    private CampusToolError mapDomainError(GlobalException exception) {
        if (exception.getResultCode() == NOT_FOUND_BUILDING || exception.getResultCode() == NOT_FOUND_PLACE) {
            return error(NOT_FOUND);
        }
        return error(TEMPORARILY_UNAVAILABLE);
    }

    private CampusToolError error(CampusToolErrorCode code) {
        return CampusToolError.of(code);
    }

    private String normalizeDetail(String detail) {
        return detail == null || detail.isBlank() || ".".equals(detail) ? null : detail;
    }

    private Integer toInteger(Double floor) {
        return floor == null ? null : floor.intValue();
    }

    private Double parseMeaningfulRating(String rating) {
        if (rating == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(rating);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
