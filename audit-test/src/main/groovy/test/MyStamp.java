package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

import grails.plugins.orm.auditable.StampInfo;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass({"grails.plugins.orm.auditable.StampASTTransformation"})
public @interface MyStamp {
	
	StampInfo removed() default @StampInfo(fieldname="removed",type=Boolean.class);
	
	StampInfo dateCreated() default @StampInfo(fieldname="dateCreated",type=Date.class);
	StampInfo lastUpdated() default @StampInfo(fieldname="lastUpdated",type=Date.class);
	
	StampInfo createdBy() default @StampInfo(fieldname="createdBy",type=String.class, nullable=true);
	StampInfo lastUpdatedBy() default @StampInfo(fieldname="lastUpdatedBy",type=String.class,nullable=true);
	
}
