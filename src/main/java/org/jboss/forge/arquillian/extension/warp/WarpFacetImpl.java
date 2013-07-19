package org.jboss.forge.arquillian.extension.warp;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.arquillian.ArquillianExtensionFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.plugins.Alias;

/**
 * @author Dan Allen, Jérémie Lagarde
 * 
 */
@Alias("forge.arquillian.extension.warp")
public class WarpFacetImpl extends ArquillianExtensionFacet implements WarpFacet
{

   @Inject
   public WarpFacetImpl(DependencyInstaller installer, Shell shell)
   {
      super(installer, shell);
   }

   @Override
   protected List<Dependency> getRequiredManagedDependency()
   {
      return Arrays.asList((Dependency) DependencyBuilder.create("org.jboss.arquillian.extension:arquillian-warp-bom::import:pom"));
   }

   @Override
   protected List<Dependency> getRequiredDependencies()
   {
      return Arrays.asList((Dependency) DependencyBuilder.create("org.jboss.arquillian.extension:arquillian-warp::test:pom") );
   }
}
