package orchastack.core.security.binding;

import java.lang.annotation.ElementType;
import java.util.logging.Logger;

import orchastack.core.security.handler.AuthzAnnotationType;

import org.apache.felix.ipojo.manipulator.spi.AbsBindingModule;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;

import orchastack.core.security.annotation.RequiresAuthentication;
import orchastack.core.security.annotation.RequiresGuest;
import orchastack.core.security.annotation.RequiresPermissions;
import orchastack.core.security.annotation.RequiresRoles;
import orchastack.core.security.annotation.RequiresUser;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.ClassNode;

public class SecuredMethodBindingModule extends AbsBindingModule {

	final private static Logger log = Logger
			.getLogger(SecuredMethodBindingModule.class.getName());

	@Override
	protected void configure() {
		bind(RequiresRoles.class).to(new AnnotationVisitorFactory() {
			public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

				ElementType type = context.getElementType();
				String name = null;
				
				if (type == ElementType.TYPE) {
					name = context.getClassNode().name;
					log.info("Annotation is on class..." + name);
				} else {
					name = context.getMethodNode().name;
					log.info("Annotation is on method..." + name);
				}

				
				return new SecuredMethodVisitor(context.getWorkbench(), type,
						name, AuthzAnnotationType.RequiresRoles);
			}
		});

		bind(RequiresPermissions.class).to(new AnnotationVisitorFactory() {
			public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

				ElementType type = context.getElementType();
				String name = null;
				
				if (type == ElementType.TYPE) {
					name = context.getClassNode().name;
					log.info("Annotation is on class..." + name);
				} else {
					name = context.getMethodNode().name;
					log.info("Annotation is on method..." + name);
				}

				return new SecuredMethodVisitor(context.getWorkbench(), type,
						name, AuthzAnnotationType.RequiresPermissions);
			}
		});
		
		bind(RequiresUser.class).to(new AnnotationVisitorFactory() {
			public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

				ElementType type = context.getElementType();
				String name = null;
				if (type == ElementType.TYPE) {
					name = context.getClassNode().name;
					log.info("Annotation is on class..." + name);
				} else {
					name = context.getMethodNode().name;
					log.info("Annotation is on method..." + name);
				}

				return new SecuredMethodVisitor(context.getWorkbench(), type,
						name, AuthzAnnotationType.RequiresUser);
			}
		});
		
		bind(RequiresGuest.class).to(new AnnotationVisitorFactory() {
			public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

				ElementType type = context.getElementType();
				String name = null;

				if (type == ElementType.TYPE) {
					name = context.getClassNode().name;
					log.info("Annotation is on class..." + name);
				} else {
					name = context.getMethodNode().name;
					log.info("Annotation is on method..." + name);
				}

				return new SecuredMethodVisitor(context.getWorkbench(), type,
						name, AuthzAnnotationType.RequiresGuest);
			}
		});
		
		bind(RequiresAuthentication.class).to(new AnnotationVisitorFactory() {
			public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

				ElementType type = context.getElementType();
				String name = null;
				if (type == ElementType.TYPE) {
					name = context.getClassNode().name;
					log.info("Annotation is on class..." + name);
				} else {
					name = context.getMethodNode().name;
					log.info("Annotation is on method..." + name);
				}

				return new SecuredMethodVisitor(context.getWorkbench(), type,
						name, AuthzAnnotationType.RequiresAuthentication);
			}
		});
		
		
		bind(RequiresPermissions.class).to(new AnnotationVisitorFactory() {
			public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

				ElementType type = context.getElementType();
				String name = null;
				if (type == ElementType.TYPE) {
					name = context.getClassNode().name;
					log.info("Annotation is on class..." + name);
				} else {
					name = context.getMethodNode().name;
					log.info("Annotation is on method..." + name);
				}

				return new SecuredMethodVisitor(context.getWorkbench(), type,
						name, AuthzAnnotationType.RequiresPermissions);
			}
		});
	}
}
