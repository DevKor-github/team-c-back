package devkor.com.teamcback.domain.operatingtime.entity;

import devkor.com.teamcback.domain.operatingtime.dto.HolidayResDto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_holiday")
@NoArgsConstructor
public class Holiday {
    @Id
    @GeneratedValue
    private Long id;

    private boolean isHoliday;

    private LocalDate date;

    private String name;

    public Holiday(HolidayResDto resDto) {
        this.isHoliday = resDto.isHoliday();
        this.date = resDto.getDate();
        this.name = resDto.getName();
    }
}
