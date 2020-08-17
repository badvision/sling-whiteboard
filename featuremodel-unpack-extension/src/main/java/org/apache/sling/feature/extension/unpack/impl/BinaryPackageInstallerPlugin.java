/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.extension.unpack.impl;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

@Component(service = { InstallTaskFactory.class, ResourceTransformer.class })
@Designate(ocd = BinaryPackageInstallerPlugin.Config.class)
public class BinaryPackageInstallerPlugin implements InstallTaskFactory, ResourceTransformer {
    public static final String BINARY_ARCHIVE_VERSION_HEADER = "Binary-Archive-Version";
    public static final String TYPE_BINARY_ARCHIVE = "binaryarchive";

    @ObjectClassDefinition(name = "Binary Package Installer",
            description = "This component supports installing binary packages into the OSGi installer")
    public @interface Config {
        String directory();

        String overwrite() default "true";

        String [] file_extensions() default {".bin", ".fonts"}; // TODO
    }

    @Activate
    private Config config;

    @Override
    public TransformationResult[] transform(RegisteredResource resource) {
        if (!InstallableResource.TYPE_FILE.equals(resource.getType())
                || !handledExtension(resource.getURL())) {
            return null;
        }

        try {
            try (JarInputStream jis = new JarInputStream(resource.getInputStream())) {
                Manifest mf = jis.getManifest();
                if (!"1".equals(mf.getMainAttributes().getValue(BINARY_ARCHIVE_VERSION_HEADER))) {
                    return null;
                }
            }

            Dictionary<String, Object> dict = resource.getDictionary();
            if (dict == null) {
                dict = new Hashtable<>();
            }

            ArtifactId aid = (ArtifactId) dict.get("artifact.id");
            if (aid == null) {
                String u = resource.getURL();
                int idx = u.lastIndexOf('/');
                String name = u.substring(idx + 1);
                int idx2 = name.lastIndexOf('.');
                if (idx2 >= 0) {
                    name = name.substring(0, idx2);
                }
                aid = new ArtifactId("binary.packages", name, resource.getDigest(), null, null);
            }

            TransformationResult tr = new TransformationResult();
            tr.setResourceType(TYPE_BINARY_ARCHIVE);
            tr.setId(aid.getGroupId() + ":" + aid.getArtifactId());
            tr.setInputStream(resource.getInputStream());

            Map<String, Object> attributes = new HashMap<>();
            // TODO try to read attributes from resource
            Object dir = dict.get("dir");
            attributes.put("dir", dir != null ? dir : config.directory());
            Object ow = dict.get("overwrite");
            attributes.put("overwrite", ow != null ? ow : config.overwrite());
            tr.setAttributes(attributes);

            return new TransformationResult [] {tr};
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    private boolean handledExtension(String url) {
        for (String fe : config.file_extensions()) {
            if (url.endsWith(fe)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InstallTask createTask(TaskResourceGroup group) {
        TaskResource tr = group.getActiveResource();
        if (!TYPE_BINARY_ARCHIVE.equals(tr.getType())) {
            return null;
        }
        if (tr.getState() == ResourceState.UNINSTALL) {
            // TODO
            return null;
        }


        return new InstallBinaryArchiveTask(group, config);
    }

}
