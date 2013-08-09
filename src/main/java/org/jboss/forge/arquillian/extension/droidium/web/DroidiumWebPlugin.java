/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.forge.arquillian.extension.droidium.web;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;
import org.jboss.forge.shell.util.ResourceUtil;

/**
 * Configures Arquillian Droidium web extension and creates skeleton test.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
@Alias(value = "arquillian-droidium-web")
@RequiresFacet({ JavaSourceFacet.class, ResourceFacet.class, DroidiumWebFacet.class })
@RequiresProject
@Help("A plugin that helps manage the Arquillian Droidium web extension")
public class DroidiumWebPlugin implements Plugin {

    @Inject
    private Project project;

    @Inject
    private Event<InstallFacets> request;

    @Inject
    private Event<PickupResource> pickup;

    @Inject
    private Shell shell;

    @SetupCommand
    public void setup(final PipeOut out) {
        if (!project.hasFacet(DroidiumWebFacet.class)) {
            request.fire(new InstallFacets(DroidiumWebFacet.class));
        }
        if (project.hasFacet(DroidiumWebFacet.class)) {
            ShellMessages.success(out, "Arquillian Droidium web extension is installed.");
        }
    }

    @Command(value = "configure-droidium-web", help = "Configures Droidium web extension")
    public void configureDroidiumWeb(
        @Option(name = "serverApk",
            description = "Sets path to Android server APK from Selenium project.",
            defaultValue = "android-server.apk") final String serverApk,
        @Option(name = "logFile",
            description = "Sets path to log file where communication with Android device during testing is saved.",
            defaultValue = "target/android.log") final String logFile,
        final PipeOut out
        ) throws Exception {

        ResourceFacet resources = project.getFacet(ResourceFacet.class);
        FileResource<?> resource = (FileResource<?>) resources.getTestResourceFolder().getChild("arquillian.xml");

        Node xml = null;
        if (!resource.exists()) {
            xml = createNewArquillianConfig();
        } else {
            xml = XMLParser.parse(resource.getResourceInputStream());
            addPropertyToArquillianConfig(xml, "droidium-web", "serverApk", serverApk);
            addPropertyToArquillianConfig(xml, "droidium-web", "logFile", logFile);
        }

        resource.setContents(XMLParser.toXMLString(xml));
    }

    private void addPropertyToArquillianConfig(Node xml, String extension, String key, String value) {
        xml.getOrCreate("extension@qualifier=" + extension).getOrCreate("configuration")
            .getOrCreate("property@name=" + key)
            .text(value);
    }

    private Node createNewArquillianConfig()
    {
        return XMLParser
            .parse("<arquillian xmlns=\"http://jboss.org/schema/arquillian\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "            xsi:schemaLocation=\"http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd\"></arquillian>");
    }

    @Command(value = "create-test", help = "Creates a new Droidium web test class with a default @Deployment method")
    public void createTest(
        @Option(required = false,
            help = "the package in which to build this test class",
            description = "source package",
            type = PromptType.JAVA_PACKAGE,
            name = "package") final String packageName,
        @Option(required = true, name = "named", help = "the test class name") String name,
        final PipeOut out) throws Exception {

        if (!name.endsWith("Test")) {
            name += "Test";
        }

        String testPackage;

        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

        if ((packageName != null) && !"".equals(packageName)) {
            testPackage = packageName;
        } else if (getPackagePortionOfCurrentDirectory() != null) {
            testPackage = getPackagePortionOfCurrentDirectory();
        } else {
            testPackage = shell.promptCommon("In which package you would like to create this test class or enter for default",
                PromptType.JAVA_PACKAGE, java.getBasePackage());
        }

        JavaClass testClass = JavaParser.create(JavaClass.class).setName(name).setPackage(testPackage);
        testClass.addImport("java.io.File");
        testClass.addImport("java.net.URL");
        testClass.addImport("org.jboss.arquillian.container.test.api.Deployment");
        testClass.addImport("org.jboss.arquillian.container.test.api.OperateOnDeployment");
        testClass.addImport("org.jboss.arquillian.container.test.api.RunAsClient");
        testClass.addImport("org.jboss.arquillian.drone.api.annotation.Drone");
        testClass.addImport("org.jboss.arquillian.junit.Arquillian");
        testClass.addImport("org.jboss.arquillian.junit.InSequence");
        testClass.addImport("org.jboss.arquillian.test.api.ArquillianResource");
        testClass.addImport("org.jboss.shrinkwrap.api.Archive");
        testClass.addImport("org.jboss.shrinkwrap.api.ShrinkWrap");
        testClass.addImport("org.jboss.shrinkwrap.api.spec.WebArchive");
        testClass.addImport("org.junit.Test");
        testClass.addImport("org.junit.runner.RunWith");
        testClass.addImport("org.openqa.selenium.android.AndroidDriver");

        testClass.addAnnotation("RunWith").setLiteralValue("Arquillian.class");
        testClass.addAnnotation("RunAsClient");

        Method<JavaClass> createDeployment = testClass.addMethod().setName("createDeployment").setStatic(true)
            .setReturnType("Archive<?>").setPublic();
        createDeployment.addAnnotation("Deployment").setLiteralValue("name", "\"jbossas\"")
            .setLiteralValue("testable", "false");
        createDeployment.setBody("return ShrinkWrap.createFromZipFile(WebArchive.class, new File(\"your.war\"));");

        Method<JavaClass> testMethod = testClass.addMethod()
            .setName("test")
            .setPublic()
            .setReturnType("void");

        testMethod.addAnnotation("org.junit.Test");
        testMethod.addAnnotation("org.jboss.arquillian.junit.InSequence").setLiteralValue("1");
        testMethod.addAnnotation("org.jboss.arquillian.container.test.api.OperateOnDeployment").setLiteralValue("\"jbossas\"");
        testMethod.setParameters("@Drone AndroidDriver driver, @ArquillianResource URL deploymentURL");
        testMethod.setBody("\ndriver.get(deploymentURL.toString());\n");

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
