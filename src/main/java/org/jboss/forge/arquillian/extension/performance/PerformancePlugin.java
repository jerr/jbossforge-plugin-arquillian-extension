package org.jboss.forge.arquillian.extension.performance;

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
@Alias("arq-performance")
@RequiresFacet({ JavaSourceFacet.class, ResourceFacet.class, PerformanceFacet.class })
@RequiresProject
@Help("A plugin that helps setting up Arquillian Performance extension")
public class PerformancePlugin implements Plugin
{

   @Inject
   private Project project;

   @Inject
   private Event<InstallFacets> request;

   @SetupCommand
   public void setup(final PipeOut out)
   {

      if (!project.hasFacet(PerformanceFacet.class))
      {
         request.fire(new InstallFacets(PerformanceFacet.class));
      }
      if (project.hasFacet(PerformanceFacet.class))
      {
         ShellMessages.success(out, "Performance arquillian extension is installed.");
      }
   }
}
