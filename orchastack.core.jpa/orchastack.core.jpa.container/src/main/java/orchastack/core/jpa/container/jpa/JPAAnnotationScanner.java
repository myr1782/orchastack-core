package orchastack.core.jpa.container.jpa;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import orchastack.core.jpa.container.util.TempBundleDelegatingClassLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

class JPAAnnotationScanner {

	final private static JPAAnnotationScanner scanner;
	static {
		scanner = new JPAAnnotationScanner();
	}

	private JPAAnnotationScanner() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static JPAAnnotationScanner getInstance() {
		return scanner;
	}

	public Collection<String> findJPAAnnotatedClasses(Bundle b) {
		BundleWiring bw = (BundleWiring)b.adapt(BundleWiring.class);
		Collection<String> resources = bw.listResources("/", "*.class",
				BundleWiring.LISTRESOURCES_LOCAL
						| BundleWiring.LISTRESOURCES_RECURSE);

		Collection<String> classes = new ArrayList<String>();
		ClassLoader cl = new TempBundleDelegatingClassLoader(b, this.getClass()
				.getClassLoader());
		for (String s : resources) {
			s = s.replace('/', '.').substring(0, s.length() - 6);
			try {
				Class<?> clazz = Class.forName(s, false, cl);

				if (clazz.isAnnotationPresent(Entity.class)
						|| clazz.isAnnotationPresent(MappedSuperclass.class)
						|| clazz.isAnnotationPresent(Embeddable.class)) {
					classes.add(s);
				}

			} catch (ClassNotFoundException cnfe) {

			} catch (NoClassDefFoundError ncdfe) {

			}
		}
		return classes;
	}

}
