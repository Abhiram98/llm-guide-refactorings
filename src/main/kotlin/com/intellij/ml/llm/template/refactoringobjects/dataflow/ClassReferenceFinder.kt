package com.intellij.ml.llm.template.refactoringobjects.dataflow

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch

class ClassReferenceFinder(val psiClass: PsiClass, val project: Project) {
    fun find(): List<String>{
        val scope = GlobalSearchScope.projectScope(project)
        val files = mutableSetOf<String>()

        ReferencesSearch.search(psiClass, scope).allowParallelProcessing()
            .forEach {
                files.add(it.element.containingFile.virtualFile.path
                    .removePrefix(project.basePath.toString()).removePrefix("/")
                )
            }
        return files.toList()
    }
}