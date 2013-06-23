package org.jboss.forge.arquillian.completers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.completer.SimpleTokenCompleter;
import org.jboss.forge.shell.util.ResourceUtil;
/**
 * @author Jérémie Lagarde
 *  
 */
public class TestMethodCompleter extends SimpleTokenCompleter
{
   private final Shell shell;

   @Inject
   public TestMethodCompleter(Shell shell)
   {
      this.shell = shell;
   }

   @Override
   public List<String> getCompletionTokens()
   {
      final List<String> tokens = new ArrayList<String>();
      final Resource<?> currentResource = shell.getCurrentResource();

      try
      {
         final JavaClass javaClass = ResourceUtil.getJavaClassFromResource(currentResource);
         for (Method<JavaClass> method : javaClass.getMethods())
         {
            if (method.hasAnnotation("Test"))
            {
               tokens.add(method.getName());
            }
         }
      }
      catch (FileNotFoundException e)
      {
         throw new RuntimeException(e);
      }

      return tokens;
   }
}
