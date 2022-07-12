package com.lw.nio.action;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

//标注方法
@Retention(RUNTIME)
@Target(METHOD)
public @interface Action {
	String action();
}
