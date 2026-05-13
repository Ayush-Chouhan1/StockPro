package com.stockpro.reportservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.stockpro.reportservice.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        log.debug("Entering: {}", pjp.getSignature().toShortString());
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.debug("Exiting: {} ({}ms)",
                    pjp.getSignature().toShortString(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("Exception in {}: {}",
                    pjp.getSignature().toShortString(), ex.getMessage());
            throw ex;
        }
    }
}