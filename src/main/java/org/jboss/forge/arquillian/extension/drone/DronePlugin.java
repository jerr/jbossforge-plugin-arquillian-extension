package org.jboss.forge.arquillian.extension.drone;

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.Properties;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.PromptType;
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
import org.jboss.forge.shell.util.ResourceUtil;

/**
 * @author Jérémie Lagarde
 * 
 */
@Alias("arq-drone")
@RequiresFacet({ JavaSourceFacet.class, DroneFacet.class })
@RequiresProject
@Help("A plugin that helps setting up Arquillian Drone extension")
public class DronePlugin implements Plugin
{

   static {
       Properties properties = new Properties();
       properties.setProperty("resource.loader", "class");
       properties.setProperty("class.resource.loader.class",
               "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

       Velocity.init(properties);
   }

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

      if (!project.hasFacet(DroneFacet.class))
      {
         request.fire(new InstallFacets(DroneFacet.class));
      }
      if (project.hasFacet(DroneFacet.class))
      {
         ShellMessages.success(out, "Drone arquillian extension is installed.");
      }
   }

   @Command(value = "create-test", help = "Create a new test class with a default @Deployment method")
   public void createTest(
            @Option(required = false,
                     help = "the package in which to build this test class",
                     description = "source package",
                     type = PromptType.JAVA_PACKAGE,
                     name = "package") final String packageName,
            @Option(required = true, name = "named", help = "the test class name") String name,
            @Option(name = "enableJPA", required = false, flagOnly = true) boolean enableJPA,
            final PipeOut out)
            throws FileNotFoundException
   {
      if (!StringUtils.endsWith(name, "Test"))
      {
         name = name + "Test";
      }

      String testPackage;
      JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
      if ((packageName != null) && !"".equals(packageName))
      {
         testPackage = packageName;
      }
      else if (getPackagePortionOfCurrentDirectory() != null)
      {
         testPackage = getPackagePortionOfCurrentDirectory();
      }
      else
      {
         testPackage = shell.promptCommon(
                  "In which package you'd like to create this Test class, or enter for default",
                  PromptType.JAVA_PACKAGE, java.getBasePackage());
      }
      JavaClass testClass = JavaParser.create(JavaClass.class).setName(name).setPackage(testPackage);

      String basePackage = project.getFacet(JavaSourceFacet.class).getBasePackage();
      
      VelocityContext context = new VelocityContext();
      context.put("package", testClass.getPackage());
      context.put("testName", testClass.getName());
      context.put("basePackage", basePackage);
      context.put("enableJPA", enableJPA);

      StringWriter writer = new StringWriter();
      Velocity.mergeTemplate( "drone/TemplateTest.vtl", "UTF-8", context, writer);

      testClass = JavaParser.parse(JavaClass.class, writer.toString());
      java.saveTestJavaSource(testClass);

      pickup.fire(new PickupResource(java.getTestJavaResource(testClass)));
   }

   /**
    * Retrieves the package portion of the current directory if it is a package, null otherwise.
    * 
    * @return String representation of the current package, or null
    */
   private String getPackagePortionOfCurrentDirectory()
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
