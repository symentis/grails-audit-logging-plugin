package org.codehaus.groovy.grails.plugins.orm.auditable;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class StampASTTransformation extends AbstractASTTransformation{
	@Override
	public void transformGeneral(AnnotationNode annotationNode, ClassNode node) {
		addStaticField(node, "stampable", Boolean.class, Boolean.TRUE);
		for(MethodNode value:annotationNode.getClassNode().getMethods()){
			String fieldName = value.getName();
			
			Object stampInfo = getAnnotationValue(annotationNode, fieldName,true);
			
			boolean shouldExclude = getStampInfoValue(stampInfo,"exclude",false);
			if(!shouldExclude){
				Class<?> fieldType = getStampInfoValue(stampInfo, "type",String.class);
				
				FieldNode fieldNode = addFieldNode(node, fieldName, fieldType);
				
				boolean nullable = getStampInfoValue(stampInfo, "nullable",true);
				if(nullable){
					addNullableConstraint(node, fieldNode);
				}
			}
		}
	}
	
	private <T> T getStampInfoValue(Object stampInfo,String field,T defaultValue){
		T result = (T) InvokerHelper.invokeMethod(stampInfo, field, new Object[]{});
		return result!=null ? result:defaultValue;
	}
}
