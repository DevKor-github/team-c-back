package devkor.com.teamcback.domain.admin.route.controller;

import devkor.com.teamcback.domain.admin.route.dto.request.CreateNodeReq;
import devkor.com.teamcback.domain.admin.route.dto.request.ModifyNodeReq;
import devkor.com.teamcback.domain.admin.route.dto.response.CreateNodeRes;
import devkor.com.teamcback.domain.admin.route.dto.response.DeleteNodeRes;
import devkor.com.teamcback.domain.admin.route.dto.response.GetNodeDetailRes;
import devkor.com.teamcback.domain.admin.route.dto.response.GetNodeListRes;
import devkor.com.teamcback.domain.admin.route.dto.response.ModifyNodeRes;
import devkor.com.teamcback.domain.admin.route.service.AdminRouteService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/routes")
public class AdminRouteController {
    private final AdminRouteService adminRouteService;

    @GetMapping
    @Operation(summary = "건물 id와 층으로 노드 리스트 및 건물 지도 검색",
        description = "건물 id, 건물명, 층수, 건물 내부 지도, 노드 리스트 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetNodeListRes> getNodeList(
        @Parameter(name = "buildingId", description = "건물 ID") @RequestParam Long buildingId,
        @Parameter(name = "floor", description = "건물 층 수") @RequestParam Double floor) {
        return CommonResponse.success(adminRouteService.getNodeList(buildingId, floor));
    }

    @GetMapping("/{nodeId}")
    @Operation(summary = "노드 상세 조회",
        description = "노드 id로 노드 정보 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "노드를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetNodeDetailRes> getNode(
        @Parameter(description = "조회할 노드 ID") @PathVariable Long nodeId
    ) {
        return CommonResponse.success(adminRouteService.getNode(nodeId));
    }

    @PostMapping
    @Operation(summary = "노드 생성",
        description = "노드 생성")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<CreateNodeRes> createNode(
        @Parameter(description = "노드 생성 요청 dto") @Valid @RequestBody CreateNodeReq req) {
        return CommonResponse.success(adminRouteService.createNode(req));
    }

    @PutMapping("/{nodeId}")
    @Operation(summary = "노드 수정",
        description = "노드 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "객체를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<ModifyNodeRes> modifyNodeRes(
        @Parameter(description = "수정할 노드 ID") @PathVariable Long nodeId,
        @Parameter(description = "노드 수정 요청 dto") @Valid @RequestBody ModifyNodeReq req
    ) {
        return CommonResponse.success(adminRouteService.modifyNode(nodeId, req));
    }

    @DeleteMapping("/{nodeId}")
    @Operation(summary = "노드 삭제",
        description = "노드 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "노드를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeleteNodeRes> deleteNode(
        @Parameter(description = "수정할 노드 ID") @PathVariable Long nodeId
    ) {
        return CommonResponse.success(adminRouteService.deleteNode(nodeId));
    }
}
