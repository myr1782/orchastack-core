package orchastack.core.transaction.binding;

import java.lang.annotation.ElementType;
import java.util.logging.Logger;

import orchastack.core.transaction.Transactional;

import org.apache.felix.ipojo.manipulator.spi.AbsBindingModule;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.objectweb.asm.AnnotationVisitor;

public class TransactionalBindingModule extends AbsBindingModule {

	final private static Logger log = Logger
			.getLogger(TransactionalBindingModule.class.getName());

	@Override
	protected void configure() {
		bind(Transactional.class).to(new AnnotationVisitorFactory() {
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

				return new TransactionalVisitor(context.getWorkbench(), type,
						name);
			}
		});

	}
}
