package com.stockpro.alertservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.stockpro.alertservice.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.info("Entering {}.{} with arguments: {}", 
            className, methodName, Arrays.toString(joinPoint.getArgs()));
        
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        
        log.info("Exiting {}.{} - Duration: {}ms", className, methodName, duration);
        
        return result;
    }

    @AfterThrowing(
        pointcut = "execution(* com.stockpro.alertservice..*(..))",
        throwing = "exception"
    )
    public void logException(JoinPoint joinPoint, Throwable exception) {
        log.error("Exception in {}.{}: {}", 
            joinPoint.getTarget().getClass().getSimpleName(),
            joinPoint.getSignature().getName(),
            exception.getMessage());
    }
}