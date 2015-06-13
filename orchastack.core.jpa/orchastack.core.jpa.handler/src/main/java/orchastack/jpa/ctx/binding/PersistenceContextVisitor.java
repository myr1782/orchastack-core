package orchastack.jpa.ctx.binding;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.metadata.Attribute;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class PersistenceContextVisitor extends AnnotationVisitor {

	private ComponentWorkbench workbench;

	private String m_field;

	private String m_unitname;

	public PersistenceContextVisitor(ComponentWorkbench workbench, String name) {
		super(Opcodes.ASM5);
		this.workbench = workbench;
		this.m_field = name;
	}

	public void visit(String name, Object value) {

		if (name.equals("unitName")) {
			this.m_unitname = value.toString();
			return;
		}
	}

	public void visitEnd() {
		Element persist = workbench.getIds().get(m_field);
		if (persist == null)
			persist = new Element("persistencecontext", "javax.persistence");
		persist.addAttribute(new Attribute("field", m_field));
		persist.addAttribute(new Attribute("unitName", m_unitname));

		workbench.getElements().put(persist, null);
	}
}
