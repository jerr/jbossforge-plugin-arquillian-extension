package org.jboss.forge.arquillian.extension.persistence;

import java.util.List;

import org.jboss.forge.project.Facet;
import org.jboss.forge.resources.Resource;

/**
 * @author Jérémie Lagarde
 * 
 */
public interface PersistenceFacet extends Facet
{

   List<Resource<?>> getDataSetFiles();
}
