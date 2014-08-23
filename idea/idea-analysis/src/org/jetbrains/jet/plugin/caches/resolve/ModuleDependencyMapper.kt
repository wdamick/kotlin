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

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.openapi.project.Project
import org.jetbrains.jet.context.GlobalContext
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.jet.utils.keysToMap
import com.intellij.openapi.roots.LibraryOrderEntry
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.jet.analyzer.AnalyzerFacade
import org.jetbrains.jet.analyzer.ResolverForModule
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.storage.ExceptionTracker
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.analyzer.ResolverForProject
import org.jetbrains.jet.analyzer.ModuleContent
import com.intellij.psi.search.GlobalSearchScope

fun createModuleResolverProvider(
        project: Project,
        analyzerFacade: AnalyzerFacade<ResolverForModule, JvmPlatformParameters>,
        syntheticFiles: Collection<JetFile>
): ModuleResolverProvider {
    if (!MultiModuleAnalysisInIdea.enabled) {
        return singleModuleResolverProvider(project, analyzerFacade, syntheticFiles)
    }

    val allModuleInfos = collectAllModuleInfosFromIdeaModel(project).toHashSet()

    val globalContext = GlobalContext()

    fun createResolverForProject(): ResolverForProject<IdeaModuleInfo, ResolverForModule> {
        val syntheticFilesByModule = syntheticFiles.groupBy { it.getModuleInfo() }
        allModuleInfos.addAll(syntheticFilesByModule.keySet())

        val modulesContent = {(module: IdeaModuleInfo) ->
            ModuleContent(syntheticFilesByModule[module] ?: listOf(), module.contentScope())
        }

        val jvmPlatformParameters = JvmPlatformParameters {
            (javaClass: JavaClass) ->
            val psiClass = (javaClass as JavaClassImpl).getPsi()
            psiClass.getModuleInfo()
        }

        val resolverForProject = analyzerFacade.setupResolverForProject(
                globalContext, project, allModuleInfos, modulesContent, jvmPlatformParameters
        )
        return resolverForProject
    }

    val resolverForProject = createResolverForProject()

    val moduleToBodiesResolveSession = allModuleInfos.keysToMap {
        module ->
        val analyzer = resolverForProject.resolverForModule(module)
        ResolveSessionForBodies(project, analyzer.lazyResolveSession)
    }
    return MultiModuleResolverProvider(
            resolverForProject,
            moduleToBodiesResolveSession,
            globalContext.exceptionTracker
    )
}

private fun collectAllModuleInfosFromIdeaModel(project: Project): List<IdeaModuleInfo> {
    val ideaModules = ModuleManager.getInstance(project).getModules().toList()
    val modulesSourcesInfos = ideaModules.map { it.toSourceInfo() }

    //TODO: (module refactoring) include libraries that are not among dependencies of any module
    val ideaLibraries = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance(javaClass<LibraryOrderEntry>()).map {
            it.getLibrary()
        }
    }.filterNotNull().toSet()

    val librariesInfos = ideaLibraries.map { LibraryInfo(project, it) }

    val ideaSdks = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance(javaClass<JdkOrderEntry>()).map {
            it.getJdk()
        }
    }.filterNotNull().toSet()

    val sdksInfos = ideaSdks.map { SdkInfo(project, it) }

    val collectAllModuleInfos = modulesSourcesInfos + librariesInfos + sdksInfos
    return collectAllModuleInfos
}

trait ModuleResolverProvider {
    fun resolverByModule(module: IdeaModuleInfo): ResolverForModule
    fun resolveSessionForBodiesByModule(module: IdeaModuleInfo): ResolveSessionForBodies
    val exceptionTracker: ExceptionTracker
}

class MultiModuleResolverProvider(
        private val resolverForProject: ResolverForProject<IdeaModuleInfo, *>,
        private val bodiesResolveByModule: Map<IdeaModuleInfo, ResolveSessionForBodies>,
        override val exceptionTracker: ExceptionTracker
) : ModuleResolverProvider {
    override fun resolverByModule(module: IdeaModuleInfo): ResolverForModule = resolverForProject.resolverForModule(module)

    override fun resolveSessionForBodiesByModule(module: IdeaModuleInfo) =
            //NOTE: if this assert fails in production, additional information can be obtained by logging on the call site
            bodiesResolveByModule[module] ?: throw AssertionError("Requested data for $module not contained in this resolver.")
}

private fun singleModuleResolverProvider(
        project: Project,
        analyzerFacade: AnalyzerFacade<ResolverForModule, JvmPlatformParameters>,
        syntheticFiles: Collection<JetFile>
): ModuleResolverProvider {
    val dummyModuleInfo: IdeaModuleInfo = NotUnderContentRootModuleInfo
    val globalContext = GlobalContext()
    val oneModuleResolver = analyzerFacade.setupResolverForProject(
            globalContext,
            project,
            listOf(dummyModuleInfo),
            { ModuleContent(syntheticFiles, GlobalSearchScope.allScope(project)) },
            JvmPlatformParameters { dummyModuleInfo }
    )
    val resolverForWholeProject = oneModuleResolver.resolverForModule(dummyModuleInfo)
    val resolveSessionForBodies = ResolveSessionForBodies(project, resolverForWholeProject.lazyResolveSession)
    return SingleModuleResolverProvider(project, globalContext.exceptionTracker, resolverForWholeProject, resolveSessionForBodies)
}

private class SingleModuleResolverProvider(
        val resolveSessionForBodies: Project,
        override val exceptionTracker: ExceptionTracker,
        val resolverForWholeProject: ResolverForModule,
        val resolveSessionForWholeProject: ResolveSessionForBodies)
: ModuleResolverProvider {
    override fun resolverByModule(module: IdeaModuleInfo): ResolverForModule {
        return resolverForWholeProject
    }

    override fun resolveSessionForBodiesByModule(module: IdeaModuleInfo): ResolveSessionForBodies {
        return resolveSessionForWholeProject
    }
}

object MultiModuleAnalysisInIdea {
    val enabled: Boolean = false // TODO: please implement proper switch
}