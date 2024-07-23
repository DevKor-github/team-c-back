package devkor.com.teamcback.domain.admin.classroom.service;

import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_CLASSROOM;
import static devkor.com.teamcback.global.response.ResultCode.NOT_FOUND_CLASSROOM_NICKNAME;

import devkor.com.teamcback.domain.admin.classroom.dto.request.SaveClassroomNicknameReq;
import devkor.com.teamcback.domain.admin.classroom.dto.response.DeleteClassroomNicknameRes;
import devkor.com.teamcback.domain.admin.classroom.dto.response.GetClassroomNicknameListRes;
import devkor.com.teamcback.domain.admin.classroom.dto.response.GetClassroomNicknameRes;
import devkor.com.teamcback.domain.admin.classroom.dto.response.SaveClassroomNicknameRes;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.entity.ClassroomNickname;
import devkor.com.teamcback.domain.classroom.repository.ClassroomNicknameRepository;
import devkor.com.teamcback.domain.classroom.repository.ClassroomRepository;
import devkor.com.teamcback.global.exception.GlobalException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminClassroomNicknameService {
    private final ClassroomNicknameRepository classroomNicknameRepository;
    private final ClassroomRepository classroomRepository;

    // 강의실 별명 저장
    @Transactional
    public SaveClassroomNicknameRes saveClassroomNickname(Long classroomId, SaveClassroomNicknameReq req) {
        Classroom classroom = findClassroom(classroomId);
        ClassroomNickname classroomNickname = new ClassroomNickname(classroom, req.getNickname());

        classroomNicknameRepository.save(classroomNickname);

        return new SaveClassroomNicknameRes();
    }

    // 강의실 별명 삭제
    @Transactional
    public DeleteClassroomNicknameRes deleteClassroomNickname(Long nicknameId) {
        ClassroomNickname classroomNickname = findClassroomNickname(nicknameId);

        classroomNicknameRepository.delete(classroomNickname);

        return new DeleteClassroomNicknameRes();
    }

    // 강의실 별명 조회
    @Transactional(readOnly = true)
    public GetClassroomNicknameListRes getClassroomNickname(Long classroomId) {
        Classroom classroom = findClassroom(classroomId);
        List<GetClassroomNicknameRes> nicknameList = classroomNicknameRepository.findAllByClassroom(classroom)
            .stream().map(classroomNickname -> new GetClassroomNicknameRes(classroomNickname)).toList();


        return new GetClassroomNicknameListRes(classroom, nicknameList);
    }

    private Classroom findClassroom(Long classroomId) {
        return classroomRepository.findById(classroomId).orElseThrow(() -> new GlobalException(NOT_FOUND_CLASSROOM));
    }

    private ClassroomNickname findClassroomNickname(Long nicknameId) {
        return classroomNicknameRepository.findById(nicknameId).orElseThrow(() -> new GlobalException(NOT_FOUND_CLASSROOM_NICKNAME));
    }
}
