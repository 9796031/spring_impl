package com.home.framework.annotation;

import java.lang.annotation.*;

/**
 * @author liqingdong
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LQDAutowired {

	String value() default "";

}