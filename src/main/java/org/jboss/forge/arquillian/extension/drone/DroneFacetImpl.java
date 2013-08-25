package org.jboss.forge.arquillian.extension.drone;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.arquillian.extension.ArquillianExtensionFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.plugins.Alias;

/**
 * @author Jérémie Lagarde
 * 
 */
@Alias("forge.arquillian.extension.drone")
public class DroneFacetImpl extends ArquillianExtensionFacet implements DroneFacet
{

   @Inject
   public DroneFacetImpl(DependencyInstaller installer, Shell shell)
   {
      super(installer, shell);
   }

   @Override
   protected List<Dependency> getRequiredManagedDependency()
   {

      return Arrays.asList((Dependency) DependencyBuilder
               .create("org.jboss.arquillian.extension:arquillian-drone-bom::import:pom"));
   }

   @Override
   protected List<Dependency> getRequiredDependencies()
   {
      // may want to add an exclusion for net.sourceforge.htmlunit:htmlunit by default since it causes conflicts in embedded containers
      return Arrays.asList((Dependency) DependencyBuilder.create("org.jboss.arquillian.extension:arquillian-drone-webdriver-depchain::test:pom"));
      /*
      DependencyBuilder seleniumServer = DependencyBuilder.create("org.seleniumhq.selenium:selenium-server::test");
      seleniumServer.getExcludedDependencies().add(DependencyBuilder.create("org.mortbay.jetty:servlet-api-2.5"));
      return Arrays.asList((Dependency) DependencyBuilder.create("org.jboss.arquillian.extension:arquillian-drone-impl::test"),
               (Dependency) DependencyBuilder.create("org.jboss.arquillian.extension:arquillian-drone-selenium::test"),
               (Dependency) DependencyBuilder.create("org.jboss.arquillian.extension:arquillian-drone-selenium-server::test"),
               (Dependency) DependencyBuilder.create("org.jboss.arquillian.extension:arquillian-drone-webdriver-depchain:1.2.0.Alpha2::pom"),
               (Dependency) DependencyBuilder.create("org.seleniumhq.selenium:selenium-java::test"),
               (Dependency) seleniumServer,
               (Dependency) DependencyBuilder.create("org.slf4j:slf4j-simple:1.6.4:test"));
      */
   }

}
