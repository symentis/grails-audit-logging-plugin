package org.codehaus.groovy.grails.plugins.orm.auditable;

import static org.springframework.asm.Opcodes.ACC_PUBLIC;
import groovy.lang.Closure;

import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;

public abstract class AbstractASTTransformation implements ASTTransformation {
	
	
	@Override
	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
		AnnotationNode annotation = (AnnotationNode) nodes[0];
		for (ASTNode aSTNode : nodes) {
			if (aSTNode instanceof ClassNode) {
				try {
					transformGeneral(annotation,(ClassNode) aSTNode);
				}
				catch(Exception exception){
					exception.printStackTrace();
				}
				
				break;
			}
		}
	}
	
	
	protected boolean hasField(ClassNode cNode,String name){
		return getField(cNode,name) != null;
	}
	protected FieldNode getField(ClassNode cNode,String name){
		return cNode.getDeclaredField(name);
	}
	
	public Object getMemberValue(AnnotationNode node, String name,Object defaultValue) {
        final Expression member = node.getMember(name);
        Object result = null;
        if (member != null && member instanceof ConstantExpression) result= ((ConstantExpression) member).getValue();
        
        if(result==null) result = defaultValue;
        return result;
    }
	
	
	public FieldNode addBooleanFieldNode(ClassNode node,String fieldName,boolean defaultValue){
		FieldNode fieldNode = createFieldNode(node,fieldName,Modifier.PRIVATE,ClassHelper.boolean_TYPE,new ConstantExpression(defaultValue,true));
		addIsser(fieldNode,node);
		addSetter(fieldNode,node);
		return fieldNode;
	}

		
	public FieldNode addFieldNode(ClassNode node,String fieldName,Class<?> fieldType){
		return addFieldNode(node,fieldName,fieldType,null);
	}
	public FieldNode addFieldNode(ClassNode node,String fieldName,Class<?> fieldType,Expression defaultValue){
		FieldNode field = createFieldNode(node,fieldName,Modifier.PRIVATE,new ClassNode(fieldType),defaultValue);
		addGetter(field,node);
		addSetter(field,node);
		return field;
	}
	
	public FieldNode createFieldNode(ClassNode owner,String fieldName,int modifier,ClassNode fieldTypeNode,Expression defaultValue){
		FieldNode field = new FieldNode(fieldName,modifier,fieldTypeNode,owner,defaultValue);
		owner.addField(field);
		return field;
	}
	
	public abstract void transformGeneral(AnnotationNode annotationNode, ClassNode node);

	
	protected void addGetter(FieldNode fieldNode, ClassNode owner) {
		addGetter(fieldNode.getName(), fieldNode, owner, ACC_PUBLIC);
	}

	protected void addGetter(String name, FieldNode fieldNode, ClassNode owner) {
		addGetter(name, fieldNode, owner, ACC_PUBLIC);
	}

	protected void addGetter(FieldNode fieldNode, ClassNode owner, int modifier) {
		addGetter(fieldNode.getName(), fieldNode, owner, modifier);
	}

	protected void addGetter(String name, FieldNode fieldNode, ClassNode owner, int modifier) {
		addGetterOrIsser("get",name,fieldNode,owner,modifier);
	}
	protected void addIsser(FieldNode fieldNode, ClassNode owner) {
		addIsser(fieldNode.getName(), fieldNode, owner, ACC_PUBLIC);
	}

	protected void addIsser(String name, FieldNode fieldNode, ClassNode owner) {
		addIsser(name, fieldNode, owner, ACC_PUBLIC);
	}

	protected void addIsser(FieldNode fieldNode, ClassNode owner, int modifier) {
		addIsser(fieldNode.getName(), fieldNode, owner, modifier);
	}

	protected void addIsser(String name, FieldNode fieldNode, ClassNode owner, int modifier) {
		addGetterOrIsser("is",name,fieldNode,owner,modifier);
	}

	protected void addGetterOrIsser(String getterType,String name, FieldNode fieldNode, ClassNode owner, int modifier) {
		ClassNode type = fieldNode.getType();
		String getterName = getterType + StringUtils.capitalize(name);
		owner.addMethod(getterName,
				modifier,
				nonGeneric(type),
				Parameter.EMPTY_ARRAY,
				null,
				new ReturnStatement(new FieldExpression(fieldNode)));
	}
    protected FieldNode addTransientMapping(ClassNode classNode, String fieldName) {
        FieldNode transients = classNode.getDeclaredField("transients");
    	if(transients==null){
    		
        	transients = new FieldNode("transients",Modifier.STATIC|Modifier.PUBLIC,new ClassNode(List.class),classNode,new ListExpression());
        	classNode.addField(transients); 
        } 
        ListExpression constraintsExpression = (ListExpression) transients.getInitialExpression();
        if(!hasFieldInList(constraintsExpression,fieldName)){
        	constraintsExpression.addExpression(new ConstantExpression(fieldName));	
        } 
		return transients;
    }
    private boolean hasFieldInList(ListExpression constraintsExpression,String fieldName) {
    	if(constraintsExpression!=null){
    		for(Expression expression:constraintsExpression.getExpressions()){
    			if(expression instanceof ConstantExpression){
    				if(((ConstantExpression)expression).getValue().equals(fieldName)){
    					return true;
    				}
    			}
    		}
    	}
    	return false;
	}

	protected FieldNode addSqlTypeMapping(ClassNode classNode, FieldNode fieldNode,String sqlType) {
        FieldNode closure = classNode.getDeclaredField("mapping");
        if(closure==null){
        	ClosureExpression constraintsExpression = new ClosureExpression(new Parameter[]{}, new BlockStatement());
        	constraintsExpression.setVariableScope(new VariableScope());
        	
        	closure = new FieldNode("mapping",Modifier.STATIC,new ClassNode(Closure.class),classNode,constraintsExpression);
        	classNode.addField(closure);
        }
        ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
        BlockStatement block = (BlockStatement) exp.getCode();

        if (!hasFieldInClosure(closure, fieldNode.getName())) {
            NamedArgumentListExpression namedarg = new NamedArgumentListExpression();
            namedarg.addMapEntryExpression(new ConstantExpression("sqlType"), new ConstantExpression(sqlType));
            MethodCallExpression constExpr = new MethodCallExpression(
                    VariableExpression.THIS_EXPRESSION,
                    new ConstantExpression(fieldNode.getName()),
                    namedarg);
            block.addStatement(new ExpressionStatement(constExpr));
        }
		return fieldNode;
    }
	
	protected <T> FieldNode addStaticField(ClassNode classNode, String fieldname,Class<T> type,T initialValue) {
		FieldNode fieldNode = classNode.getDeclaredField(fieldname);
		
		if(fieldNode==null){
			fieldNode = new FieldNode(fieldname,Modifier.STATIC+Modifier.PUBLIC,new ClassNode(type),classNode,new ConstantExpression(initialValue));
			classNode.addField(fieldNode);
		}
		return fieldNode;
	}
	
    protected FieldNode addNullableConstraint(ClassNode classNode, FieldNode fieldNode) {
        FieldNode closure = classNode.getDeclaredField("constraints");
        if(closure==null){
        	ClosureExpression constraintsExpression = new ClosureExpression(new Parameter[]{}, new BlockStatement());
        	constraintsExpression.setVariableScope(new VariableScope());
        	
        	closure = new FieldNode("constraints",Modifier.STATIC,new ClassNode(Closure.class),classNode,constraintsExpression);
        	classNode.addField(closure);
        }
        ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
        BlockStatement block = (BlockStatement) exp.getCode();

        if (!hasFieldInClosure(closure, fieldNode.getName())) {
            NamedArgumentListExpression namedarg = new NamedArgumentListExpression();
            namedarg.addMapEntryExpression(new ConstantExpression("nullable"), new ConstantExpression(true));
            namedarg.addMapEntryExpression(new ConstantExpression("blank"), new ConstantExpression(true));
            MethodCallExpression constExpr = new MethodCallExpression(
                    VariableExpression.THIS_EXPRESSION,
                    new ConstantExpression(fieldNode.getName()),
                    namedarg);
            block.addStatement(new ExpressionStatement(constExpr));
        }
		return fieldNode;
    }
    
    protected MethodNode getBeforeInsertMethod(ClassNode node){
        final String methodName = "beforeInsert";
        MethodNode beforeInsertMethod = node.getDeclaredMethod(methodName, new Parameter[]{});

        if (beforeInsertMethod == null){
            beforeInsertMethod = new MethodNode(methodName, Modifier.PUBLIC, new ClassNode(Object.class), new Parameter[]{}, new ClassNode[]{}, new BlockStatement());
            node.addMethod(beforeInsertMethod);
        }

        return beforeInsertMethod;
    }

    protected MethodNode getBeforeUpdateMethod(ClassNode node){
        final String methodName = "beforeUpdate";
        MethodNode beforeUpdateMethod = node.getDeclaredMethod(methodName, new Parameter[]{});

        if (beforeUpdateMethod == null){
            beforeUpdateMethod = new MethodNode(methodName, Modifier.PUBLIC, new ClassNode(Object.class), new Parameter[]{}, new ClassNode[]{}, new BlockStatement());
            node.addMethod(beforeUpdateMethod);
        }

        return beforeUpdateMethod;
    }
    
    
    protected boolean hasFieldInClosure(FieldNode closure, String fieldName) {
        if (closure != null) {
            ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
            BlockStatement block = (BlockStatement) exp.getCode();
            List<Statement> ments = block.getStatements();
            for (Statement expstat : ments) {
                if (expstat instanceof ExpressionStatement && ((ExpressionStatement) expstat).getExpression() instanceof MethodCallExpression) {
                    MethodCallExpression methexp = (MethodCallExpression) ((ExpressionStatement) expstat).getExpression();
                    ConstantExpression conexp = (ConstantExpression) methexp.getMethod();
                    if (conexp.getValue().equals(fieldName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


	
	
	protected ClassNode nonGeneric(ClassNode type) {
		if (type.isUsingGenerics()) {
			final ClassNode nonGen = ClassHelper.makeWithoutCaching(type.getName());
			nonGen.setRedirect(type);
			nonGen.setGenericsTypes(null);
			nonGen.setUsingGenerics(false);
			return nonGen;
		} else {
			return type;
		}
	}
	
	protected void addSetter(FieldNode fieldNode, ClassNode owner) {
		addSetter(fieldNode, owner, ACC_PUBLIC);
	}

	protected void addSetter(FieldNode fieldNode, ClassNode owner, int modifier) {
		ClassNode type = fieldNode.getType();
		String name = fieldNode.getName();
		String setterName = "set" + StringUtils.capitalize(name);
		
		owner.addMethod(setterName,
			modifier,
			ClassHelper.VOID_TYPE,
			new Parameter[]{new Parameter(nonGeneric(type), "value")},
			null,
			new ExpressionStatement(
				new BinaryExpression(new FieldExpression(fieldNode),
					Token.newSymbol(Types.EQUAL, -1, -1),
					new VariableExpression("value"))));
	}
	public <T> T getDefaultAnnotationValue(AnnotationNode annotationNode,String memberName,T defaultValue){
		T result = null;
		MethodNode methodNode = annotationNode.getClassNode().getMethod(memberName, new Parameter[]{});
		if(methodNode!=null && methodNode.getCode() instanceof ReturnStatement){
			ReturnStatement returnStatement = (ReturnStatement) methodNode.getCode();
			if(returnStatement!=null && returnStatement.getExpression() instanceof ConstantExpression){
				result = (T) ((ConstantExpression)returnStatement.getExpression()).getValue();
			}
		}
		return result == null? defaultValue:result;
	}
	public <T> T getAnnotationValue(AnnotationNode annotationNode,String memberName,T defaultValue){
		T result = null;
		
		Expression expression = annotationNode.getMember(memberName);
		if(expression !=null && expression instanceof ConstantExpression){
			result = (T) ((ConstantExpression)expression).getValue();
		}
		else{
			result = getDefaultAnnotationValue(annotationNode, memberName, defaultValue);
		}
		return result;
	}
	
	
}
