package orchastack.core.transaction.binding;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names.computeEffectiveMethodName;

import java.lang.annotation.ElementType;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.transaction.handler.TransactionHandler;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class TransactionalVisitor extends AnnotationVisitor {

	final private static Logger log = Logger
			.getLogger(TransactionalVisitor.class.getName());

	private ComponentWorkbench workbench;

	private String m_name;
	private ElementType m_type;

	private String m_timeout;
	private String m_propagation = "requires";
	private String m_norollbackfor;
	private String m_exceptiononrollback;

	public TransactionalVisitor(ComponentWorkbench workbench, ElementType type,
			String name) {
		super(Opcodes.ASM5);
		this.workbench = workbench;
		this.m_name = name;
		this.m_type = type;
	}

	public void visit(String name, Object value) {
		log.log(Level.INFO, "Visiting property ..." + name);

		if (name.equals("timeout")) {
			this.m_timeout = value.toString();
			return;
		}
		if (name.equals("propagation")) {
			this.m_propagation = value.toString();
			return;
		}
		if (name.equals("norollbackfor")) {
			this.m_norollbackfor = value.toString();
			return;
		}
		if (name.equals("exceptiononrollback")) {
			this.m_exceptiononrollback = value.toString();
			return;
		}
	}

	private void visitMethod(String method_name) {

		method_name = computeEffectiveMethodName(method_name);

		log.log(Level.INFO, "Visiting method ..." + method_name);

		Element transact = workbench.getIds().get(method_name);
		if (transact == null)
			transact = new Element(TransactionHandler.NAME,
					TransactionHandler.NAMESPACE);

		transact.addAttribute(new Attribute("method", method_name));
		if (m_timeout != null)
			transact.addAttribute(new Attribute("timeout", m_timeout));
		if (m_propagation != null)
			transact.addAttribute(new Attribute("propagation", m_propagation));
		if (m_norollbackfor != null)
			transact.addAttribute(new Attribute("norollbackfor",
					m_norollbackfor));
		if (m_exceptiononrollback != null)
			transact.addAttribute(new Attribute("exceptiononrollback",
					m_exceptiononrollback));

		workbench.getElements().put(transact, null);
	}

	public void visitEnd() {

		if (m_type == ElementType.METHOD) {
			visitMethod(m_name);
		} else if (m_type == ElementType.TYPE) {

			Map<Element, String> elts = workbench.getElements();

			// get all public methods
			ClassNode classNode = workbench.getClassNode();

			String cname = classNode.name;

			log.log(Level.INFO, "Manipulating methods ..." + classNode.methods);

			// for (String mthd : pmthods) {
			//
			// log.log(Level.INFO, "Manipulating method ..." + mthd);
			// visitMethod(mthd);
			//
			// }

		}
	}
}
