package devkor.com.teamcback.domain.koyeon.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "tb_tag_menu")
@NoArgsConstructor
public class TagMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "food_tag_id")
    private FoodTag foodTag;

    @ManyToOne
    @JoinColumn(name = "menu_id")
    private Menu menu;

    public void setTagMenu(FoodTag foodTag, Menu menu) {
        setFoodTag(foodTag);
        setMenu(menu);
    }

    private void setFoodTag(FoodTag foodTag) {
        this.foodTag = foodTag;
    }

    private void setMenu(Menu menu) {
        this.menu = menu;
    }
}
