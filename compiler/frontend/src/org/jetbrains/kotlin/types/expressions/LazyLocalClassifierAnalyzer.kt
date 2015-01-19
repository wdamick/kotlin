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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider
import org.jetbrains.kotlin.types.DynamicTypesSettings
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters
import com.google.common.base.Predicates
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.lazy.data.JetClassInfoUtil
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassContext
import org.jetbrains.kotlin.di.InjectorForLazyLocalClassResolve
import org.jetbrains.kotlin.resolve.lazy.DeclarationScopeProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.lazy.descriptors.DelegatingLazyClassContext
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer

public class LazyLocalClassifierAnalyzer(
        private val resolveSession: KotlinCodeAnalyzer,
        private val lazyClassContext: LazyClassContext
) : LocalClassifierAnalyzer() {

    public class LocalClassDescriptorProvider(
            val c: LazyClassContext,
            val containingDeclaration: DeclarationDescriptor
    ) : LazyTopDownAnalyzer.LazyClassDescriptorProvider() {
        override fun getClassDescriptor(classOrObject: JetClassOrObject): ClassDescriptorWithResolutionScopes {
            return LazyClassDescriptor(
                c,
                containingDeclaration,
                classOrObject.getNameAsName(),
                JetClassInfoUtil.createClassLikeInfo(classOrObject)
            )
        }
    }

    override fun processClassOrObject(
            globalContext: GlobalContext,
            scope: WritableScope?,
            context: ExpressionTypingContext,
            containingDeclaration: DeclarationDescriptor,
            classOrObject: JetClassOrObject,
            additionalCheckerProvider: AdditionalCheckerProvider,
            dynamicTypesSettings: DynamicTypesSettings
    ) {
        val topDownAnalysisParameters = TopDownAnalysisParameters.createForLocalDeclarations(
                globalContext.storageManager,
                globalContext.exceptionTracker,
                Predicates.equalTo(classOrObject.getContainingFile()))

        val c = TopDownAnalysisContext(topDownAnalysisParameters)
        c.setOuterDataFlowInfo(context.dataFlowInfo)

        val lazyTopDownAnalyzer = InjectorForLazyLocalClassResolve(
                classOrObject.getProject(),
                globalContext,
                resolveSession,
                context.trace,
                additionalCheckerProvider,
                dynamicTypesSettings,
                LocalClassDescriptorProvider(
                        object : DelegatingLazyClassContext(lazyClassContext) {
                            override val scopeProvider = object : DeclarationScopeProvider {
                                override fun getResolutionScopeForDeclaration(elementOfDeclaration: PsiElement): JetScope {
                                    return resolveSession.getScopeProvider().getResolutionScopeForDeclaration(elementOfDeclaration) {
                                        assert (it == classOrObject) { "Unexpected local declaration: $it, expected: $classOrObject" }
                                        context.scope
                                    }
                                }

                            }
                        },
                        containingDeclaration
                )
        ).getLazyTopDownAnalyzer()

        lazyTopDownAnalyzer.analyzeDeclarations(topDownAnalysisParameters, listOf(classOrObject))
    }
}