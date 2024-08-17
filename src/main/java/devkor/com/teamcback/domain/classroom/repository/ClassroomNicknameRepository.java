package devkor.com.teamcback.domain.classroom.repository;

import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.classroom.entity.ClassroomNickname;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomNicknameRepository extends JpaRepository<ClassroomNickname, Long> {
    List<ClassroomNickname> findByNicknameContainingOrderByNickname(String word, Pageable pageable);

    List<ClassroomNickname> findAllByClassroom(Classroom classroom);

    List<ClassroomNickname> findByNicknameContainingAndClassroomInOrderByNickname(String word, List<Classroom> list,  Pageable pageable);
}
