package org.jboss.forge.arquillian;

import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.plugins.RequiresFacet;

/**
 * A base facet implementation for Facets which require Arquillian configuration to be installed.
 * 
 * @author Jérémie Lagarde
 * 
 */
@RequiresFacet({ DependencyFacet.class, ResourceFacet.class })
public abstract class ArquillianExtensionFacet extends BaseFacet
{
   public static final Dependency ARQUILLIAN_BOM =
            DependencyBuilder.create().setGroupId("org.jboss.arquillian")
                     .setArtifactId("arquillian-bom").setPackagingType(PackagingType.BASIC)
                     .setScopeType(ScopeType.IMPORT);

   private final DependencyInstaller installer;

   private final Shell shell;

   @Inject
   public ArquillianExtensionFacet(final DependencyInstaller installer, final Shell shell)
   {
      this.installer = installer;
      this.shell = shell;
   }

   abstract protected List<Dependency> getRequiredManagedDependency();

   abstract protected List<Dependency> getRequiredDependencies();

   @Override
   public boolean install()
   {
      DependencyFacet deps = project.getFacet(DependencyFacet.class);
      if (!deps.hasEffectiveManagedDependency(ARQUILLIAN_BOM) && !deps.hasDirectManagedDependency(ARQUILLIAN_BOM))
      {
         shell.println("Please use the arquillian plugin to add " + ARQUILLIAN_BOM.toString());
         return false;
      }

      ResourceFacet resources = project.getFacet(ResourceFacet.class);
      FileResource<?> resource = (FileResource<?>) resources.getTestResourceFolder().getChild("arquillian.xml");
      if (!resource.exists())
      {
         shell.println("Please use the arquillian plugin to create " + resource.getFullyQualifiedName());
         return false;
      }

      DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);

      for (Dependency bom : getRequiredManagedDependency())
      {
         if (!deps.hasEffectiveManagedDependency(bom))
         {

            String extensionVersionProperty = "version." + bom.getArtifactId().replaceAll("-", ".");
            String extensionVersion = dependencyFacet.getProperty(extensionVersionProperty);
            if (extensionVersion == null)
            {
               List<Dependency> versions = dependencyFacet.resolveAvailableVersions(bom);

               Dependency dependency = shell.promptChoiceTyped("What version of " + bom.getArtifactId()
                        + " do you want to use?", versions, getLatestNonSnapshotVersion(versions));
               extensionVersion = dependency.getVersion();
               dependencyFacet.setProperty(extensionVersionProperty, extensionVersion);
            }

            // need to set version after resolve is done, else nothing will resolve.
            if (!dependencyFacet.hasDirectManagedDependency(bom))
            {
               bom = DependencyBuilder.create(bom).setVersion("${" + extensionVersionProperty + "}");
               dependencyFacet.addDirectManagedDependency(bom);
            }
         }
      }

      for (Dependency dependency : getRequiredDependencies())
      {
         if (!dependencyFacet.hasEffectiveDependency(dependency))
         {
            if (!dependencyFacet.hasEffectiveManagedDependency(dependency))
            {
               String versionProperty = "version." + dependency.getArtifactId().replaceAll("-", ".");
               List<Dependency> versions = dependencyFacet.resolveAvailableVersions(dependency);
               Dependency selectedDependency = shell.promptChoiceTyped("What version of " + dependency.getArtifactId()
                        + " do you want to use?", versions, getLatestNonSnapshotVersion(versions));

               dependencyFacet.setProperty(versionProperty, selectedDependency.getVersion());
               dependencyFacet.addDirectDependency(DependencyBuilder.create(dependency).setVersion(
                        "${" + versionProperty + "}"));
            }
            else
            {
               dependencyFacet.addDirectDependency(DependencyBuilder.create(dependency).setVersion(null));
            }
         }

         if (!dependencyFacet.hasEffectiveDependency(dependency))
         {
            dependencyFacet.addDirectDependency(DependencyBuilder.create(dependency).setScopeType(ScopeType.TEST));
         }
      }
      return true;
   }

   @Override
   public boolean isInstalled()
   {
      DependencyFacet deps = project.getFacet(DependencyFacet.class);
      for (Dependency requirement : getRequiredDependencies())
      {
         if (!deps.hasEffectiveDependency(requirement))
         {
            return false;
         }
      }
      return true;
   }

   public DependencyInstaller getInstaller()
   {
      return installer;
   }

   public static Dependency getLatestNonSnapshotVersion(List<Dependency> dependencies)
   {
      if (dependencies == null)
      {
         return null;
      }
      for (int i = dependencies.size() - 1; i >= 0; i--)
      {
         Dependency dep = dependencies.get(i);
         if (!dep.getVersion().endsWith("SNAPSHOT"))
         {
            return dep;
         }
      }
      // FIXME this causes ArrayIndexOutOfBoundsException if the list is empty
      return dependencies.get(dependencies.size() - 1);
   }

}
