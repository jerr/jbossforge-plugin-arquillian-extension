package org.jboss.forge.arquillian.extension;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.util.ResourceUtil;

/**
 * @author Jérémie Lagarde
 * 
 */
public abstract class AbstractExtensionPlugin implements Plugin
{
   @Inject
   protected Project project;

   @Inject
   protected Shell shell;

   @Inject
   @Current
   protected Resource<?> currentResource;

   @Inject
   protected Event<InstallFacets> request;

   @Inject
   protected Event<PickupResource> pickup;

   protected void addPropertyToArquillianConfig(Node xml, String qualifier, String key, String value)
   {
      if (value != null)
      {
         xml.getOrCreate("extension@qualifier=" + qualifier)
                  .getOrCreate("property@name=" + key)
                  .text(value);
      }
      else
      {
         if (xml.getOrCreate("extension@qualifier=" + qualifier).getSingle("property@name=" + key) != null)
         {
            xml.getOrCreate("extension@qualifier=" + qualifier).removeChild("property@name=" + key);
         }
      }
   }

   /**
    * Retrieves the package portion of the current directory if it is a package, null otherwise.
    * 
    * @return String representation of the current package, or null
    */
   protected String getPackagePortionOfCurrentDirectory()
   {
      for (DirectoryResource r : project.getFacet(JavaSourceFacet.class).getSourceFolders())
      {
         final DirectoryResource currentDirectory = shell.getCurrentDirectory();
         if (ResourceUtil.isChildOf(r, currentDirectory))
         {
            return currentDirectory.getFullyQualifiedName().replace(r.getFullyQualifiedName() + "/", "")
                     .replaceAll("/", ".");
         }
      }
      return null;
   }

}