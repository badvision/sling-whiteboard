/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.osgi.util.features;

import org.osgi.util.features.impl.FeatureServiceImpl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * The Features class is the primary entry point for interacting with the feature model.
 * @ThreadSafe
 */
public class Features {
    private static final FeatureServiceImpl IMPL = new FeatureServiceImpl();

    /**
     * Get a factory which can be used to build feature model entities.
     * @return A builder factory.
     */
    public static BuilderFactory getBuilderFactory() {
        return IMPL.getBuilderFactory();
    }

    /**
     * Read a Feature from JSON
     * @param jsonReader A Reader to the JSON input
     * @return The Feature represented by the JSON
     * @throws IOException When reading fails
     */
    public static Feature readFeature(Reader jsonReader) throws IOException {
        return IMPL.readFeature(jsonReader);
    }

    /**
     * Write a Feature Model to JSON
     * @param feature the Feature to write.
     * @param jsonWriter A Writer to which the Feature should be written.
     * @throws IOException When writing fails.
     */
    public static void writeFeature(Feature feature, Writer jsonWriter) throws IOException {
        IMPL.writeFeature(feature, jsonWriter);
    }

    /**
     * Merge two features into a new feature.
     * @param targetID The ID of the new feature.
     * @param f1 The first feature
     * @param f2 The second feature
     * @param ctx The merge context to use for the merge operation.
     * @return The merged feature.
     */
    public static Feature mergeFeatures(ID targetID, Feature f1, Feature f2, MergeContext ctx) {
        return IMPL.mergeFeatures(targetID, f1, f2, ctx);
    }
}
