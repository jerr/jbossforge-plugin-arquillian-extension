package org.jboss.forge.arquillian.extension.byteman;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.arquillian.extension.drone.BrowserType;
import org.jboss.forge.arquillian.extension.portal.PortalFacet;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

@Alias("arquillian-byteman")
@RequiresFacet({ JavaSourceFacet.class, ResourceFacet.class, BytemanFacet.class })
@RequiresProject
@Help("A plugin that helps manage the Arquillian Byteman extension")
public class BytemanPlugin implements Plugin
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
	         ShellMessages.success(out, "Arquillian Portal extension is installed.");
	      }
	   }

	   @Inject
	   private Shell shell;

	   @Command(value = "configure")
	   public void configureWebdriver(
	            @Option(name = "autoInstallAgent",
	                     description = "If true the extension will attempt to install the Byteman Agent in the target Container runtime. If false it assumes the Byteman Agent is manually installed. autoInstallAgent requires tools.jar on the container classpath to perform the installation.",
	                     defaultValue = "false") final boolean autoInstallAgent,
	            @Option(name = "agentProperties", description = "Additional properties to use when auto installing the Byteman Agent. See the Byteman documentation for more details.") String agentProperties,
	            final PipeOut out)
	            throws Exception
	   {

	      ResourceFacet resources = project.getFacet(ResourceFacet.class);
	      FileResource<?> resource = (FileResource<?>) resources.getTestResourceFolder().getChild("arquillian.xml");

	      Node xml = null;
	      if (!resource.exists())
	      {
	         ShellMessages.error(shell, "Cannot edit '" + resource.getFullyQualifiedName()
	                  + "': No such resource exists");
	         return;
	      }
	      else
	      {
	         xml = XMLParser.parse(resource.getResourceInputStream());
	      }
	      addPropertyToArquillianConfig(xml, "webdriver", "autoInstallAgent", Boolean.toString(autoInstallAgent));
	      addPropertyToArquillianConfig(xml, "webdriver", "agentProperties", agentProperties);
	      resource.setContents(XMLParser.toXMLString(xml));
	   }


	   private void addPropertyToArquillianConfig(Node xml, String qualifier, String key, String value)
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
}
