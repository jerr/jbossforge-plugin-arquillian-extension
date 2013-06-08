package org.jboss.forge.arquillian.extension.drone;

import java.net.URL;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
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
@RequiresFacet({ JavaSourceFacet.class, ResourceFacet.class, DroneFacet.class })
@RequiresProject
@Help("A plugin that helps setting up Arquillian Drone extension")
public class DronePlugin implements Plugin
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
            throws Exception
   {
      if (!name.endsWith("Test"))
      {
         name = name + "Test";
      }

      String testPackage;
      
      JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
      ResourceFacet resource = project.getFacet(ResourceFacet.class);
      
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
                  PromptType.JAVA_PACKAGE, java.getBasePackage() + ".view");
      }
      JavaClass testClass = JavaParser.create(JavaClass.class).setName(name).setPackage(testPackage);
      testClass.addImport("java.net.URL");
      testClass.addImport("org.jboss.arquillian.container.test.api.Deployment");
      testClass.addImport("org.jboss.arquillian.drone.api.annotation.Drone");
      testClass.addImport("org.jboss.arquillian.graphene.spi.annotations.Page");
      testClass.addImport("org.jboss.arquillian.junit.Arquillian");
      testClass.addImport("org.jboss.arquillian.test.api.ArquillianResource");
      testClass.addImport("org.jboss.shrinkwrap.api.ShrinkWrap");
      testClass.addImport("org.jboss.shrinkwrap.api.importer.ExplodedImporter");
      testClass.addImport("org.jboss.shrinkwrap.api.spec.WebArchive");
      testClass.addImport("org.junit.Test");
      testClass.addImport("org.junit.runner.RunWith");
      testClass.addImport("org.openqa.selenium.WebDriver");
      
      String basePackage = project.getFacet(JavaSourceFacet.class).getBasePackage();  
  
      Field<JavaClass> webappsrc = testClass.addField();
      webappsrc.setName("WEBAPP_SRC").setPrivate().setStatic(true).setType(String.class).setLiteralInitializer("\"src/main/webapp\"");

      Field<JavaClass> browser = testClass.addField();
      browser.setName("browser").setPrivate().setType("WebDriver");
      browser.addAnnotation("Drone");
      
      Field<JavaClass> baseUrl = testClass.addField();
      baseUrl.setName("baseUrl").setPrivate().setType(URL.class);
      baseUrl.addAnnotation("ArquillianResource");
      
      Method<JavaClass> createDeployment = testClass.addMethod().setName("createDeployment").setStatic(true).setPublic();
      createDeployment.setReturnType("WebArchive");
      createDeployment.addAnnotation("Deployment").setLiteralValue("testable", "false");
      StringBuilder body = new StringBuilder();
      
      body.append("return ShrinkWrap.create(WebArchive.class,\"").append(name.toLowerCase()).append(".war\")");
      body.append("               .addPackages(true, \"").append(basePackage).append("\")\n");
      List<Resource<?>> resources =  resource.getResourceFolder().getChild("META-INF").listResources();
      for (Resource<?> file : resources)
      {
         body.append("               .addAsResource(\"META-INF/").append(file.getName()).append("\", \"META-INF/").append(file.getName()).append("\")");
      }
      body.append("               .as(ExplodedImporter.class).importDirectory(WEBAPP_SRC).as(WebArchive.class);");

      createDeployment.setBody(body.toString());

      Method<JavaClass> testIsDeployed = testClass.addMethod().setName("testIsDeployed").setStatic(false).setPublic();
      testIsDeployed.addAnnotation("org.junit.Test");
      testIsDeployed.setBody("browser.navigate().to(baseUrl);");
      
      JavaResource javaFileLocation = java.saveTestJavaSource(testClass);

      shell.println("Created Test [" + testClass.getQualifiedName() + "]");
      pickup.fire(new PickupResource(javaFileLocation));
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
