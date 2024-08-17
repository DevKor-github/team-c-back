package devkor.com.teamcback.domain.classroom.controller;

import devkor.com.teamcback.domain.classroom.dto.request.CreateClassroomReq;
import devkor.com.teamcback.domain.classroom.dto.request.ModifyClassroomReq;
import devkor.com.teamcback.domain.classroom.dto.response.CreateClassroomRes;
import devkor.com.teamcback.domain.classroom.dto.response.DeleteClassroomRes;
import devkor.com.teamcback.domain.classroom.dto.response.GetClassroomListRes;
import devkor.com.teamcback.domain.classroom.dto.response.ModifyClassroomRes;
import devkor.com.teamcback.domain.classroom.service.AdminClassroomService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/classrooms")
public class AdminClassroomController {
    private final AdminClassroomService adminClassroomService;

    @GetMapping
    @Operation(summary = "건물 id와 층으로 교실 리스트 검색",
        description = "교실 list 반환")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetClassroomListRes> getClassroomList(
        @Parameter(name = "buildingId", description = "건물 ID") @RequestParam Long buildingId,
        @Parameter(name = "floor", description = "건물 층 수") @RequestParam Double floor) {
        return CommonResponse.success(adminClassroomService.getClassroomList(buildingId, floor));
    }

    @PostMapping
    @Operation(summary = "교실 생성",
        description = "교실 생성")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "건물을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<CreateClassroomRes> createClassroom(
        @Parameter(description = "교실 생성 요청 dto") @Valid @RequestBody CreateClassroomReq req) {
        return CommonResponse.success(adminClassroomService.createClassroom(req));
    }

    @PutMapping("/{classroomId}")
    @Operation(summary = "교실 수정",
        description = "교실 수정")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "객체를 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<ModifyClassroomRes> modifyClassroom(
        @Parameter(description = "수정할 교실 ID") @PathVariable Long classroomId,
        @Parameter(description = "교실 수정 요청 dto") @Valid @RequestBody ModifyClassroomReq req) {
        return CommonResponse.success(adminClassroomService.modifyClassroom(classroomId, req));
    }

    @DeleteMapping("/{classroomId}")
    @Operation(summary = "교실 삭제",
        description = "교실 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "교실을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeleteClassroomRes> deleteNode(
        @Parameter(description = "삭제할 노드 ID") @PathVariable Long classroomId) {
        return CommonResponse.success(adminClassroomService.deleteClassroom(classroomId));
    }


}
