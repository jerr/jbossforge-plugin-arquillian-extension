package org.jboss.forge.arquillian.extension.graphene;

import java.io.FileNotFoundException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.arquillian.extension.drone.DroneFacet;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.util.Strings;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
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
@Alias("arq-graphene")
@RequiresFacet({ JavaSourceFacet.class, DroneFacet.class, GrapheneFacet.class })
@RequiresProject
@Help("A plugin that helps setting up Arquillian Graphene extension")
public class GraphenePlugin implements Plugin
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

      if (!project.hasFacet(GrapheneFacet.class))
      {
         request.fire(new InstallFacets(GrapheneFacet.class));
      }
      if (project.hasFacet(GrapheneFacet.class))
      {
         ShellMessages.success(out, "Graphene arquillian extension is installed.");
      }
   }

   @Command(value = "new-page", help = "Create a new graphene page element class")
   public void createTest(
            @Option(required = false,
                     help = "the package in which to build this page class",
                     description = "source package",
                     type = PromptType.JAVA_PACKAGE,
                     name = "package") final String packageName,
            @Option(required = true, name = "named", help = "the page class name") String name,
            @Option(required = false, name = "type", help = "Page Object or Page Fragment", defaultValue = "page") PageType type,
            final PipeOut out)
            throws Exception
   {
      if (PageType.page.equals(type) && !name.endsWith("Page"))
      {
         name = name + "Page";
      }
      if (PageType.fragment.equals(type) && !name.endsWith("Fragment"))
      {
         name = name + "Fragment";
      }
      final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

      String pagePackage;

      if ((packageName != null) && !"".equals(packageName))
      {
         pagePackage = packageName;
      }
      else if (getPackagePortionOfCurrentDirectory() != null)
      {
         pagePackage = getPackagePortionOfCurrentDirectory();
      }
      else
      {
         pagePackage = shell.promptCommon(
                  "In which package you'd like to create this Page, or enter for default",
                  PromptType.JAVA_PACKAGE, java.getBasePackage() + ".pages");
      }

      JavaClass javaClass = JavaParser.create(JavaClass.class)
               .setPackage(pagePackage)
               .setName(name)
               .setPublic();

      if (PageType.fragment.equals(type))
      {
         Field<JavaClass> field = javaClass.addField();
         field.setName("root").setPrivate().setType("org.openqa.selenium.WebElement")
                  .addAnnotation("org.jboss.arquillian.graphene.spi.annotations.Root");
      }
      JavaResource javaFileLocation = java.saveTestJavaSource(javaClass);

      shell.println("Created " + type + " [" + javaClass.getQualifiedName() + "]");
      pickup.fire(new PickupResource(javaFileLocation));
   }

   @Command(value = "new-element", help = "Create a new WebElement in the current class")
   public void newElement(
            @Option(required = true, name = "named", help = "the element name") String name,
            @Option(required = true, name = "findby", help = "the locator name") FindByType findBy,
            @Option(required = true, name = "value", help = "the locator value") String value,
            @Option(required = false, name = "fragmentClass", help = "the Page Fragment class" ) Resource <?> fragmentClass,
            final PipeOut out)
            throws Exception
   {
      final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

      JavaClass javaClass = getJavaClass();


      if (javaClass.hasField(name))
      {
         throw new IllegalStateException("Class already has a field named [" + name + "]");
      }

      Field<JavaClass> field = javaClass.addField();
      field.setName(name).setPrivate();
      if (fragmentClass != null)
      {

         JavaClass javaFragment = JavaParser.parse(JavaClass.class, fragmentClass.getResourceInputStream());
         if (javaFragment == null)
         {
            throw new IllegalStateException("Class notfound in test resources [" + fragmentClass.getFullyQualifiedName() + "]");
         }
         field.setType(javaFragment.getQualifiedName());         
      }
      else
      {
         field.setType("org.openqa.selenium.WebElement");
      }
      Annotation<JavaClass> annotation = field.addAnnotation("org.jboss.arquillian.graphene.enricher.findby.FindBy");
      annotation.setStringValue(findBy.name(), value);
      javaClass.addMethod().setReturnType(field.getTypeInspector().toString())
               .setName("get" + Strings.capitalize(name))
               .setPublic()
               .setBody("return this." + name + ";");

      java.saveTestJavaSource(javaClass);

      shell.println("Created element  [" + field.getName() + "]");
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
            // Have to remember to include the last slash so it's not part of the package
            return currentDirectory.getFullyQualifiedName().replace(r.getFullyQualifiedName() + "/", "")
                     .replaceAll("/", ".");
         }
      }
      return null;
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
