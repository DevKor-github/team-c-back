package devkor.com.teamcback.domain.admin.classroom.controller;

import devkor.com.teamcback.domain.admin.classroom.dto.request.SaveClassroomNicknameReq;
import devkor.com.teamcback.domain.admin.classroom.dto.response.DeleteClassroomNicknameRes;
import devkor.com.teamcback.domain.admin.classroom.dto.response.GetClassroomNicknameListRes;
import devkor.com.teamcback.domain.admin.classroom.dto.response.SaveClassroomNicknameRes;
import devkor.com.teamcback.domain.admin.classroom.service.AdminClassroomNicknameService;
import devkor.com.teamcback.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/classrooms")
public class AdminClassroomNicknameController {
    private final AdminClassroomNicknameService adminClassroomNicknameService;

    @PostMapping("/{classroomId}/nicknames")
    @Operation(summary = "강의실 별명 저장",
        description = "강의실 별명 저장")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "강의실을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<SaveClassroomNicknameRes> saveClassroomNickname(
        @Parameter(name = "classroomId", description = "강의실 ID") @PathVariable Long classroomId,
        @Parameter(description = "강의실 별명 저장 dto") @RequestBody SaveClassroomNicknameReq req) {
        return CommonResponse.success(adminClassroomNicknameService.saveClassroomNickname(classroomId, req));
    }

    @DeleteMapping("/nicknames/{nicknameId}")
    @Operation(summary = "강의실 별명 삭제",
        description = "강의실 별명 삭제")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "강의실 별명을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<DeleteClassroomNicknameRes> deleteClassroomNickname(
        @Parameter(name = "nicknameId", description = "강의실 별명 ID") @PathVariable Long nicknameId) {
        return CommonResponse.success(adminClassroomNicknameService.deleteClassroomNickname(nicknameId));
    }

    @GetMapping("/{classroomId}/nicknames")
    @Operation(summary = "강의실 별명 조회",
        description = "강의실 별명 조회")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 처리 되었습니다."),
        @ApiResponse(responseCode = "404", description = "강의실을 찾을 수 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "권한이 없습니다.",
            content = @Content(schema = @Schema(implementation = CommonResponse.class))),
    })
    public CommonResponse<GetClassroomNicknameListRes> getClassroomNickname(
        @Parameter(name = "classroomId", description = "강의실 ID") @PathVariable Long classroomId) {
        return CommonResponse.success(adminClassroomNicknameService.getClassroomNickname(classroomId));
    }
}
