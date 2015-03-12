package org.codehaus.groovy.grails.plugins.orm.auditable;

public @interface StampInfo{
	boolean nullable() default true;
	boolean exclude() default false;
	Class<?> type() default String.class;
}
