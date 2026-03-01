package devkor.com.teamcback.global.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UpdateScore {
    int addScore() default 0;

    /**
     * 동적 점수 계산 여부
     * true인 경우 addScore 무시하고 Request 내용 기반으로 점수 계산
     * (리뷰 생성/수정/삭제 등에서 사용)
     */
    boolean dynamic() default false;
}
