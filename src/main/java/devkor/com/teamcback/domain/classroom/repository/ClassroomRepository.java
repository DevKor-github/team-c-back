package devkor.com.teamcback.domain.classroom.repository;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.classroom.entity.Classroom;
import devkor.com.teamcback.domain.navigate.entity.Node;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Classroom findClassroomById(Long id);

    List<Classroom> findAllByBuildingAndFloor(Building building, int floor);

    Classroom findByNode(Node node);
}
