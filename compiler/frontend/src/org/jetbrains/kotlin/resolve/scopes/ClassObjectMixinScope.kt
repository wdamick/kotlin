/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.ClassDescriptor

/**
 * Members of the class object are accessible from the class.
 * Scope lazily delegates requests to class object scope.
 */
public class ClassObjectMixinScope(private val classObjectDescriptor: ClassDescriptor) : AbstractScopeAdapter() {
    override val workerScope: JetScope
        get() = classObjectDescriptor.getDefaultType().getMemberScope()

    override fun getImplicitReceiversHierarchy() = listOf(classObjectDescriptor.getThisAsReceiverParameter())
}
