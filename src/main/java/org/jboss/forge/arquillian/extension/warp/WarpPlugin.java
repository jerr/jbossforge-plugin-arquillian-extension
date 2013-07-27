package org.jboss.forge.arquillian.extension.warp;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.arquillian.completers.TestMethodCompleter;
import org.jboss.forge.arquillian.extension.drone.DroneFacet;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
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

   @Inject
   private Event<PickupResource> pickup;

   @Inject
   @Current
   private Resource<?> currentResource;

   @Inject
   private Shell shell;

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

      ResourceFacet resources = project.getFacet(ResourceFacet.class);
      FileResource<?> resource = (FileResource<?>) resources.getTestResourceFolder().getChild("arquillian.xml");

      if (!resource.exists())
      {
         ShellMessages.warn(out, "arquillian.xml file could not be found!");
      }
      else
      {
         final Node xml = XMLParser.parse(resource.getResourceInputStream());
         Node defaultProtocol = xml.getSingle("defaultProtocol");
         if (defaultProtocol == null || !"Servlet 3.0".equals(defaultProtocol.getAttribute("type")))
         {
            if (shell.promptBoolean(
                     "\"Servlet 3.0\" protocol is required in the arquillian.xml file, do you want to add id?", true))
            {
               final Node node = xml.getOrCreate("defaultProtocol@type=Servlet 3.0");
               //TODO : litle hack to wait FORGE-998
               Node root = new Node(xml.getName()) {

                  @Override
                  public Map<String, String> getAttributes()
                  {
                     return xml.getAttributes();
                  }
                  
                  @Override
                  public List<Node> getChildren()
                  {
                     List<Node> children =  new ArrayList<Node>(xml.getChildren());
                     children.remove(node);
                     children.add(0,node);
                     return Collections.unmodifiableList(children);
                  }
               };
               resource.setContents(XMLParser.toXMLString(root));
            }
         }
      }
   }

   @Command(value = "usingWarp", help = "Adding Wrap to a test method.")
   public void newElement(@Option(name = "test", completer = TestMethodCompleter.class) final String test,
            final PipeOut out)
            throws Exception
   {
      final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

      JavaClass javaClass = getJavaClass();

      if (!javaClass.hasMethodSignature(test))
      {
         throw new IllegalStateException("Class does not have a method named [" + test + "]");
      }

      if (!javaClass.hasAnnotation("org.jboss.arquillian.warp.WarpTest"))
      {
         javaClass.addAnnotation("org.jboss.arquillian.warp.WarpTest");
      }

      Method<JavaClass> method = javaClass.getMethod(test);

      if (!javaClass.hasAnnotation("org.jboss.arquillian.container.test.api.RunAsClient")
               && !method.hasAnnotation("org.jboss.arquillian.container.test.api.RunAsClient"))
      {
         method.addAnnotation("org.jboss.arquillian.container.test.api.RunAsClient");
      }
      javaClass.addImport("org.jboss.arquillian.warp.Activity");
      javaClass.addImport("org.jboss.arquillian.warp.Inspection");
      javaClass.addImport("org.jboss.arquillian.warp.Warp");
      javaClass.addImport("org.jboss.arquillian.warp.servlet.BeforeServlet");

      StringBuilder body = new StringBuilder(method.getBody());
      body.append("Warp\n")
               .append("      .initiate(new Activity() {\n")
               .append("                public void perform() {\n")
               .append("              // TODO : Add activity (ex : browser.navigate().to(contextPath + \"index.html\");)\n")
               .append("          }\n")
               .append("      })\n")
               .append("      .inspect(new Inspection() {\n")
               .append("           private static final long serialVersionUID = 1L;\n")
               .append("\n")
               .append("          @BeforeServlet\n")
               .append("          public void beforeServlet() {\n")
               .append("              System.out.println(\"Hi server! : Message injected by Arquillian Warp!\");\n")
               .append("          }\n")
               .append("      });\n");
      method.setBody(body.toString());
      java.saveTestJavaSource(javaClass);

   }

   private JavaClass getJavaClass() throws FileNotFoundException
   {
      Resource<?> resource = shell.getCurrentResource();
      if (resource instanceof JavaResource)
      {
         return getJavaClassFrom(resource);
      }
      else
      {
         throw new RuntimeException("Current resource is not a JavaResource!");
      }

   }

   private JavaClass getJavaClassFrom(final Resource<?> resource) throws FileNotFoundException
   {
      JavaSource<?> source = ((JavaResource) resource).getJavaSource();
      if (!source.isClass())
      {
         throw new IllegalStateException("Current resource is not a JavaClass!");
      }
      return (JavaClass) source;
   }
}
