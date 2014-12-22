/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve

import org.jetbrains.annotations.ReadOnly

import java.util.Collections
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.resolve.calls.CallChecker
import org.jetbrains.jet.lang.resolve.calls.NeedSyntheticCallChecker
import org.jetbrains.jet.lang.resolve.calls.ReifiedTypeParameterSubstitutionCheck
import org.jetbrains.jet.lang.resolve.calls.InlineCallCheckerWrapper

public abstract class AdditionalCheckerProvider {

    public val annotationCheckers: List<AnnotationChecker> by Delegates.lazy {
        with (arrayListOf<AnnotationChecker>()) {
            addAll(additionalAnnotationCheckers)
            this
        }
    }

    public val callCheckers: List<CallChecker> by Delegates.lazy {
        with (arrayListOf<CallChecker>()) {
            addAll(defaultCallCheckers)
            addAll(additionalCallCheckers)
            this
        }
    }

    protected abstract val additionalAnnotationCheckers: List<AnnotationChecker>
    protected abstract val additionalCallCheckers: List<CallChecker>

    private val defaultCallCheckers: List<CallChecker> = listOf(
            ReifiedTypeParameterSubstitutionCheck(),
            InlineCallCheckerWrapper()
    )

    public object Empty : AdditionalCheckerProvider() {

        override val additionalAnnotationCheckers: List<AnnotationChecker> = listOf()
        override val additionalCallCheckers: List<CallChecker> = listOf()
    }
}
