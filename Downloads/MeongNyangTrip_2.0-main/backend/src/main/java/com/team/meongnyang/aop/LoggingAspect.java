package com.team.meongnyang.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 컨트롤러 및 서비스 계층의 실행 로그를 기록하는 AOP Aspect
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.team.meongnyang..controller..*(..)) || execution(* com.team.meongnyang..service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String layer = className.contains("controller") ? "Controller" : "Service";
        String methodName = joinPoint.getSignature().getName();

        long start = System.currentTimeMillis();
        log.info("[AOP START] => {} : {}", layer, methodName);

        try {
            Object result = joinPoint.proceed();
            long end = System.currentTimeMillis();
            log.info("[AOP END]   => {} : {} : {}ms", layer, methodName, (end - start));
            return result;
        } catch (Exception e) {
            log.error("[AOP ERROR] => {} : {} | {}", layer, methodName, e.getMessage());
            throw e;
        }
    }
}
