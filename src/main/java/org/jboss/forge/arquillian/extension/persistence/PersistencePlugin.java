package org.jboss.forge.arquillian.extension.persistence;

import java.io.FileNotFoundException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.arquillian.completers.TestMethodCompleter;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
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
 * @author Jérémie Lagarde
 * 
 */
@Alias("arquillian-persistence")
@RequiresFacet({ JavaSourceFacet.class, ResourceFacet.class, PersistenceFacet.class })
@RequiresProject
@Help("A plugin that helps manage the Arquillian Persistence extension (APE)")
public class PersistencePlugin implements Plugin
{

   @Inject
   private Project project;

   @Inject
   private Event<InstallFacets> request;

   @Inject
   @Current
   private Resource<?> currentResource;

   @Inject
   private Shell shell;

   @SetupCommand
   public void setup(final PipeOut out)
   {

      if (!project.hasFacet(PersistenceFacet.class))
      {
         request.fire(new InstallFacets(PersistenceFacet.class));
      }
      if (project.hasFacet(PersistenceFacet.class))
      {
         ShellMessages.success(out, "Arquillian Persistence extension is installed.");
      }
   }

   @Command(value = "usingDataSet", help = "Adding dataset to a test.")
   public void newElement(@Option(name = "dataset") String dataset,
            @Option(name = "test", completer = TestMethodCompleter.class) final String test,
            final PipeOut out)
            throws Exception
   {
      final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

      JavaClass javaClass = getJavaClass();

      if (!javaClass.hasMethodSignature(test))
      {
         throw new IllegalStateException("Class does not have a method named [" + test + "]");
      }

      if (dataset == null)
      {
         Resource<?> datasetFile = shell.promptChoiceTyped("What dataset do you want?",
                  project.getFacet(PersistenceFacet.class).getDataSetFiles());
         String testResourceFolder = project.getFacet(ResourceFacet.class).getTestResourceFolder().getFullyQualifiedName();
         dataset = datasetFile.getFullyQualifiedName().substring(testResourceFolder.length() + 1);
                  
      }
      
      Method<JavaClass> method = javaClass.getMethod(test);
      Annotation<JavaClass> annotation = method.addAnnotation("org.jboss.arquillian.persistence.UsingDataSet");
      annotation.setStringValue(dataset);
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
