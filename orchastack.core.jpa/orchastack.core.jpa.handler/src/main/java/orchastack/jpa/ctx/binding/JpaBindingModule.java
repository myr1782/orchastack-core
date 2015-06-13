package orchastack.jpa.ctx.binding;

import javax.persistence.PersistenceContext;

import org.apache.felix.ipojo.manipulator.spi.AbsBindingModule;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.objectweb.asm.AnnotationVisitor;

public class JpaBindingModule extends AbsBindingModule {

	@Override
	protected void configure() {
		bind(PersistenceContext.class).to(new AnnotationVisitorFactory() {
			public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
				return new PersistenceContextVisitor(context.getWorkbench(),context.getFieldNode().name);
			}
		});

	}

}
