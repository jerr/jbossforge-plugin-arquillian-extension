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
package org.jboss.forge.arquillian.extension.droidium.native_;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.arquillian.extension.ArquillianExtensionFacet;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.shell.Shell;

/**
 * DroidiumNativeFacetImpl
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class DroidiumNativeFacetImpl extends ArquillianExtensionFacet implements DroidiumNativeFacet {

    @Inject
    public DroidiumNativeFacetImpl(DependencyInstaller installer, Shell shell) {
        super(installer, shell);
    }

    @Override
    protected List<Dependency> getRequiredManagedDependency() {
        return Arrays.asList();
    }

    @Override
    protected List<Dependency> getRequiredDependencies() {
        return Arrays.asList((Dependency) DependencyBuilder
            .create("org.arquillian.extension:arquillian-droidium-native-depchain:0.0.1-SNAPSHOT:test:pom"));
    }
}
