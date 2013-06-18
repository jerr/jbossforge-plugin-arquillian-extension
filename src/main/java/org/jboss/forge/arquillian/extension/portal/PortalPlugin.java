package org.jboss.forge.arquillian.extension.portal;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

/**
 * @author Jérémie Lagarde
 * 
 */
@Alias("arq-portal")
@RequiresFacet({ JavaSourceFacet.class, ResourceFacet.class, PortalFacet.class })
@RequiresProject
@Help("A plugin that helps setting up Arquillian Portal extension")
public class PortalPlugin implements Plugin
{

   @Inject
   private Project project;

   @Inject
   private Event<InstallFacets> request;

   @SetupCommand
   public void setup(final PipeOut out)
   {

      if (!project.hasFacet(PortalFacet.class))
      {
         request.fire(new InstallFacets(PortalFacet.class));
      }
      if (project.hasFacet(PortalFacet.class))
      {
         ShellMessages.success(out, "Portal arquillian extension is installed.");
      }
   }
}
