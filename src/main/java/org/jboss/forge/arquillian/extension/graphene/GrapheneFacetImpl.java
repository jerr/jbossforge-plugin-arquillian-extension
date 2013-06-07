package org.jboss.forge.arquillian.extension.graphene;

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
 * @author Jérémie Lagarde
 * 
 */
@Alias("forge.arquillian.extension.graphene")
public class GrapheneFacetImpl extends ArquillianExtensionFacet implements GrapheneFacet
{

   @Inject
   public GrapheneFacetImpl(DependencyInstaller installer, Shell shell)
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
      return Arrays.asList((Dependency) DependencyBuilder.create("org.jboss.arquillian.graphene:arquillian-graphene::test:pom") );
   }
}
