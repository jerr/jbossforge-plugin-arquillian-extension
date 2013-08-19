package org.jboss.forge.arquillian.extension.byteman;

import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.jboss.forge.arquillian.extension.AbstractExtensionPlugin;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.maven.plugins.Configuration;
import org.jboss.forge.maven.plugins.ConfigurationElement;
import org.jboss.forge.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.maven.plugins.MavenPluginAdapter;
import org.jboss.forge.maven.plugins.PluginElement;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

@Alias("arquillian-byteman")
@RequiresFacet({ JavaSourceFacet.class, ResourceFacet.class, BytemanFacet.class })
@RequiresProject
@Help("A plugin that helps manage the Arquillian Byteman extension")
public class BytemanPlugin extends AbstractExtensionPlugin
{

   private static final String PATH_TOOLS_JAR = "path.tools_jar";

   @SetupCommand
   public void setup(final PipeOut out)
   {

      DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);

      if (!project.hasFacet(BytemanFacet.class))
      {
         request.fire(new InstallFacets(BytemanFacet.class));
      }

      MavenCoreFacet mavenCoreFacet = project.getFacet(MavenCoreFacet.class);
      Model pom = mavenCoreFacet.getPOM();
      List<Profile> profileList = pom.getProfiles();
      for (Profile profile : profileList)
      {
         installToolsDotJar(profile, dependencyFacet);
      }
      mavenCoreFacet.setPOM(pom);
      if (project.hasFacet(BytemanFacet.class))
      {
         ShellMessages.success(out, "Arquillian Byteman extension is installed.");
      }
   }

   private void installToolsDotJar(Profile profile, DependencyFacet dependencyFacet)
   {
      String path_tools_jar = dependencyFacet.getProperty(PATH_TOOLS_JAR);
      if (path_tools_jar == null)
      {
         dependencyFacet.setProperty(PATH_TOOLS_JAR, "${java.home}/../lib/tools.jar");
      }

      if (profile.getBuild().getPluginsAsMap().containsKey("org.apache.maven.plugins:maven-surefire-plugin"))
      {
         Plugin plugin = profile.getBuild().getPluginsAsMap().get("org.apache.maven.plugins:maven-surefire-plugin");

         MavenPluginAdapter adapter = new MavenPluginAdapter(plugin);

         if (!adapter.getConfig().hasConfigurationElement("systemPropertyVariables"))
         {
            ConfigurationElementBuilder configElement = ConfigurationElementBuilder.create().setName(
                     "systemPropertyVariables");
            configElement.addChild(PATH_TOOLS_JAR).setText("${path.tools_jar}");
            adapter.getConfig().addConfigurationElement(configElement);
         }
         else
         {
            ConfigurationElement configElementOld = adapter.getConfig().getConfigurationElement(
                     "systemPropertyVariables");
            ConfigurationElementBuilder configElement = ConfigurationElementBuilder.create().setName(
                     "systemPropertyVariables");
            for (PluginElement element : configElementOld.getChildren())
            {
               if (!(element instanceof ConfigurationElement)
                        || !PATH_TOOLS_JAR.equals(((ConfigurationElement) element).getName()))
               {
                  configElement.addChild(element);
               }
            }
            configElement.addChild(PATH_TOOLS_JAR).setText("${path.tools_jar}");
            Configuration config = adapter.getConfig();
            config.removeConfigurationElement("systemPropertyVariables");
            config.addConfigurationElement(configElement);
            adapter.setConfig(config);
         }
         profile.getBuild().removePlugin(plugin);
         profile.getBuild().addPlugin(adapter);
      }
   }

   @Command(value = "configure")
   public void configure(
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
      addPropertyToArquillianConfig(xml, "byteman", "autoInstallAgent", Boolean.toString(autoInstallAgent));
      addPropertyToArquillianConfig(xml, "byteman", "agentProperties", agentProperties);
      resource.setContents(XMLParser.toXMLString(xml));
   }

   @Command(value = "create-test", help = "Create a new test class with a default @Deployment method")
   public void createTest(
            @Option(required = false,
                     help = "the package in which to build this test class",
                     description = "source package",
                     type = PromptType.JAVA_PACKAGE,
                     name = "package") final String packageName,
            @Option(required = true, name = "named", help = "the test class name") String name,
            final PipeOut out)
            throws Exception
   {
      if (!name.endsWith("Test"))
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
      testClass.addImport("java.net.URL");
      testClass.addImport("org.jboss.arquillian.container.test.api.Deployment");
      testClass.addImport("org.jboss.arquillian.extension.byteman.api.BMRule");
      testClass.addImport("org.jboss.arquillian.junit.Arquillian");
      testClass.addImport("org.jboss.shrinkwrap.api.ShrinkWrap");
      testClass.addImport("org.jboss.shrinkwrap.api.asset.EmptyAsset");
      testClass.addImport("org.jboss.shrinkwrap.api.spec.JavaArchive");
      testClass.addImport("org.junit.Test");
      testClass.addImport("org.junit.runner.RunWith");

      testClass.addAnnotation("RunWith").setLiteralValue("Arquillian.class");

      Method<JavaClass> createDeployment = testClass.addMethod().setName("createDeployment").setStatic(true)
               .setPublic();
      createDeployment.setReturnType("JavaArchive");
      StringBuilder body = new StringBuilder();

      body.append("return ShrinkWrap.create(JavaArchive.class,\"").append(name.toLowerCase()).append(".jar\");");
      createDeployment.setBody(body.toString());

      Method<JavaClass> testThrowRule = testClass.addMethod().setName("testThrowRule").setStatic(false).setPublic();
      testThrowRule.addAnnotation("Test").setLiteralValue("expected", "RuntimeException.class");
      testThrowRule.addAnnotation("BMRule").setStringValue("name", "Throw exception on success")
               .setStringValue("targetClass", "Long").setStringValue("targetMethod", "parseLong")
               .setStringValue("action", "throw new java.lang.RuntimeException()");
      testThrowRule.setBody("Long.parseLong(\"1234\");");

      JavaResource javaFileLocation = java.saveTestJavaSource(testClass);

      shell.println("Created Test [" + testClass.getQualifiedName() + "]");
      pickup.fire(new PickupResource(javaFileLocation));
   }

}
