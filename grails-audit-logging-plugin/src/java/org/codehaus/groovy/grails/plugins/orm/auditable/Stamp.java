package org.codehaus.groovy.grails.plugins.orm.auditable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass({"org.codehaus.groovy.grails.plugins.orm.auditable.StampASTTransformation"})
public @interface Stamp {
	
	StampInfo dateCreated() default @StampInfo(type=Date.class);
	StampInfo lastUpdated() default @StampInfo(type=Date.class);
	
	StampInfo createdBy() default @StampInfo(type=String.class, nullable=true);
	StampInfo lastUpdatedBy() default @StampInfo(type=String.class,nullable=true);
	
}
