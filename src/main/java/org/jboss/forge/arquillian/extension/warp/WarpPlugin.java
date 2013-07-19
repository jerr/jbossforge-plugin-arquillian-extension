package org.jboss.forge.arquillian.extension.warp;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.arquillian.extension.drone.DroneFacet;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

/**
 * @author Dan Allen, Jérémie Lagarde
 * 
 */
@Alias("arquillian-warp")
@RequiresFacet({ JavaSourceFacet.class, DroneFacet.class, WarpFacet.class })
@RequiresProject
@Help("A plugin that helps setting up Arquillian Warp extension")
public class WarpPlugin implements Plugin
{

   @Inject
   private Project project;

   @Inject
   private Event<InstallFacets> request;

   @SetupCommand
   public void setup(final PipeOut out)
   {

      if (!project.hasFacet(WarpFacet.class))
      {
         request.fire(new InstallFacets(WarpFacet.class));
      }
      if (project.hasFacet(WarpFacet.class))
      {
         ShellMessages.success(out, "Arquillian Warp extension is installed.");
      }
   }


}
