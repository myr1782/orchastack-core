package orchastack.core.security.binding;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names.computeEffectiveMethodName;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.security.handler.AuthzAnnotationType;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class SecuredMethodVisitor extends AnnotationVisitor {

	final private static Logger log = Logger
			.getLogger(SecuredMethodVisitor.class.getName());

	private ComponentWorkbench workbench;

	private String m_name;
	private ElementType m_type;

	private String m_property = "value";

	private String m_value;

	private String m_logical;

	private AuthzAnnotationType m_authzType;

	public SecuredMethodVisitor(ComponentWorkbench workbench, ElementType type,
			String name, AuthzAnnotationType authzType) {
		super(Opcodes.ASM5);
		this.workbench = workbench;
		this.m_name = name;
		this.m_type = type;
		this.m_authzType = authzType;
		// switch (m_authzType) {
		// case RequiresRoles:
		// m_property = "roles";
		// break;
		// case RequiresPermissions:
		// m_property = "permissions";
		// break;
		// }
	}

	public void visit(String name, Object value) {
		log.log(Level.INFO, "Visiting property ..." + name);

		if (name.equals(m_property)) {
			this.m_value = value.toString();
			return;
		}
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		
		log.log(Level.INFO, "Visiting enum property ..." + name);
		
		if (name.equals("logical")) {
			this.m_logical = value;
			return;
		}

	}

	private void visitMethod(String method_name) {

		String name = null;
		switch (m_authzType) {
		case RequiresRoles:
			name = "requiresroles";
			break;
		case RequiresPermissions:
			name = "requirespermissions";
			break;
		case RequiresAuthentication:
			name = "requiresauthentication";
			break;
		case RequiresGuest:
			name = "requiresguest";
			break;
		case RequiresUser:
			name = "requiresuser";
			break;
		}

		method_name = computeEffectiveMethodName(method_name);

		log.log(Level.INFO, "Visiting method ..." + method_name);

		Element methodName = workbench.getIds().get(method_name);
		if (methodName == null)
			methodName = new Element(name, AuthzAnnotationType.NAMESPACE);

		methodName.addAttribute(new Attribute("method", method_name));

		if (m_value != null)
			methodName.addAttribute(new Attribute(m_property, m_value));

		if (m_logical != null)
			methodName.addAttribute(new Attribute("logical", m_logical));

		workbench.getElements().put(methodName, null);
	}

	public void visitEnd() {

		if (this.m_type == ElementType.METHOD) {
			visitMethod(m_name);
		}

		// else {
		//
		// List<MethodNode> methods = this.m_classNode.methods;
		//
		// log.log(Level.INFO, "Manipulating methods ..." + methods
		// + " for class node" + this.m_classNode);
		//
		// for (MethodNode mthd : methods) {
		//
		// log.log(Level.INFO, "Manipulating method ..." + mthd);
		// visitMethod(mthd.name);
		//
		// }
		//
		// }
	}
}
