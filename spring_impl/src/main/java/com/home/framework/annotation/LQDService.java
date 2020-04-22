package com.home.framework.annotation;

import java.lang.annotation.*;

/**
 * @author liqingdong
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LQDService {

	String value() default "";

}