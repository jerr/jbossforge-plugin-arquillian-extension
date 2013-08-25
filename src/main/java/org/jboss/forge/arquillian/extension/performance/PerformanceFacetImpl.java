package org.jboss.forge.arquillian.extension.performance;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.arquillian.extension.ArquillianExtensionFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.shell.Shell;

/**
 * @author Jérémie Lagarde
 * 
 */
public class PerformanceFacetImpl extends ArquillianExtensionFacet implements PerformanceFacet
{

   @Inject
   public PerformanceFacetImpl(DependencyInstaller installer, Shell shell)
   {
      super(installer, shell);
   }

   @Override
   protected List<Dependency> getRequiredManagedDependency()
   {
      return Arrays.asList();
   }

   @Override
   protected List<Dependency> getRequiredDependencies()
   {
      return Arrays.asList((Dependency) DependencyBuilder
               .create("org.jboss.arquillian.extension:arquillian-performance-impl::test"));
   }
}
