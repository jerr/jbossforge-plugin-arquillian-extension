package org.jboss.forge.arquillian.extension.byteman;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.arquillian.ArquillianExtensionFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.shell.Shell;

/**
 * @author Jérémie Lagarde
 * 
 */
public class BytemanFacetImpl extends ArquillianExtensionFacet implements BytemanFacet
{

   @Inject
   public BytemanFacetImpl(DependencyInstaller installer, Shell shell)
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
               .create("org.jboss.arquillian.extension:arquillian-extension-byteman::test"),
               (Dependency) DependencyBuilder.create("org.jboss.byteman:byteman::test"),
               // TODO : reuse ${version.byteman} property for byteman-submit dependency.
               (Dependency) DependencyBuilder.create("org.jboss.byteman:byteman-submit::test"));
   }
}
