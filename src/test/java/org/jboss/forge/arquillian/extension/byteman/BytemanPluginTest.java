package org.jboss.forge.arquillian.extension.byteman;

import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.project.Project;
import org.jboss.forge.test.AbstractShellTest;
import org.junit.Test;

public class BytemanPluginTest extends AbstractShellTest {

	@Test
	public void setup() throws Exception {
		Project project = initializeJavaProject();

		MavenCoreFacet coreFacet = project.getFacet(MavenCoreFacet.class);

		getShell().execute("forge install-plugin arquillian");
		getShell().execute("arquillian setup");

	}

}
