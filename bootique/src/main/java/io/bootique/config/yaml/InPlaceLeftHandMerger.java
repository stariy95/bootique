/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
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

package io.bootique.config.yaml;

import java.util.function.BinaryOperator;

import io.bootique.yaml.node.Mapping;
import io.bootique.yaml.node.Node;

/**
 * A configuration merger that merges right-hand config argument into left-hand
 * argument. It will try to avoid copying objects as much as possible, so chunks
 * of left and right documents may end up merged in the resulting object. This
 * assumes an app that cares about the merge result and throws away both source
 * and target after the merge.
 *
 * @since 2.0
 */
class InPlaceLeftHandMerger implements BinaryOperator<Node> {

    @Override
    public Node apply(Node target, Node source) {
        if (target == null) {
            return source;
        } else if (source == null) {
            return target;
        }

        if(source.isScalar() && target.isScalar()) {
            return mergeScalars(source, target);
        } else if(source.isSequence() && target.isSequence()) {
            return mergeSequence(source, target);
        } else if(source.isObjectMapping() && target.isObjectMapping()) {
            return mergeObjects(source, target);
        }

        throw new RuntimeException("Incompatible or unsupported node types: " + target + " vs. " + source);
    }

    private Node mergeObjects(Node target, Node source) {
        Mapping targetObject = target.asMapping();
        Mapping srcObject = source.asMapping();

        for(Node key: srcObject.keys()) {
            Node srcChild = srcObject.get(key);
            Node targetChild = targetObject.get(key);
            targetObject.set(key, apply(targetChild, srcChild));
        }
        return targetObject;
    }

    private Node mergeSequence(Node target, Node source) {
        return source;
    }

    private Node mergeScalars(Node target, Node source) {
        // side effect - source becomes mutable
        return source;
    }
}
