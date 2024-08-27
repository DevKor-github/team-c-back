package devkor.com.teamcback.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* devkor.com.teamcback.domain.routes.controller..*(..))")
    private void navigation() {}

    @Pointcut("execution(* devkor.com.teamcback.domain.search.contoller.*.globalSearch(..))")
    private void search() {}

//    @Around("navigation() || search()") // 길찾기와 검색 api 소요시간 측정
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object proceed = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - startTime;

        System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");

        return proceed;
    }
}
