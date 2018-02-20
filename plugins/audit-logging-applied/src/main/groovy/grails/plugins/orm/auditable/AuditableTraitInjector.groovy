package grails.plugins.orm.auditable

import grails.compiler.ast.SupportsClassNode
import grails.compiler.traits.TraitInjector
import org.codehaus.groovy.ast.ClassNode
import org.grails.compiler.injection.GrailsASTUtils
import org.grails.core.artefact.DomainClassArtefactHandler

class AuditableTraitInjector implements TraitInjector, SupportsClassNode{
    @Override
    boolean supports(ClassNode classNode) {
        return !GrailsASTUtils.hasAnnotation(classNode, ExcludeAuditLogging)
    }

    @Override
    Class getTrait() {
        Auditable
    }

    @Override
    String[] getArtefactTypes() {
        [DomainClassArtefactHandler.TYPE] as String[]
    }
}
