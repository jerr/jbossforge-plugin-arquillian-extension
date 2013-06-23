package org.jboss.forge.arquillian.extension.persistence;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.arquillian.ArquillianExtensionFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.facets.MetadataFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceFilter;
import org.jboss.forge.resources.UnknownFileResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.util.Streams;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.servlet.web.WebAppDescriptor;

/**
 * @author Jérémie Lagarde
 * 
 */
public class PersistenceFacetImpl extends ArquillianExtensionFacet implements PersistenceFacet
{

   @Inject
   public PersistenceFacetImpl(DependencyInstaller installer, Shell shell)
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
               .create("org.jboss.arquillian.extension:arquillian-persistence-impl::test"));
   }
   
   @Override
   public List<Resource<?>> getDataSetFiles()
   {
      DirectoryResource testResourceFolder = project.getFacet(ResourceFacet.class).getTestResourceFolder();

      ResourceFilter filter = new ResourceFilter()
      {
         @Override
         public boolean accept(Resource<?> resource)
         {
            String name = resource.getName().toLowerCase();
            return (name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".yaml"));
         }
      };
      return testResourceFolder.listResources(filter);
   }

   
}
