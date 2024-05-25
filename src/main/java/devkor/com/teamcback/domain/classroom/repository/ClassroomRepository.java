package devkor.com.teamcback.domain.classroom.repository;

import devkor.com.teamcback.domain.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Classroom findClassroomById(Long id);

    Classroom findByNameAndBuildingId(String name, Long buildingId);
}
