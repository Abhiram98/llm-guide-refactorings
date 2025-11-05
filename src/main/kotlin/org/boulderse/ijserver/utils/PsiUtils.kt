package org.boulderse.ijserver.utils

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.endOffset
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.endLine
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.startLine
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.j2k.accessModifier
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi.psiUtil.textRangeWithoutComments
import kotlin.math.sqrt

class PsiUtils {
    companion object {
        fun getParentClassOrNull(
            editor: Editor,
            language: Language?,
        ): PsiElement? {
            val psiElement = PsiUtilBase.getElementAtCaret(editor)
            when (language) {
                JavaLanguage.INSTANCE -> return PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
            }
            return null
        }

        fun getClassBodyStartLine(psiClass: PsiClass): Int = psiClass.getLineNumber(true) + 1

        fun getParentFunctionOrNull(
            psiElement: PsiElement?,
            language: Language?,
        ): PsiElement? {
            when (language) {
                JavaLanguage.INSTANCE -> return PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java)
                KotlinLanguage.INSTANCE -> return PsiTreeUtil.getParentOfType(psiElement, KtNamedFunction::class.java)
            }
            return null
        }

        fun getParentFunctionOrNull(
            editor: Editor,
            language: Language?,
        ): PsiElement? = getParentFunctionOrNull(PsiUtilBase.getElementAtCaret(editor), language)

        fun getParentFunctionBlockOrNull(
            psiElement: PsiElement?,
            language: Language?,
        ): PsiElement? {
            if (psiElement == null) return null
            return getFunctionBlockOrNull(getParentFunctionOrNull(psiElement, language))
        }

        fun getFunctionBlockOrNull(psiElement: PsiElement?): PsiElement? {
            when (psiElement) {
                is PsiMethod -> return PsiTreeUtil.getChildOfType(psiElement, PsiCodeBlock::class.java)
                is KtNamedFunction -> return PsiTreeUtil.getChildOfType(psiElement, KtBlockExpression::class.java)
            }
            return null
        }

        fun getFunctionBodyStartLine(psiElement: PsiElement?): Int {
            val block = getFunctionBlockOrNull(psiElement)
            var startLine = -1
//            when(psiElement){
//                is PsiClass -> 1
//            }
            if (block != null) {
                startLine = block.firstChild.getLineNumber(false) + 1
            }
            return startLine
        }

        fun getParentFunctionCallOrNull(
            psiElement: PsiElement,
            language: Language,
        ): PsiElement? {
            when (language) {
                KotlinLanguage.INSTANCE ->
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, KtDotQualifiedExpression::class.java)
                        ?: PsiTreeUtil.getTopmostParentOfType(psiElement, KtCallExpression::class.java)
            }
            return null
        }

        fun elementsBelongToSameFunction(
            start: PsiElement,
            end: PsiElement,
            language: Language,
        ): Boolean {
            val parentFunctionStart = getParentFunctionOrNull(start, language) ?: return false
            val parentFunctionEnd = getParentFunctionOrNull(end, language) ?: return false
            return parentFunctionStart == parentFunctionEnd
        }

        fun isElementArgumentOrArgumentList(
            psiElement: PsiElement,
            language: Language,
        ): Boolean {
            when (language) {
                KotlinLanguage.INSTANCE -> {
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, KtValueArgumentList::class.java) != null
                }

                JavaLanguage.INSTANCE -> {
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, PsiExpressionList::class.java) != null
                }
            }
            return false
        }

        fun isElementParameterOrParameterList(
            psiElement: PsiElement?,
            language: Language,
        ): Boolean {
            when (language) {
                KotlinLanguage.INSTANCE -> {
                    val x = PsiTreeUtil.getTopmostParentOfType(psiElement, KtParameterList::class.java)
                    return x != null
                }

                JavaLanguage.INSTANCE -> {
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, PsiParameterList::class.java) != null
                }
            }
            return false
        }

        fun getVariableFromPsi(
            psiElement: PsiElement?,
            variableName: String,
        ): PsiElement? {
            var foundVariable: PsiElement? = null

            class VariableFinder : JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    if (variable.name == variableName) {
                        foundVariable = variable
                    }
                }

                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    super.visitReferenceExpression(expression)
                    if (expression.referenceName == variableName) {
                        foundVariable = expression.reference?.resolve()
                    }
                }
//                override fun visitIdentifier(identifier: PsiIdentifier) {
//                    super.visitIdentifier(identifier)
//                    if (identifier.text == variableName)
//                        foundVariable = identifier
//                }
            }
            if (psiElement != null) {
                psiElement.accept(VariableFinder())
            }
            if (foundVariable != null && psiElement != null) {
                return foundVariable
            }
            return null
        }

        fun getAllVariableFromPsi(
            psiElement: PsiElement?,
            variableName: String,
        ): List<PsiElement> {
            var foundVariables: MutableSet<PsiElement> = mutableSetOf()

            class VariableFinder : JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    if (variable.name == variableName) {
                        foundVariables.add(variable)
                    }
                }

//                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
//                    super.visitReferenceExpression(expression)
//                    if (expression.referenceName==variableName)
//                        expression.reference?.resolve()?.let { foundVariables.add(it) }
//                }
                override fun visitField(field: PsiField) {
                    super.visitField(field)
                    if (field.name == variableName) {
                        foundVariables.add(field)
                    }
                }

                override fun visitClass(aClass: PsiClass) {
                    super.visitClass(aClass)
                    if (aClass.name == variableName) {
                        foundVariables.add(aClass)
                    }
                }

                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    if (method.name == variableName) {
                        foundVariables.add(method)
                    }
                }

                override fun visitParameter(parameter: PsiParameter) {
                    super.visitParameter(parameter)
                    if (parameter.name == variableName) {
                        foundVariables.add(parameter)
                    }
                }
            }
            if (psiElement != null) {
                psiElement.accept(VariableFinder())
            }
            return foundVariables.toList()
        }

        fun getVariableAndReferencesFromPsi(
            psiElement: PsiElement?,
            variableName: String,
        ): List<PsiElement> {
            var matches: MutableList<PsiElement> = mutableListOf()

            class VariableFinder : JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    if (variable.name == variableName) {
                        matches.add(variable)
                    }
                }

                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    super.visitReferenceExpression(expression)
                    if (expression.referenceName == variableName) {
                        matches.add(expression)
                    }
                }
            }
            if (psiElement != null) {
                psiElement.accept(VariableFinder())
            }
            return matches
        }

        fun getLeftmostPsiElement(
            lineNumber: Int,
            editor: Editor,
            file: PsiFile,
        ): PsiElement? {
            // get the PsiElement on the given lineNumber
            var psiElement: PsiElement = file.findElementAt(editor.document.getLineStartOffset(lineNumber)) ?: return null

            // if there are multiple sibling PsiElements on the same line, look for the first one
            while (psiElement.getLineNumber(false) == psiElement.prevSibling?.getLineNumber(false)) {
                psiElement = psiElement.prevSibling
            }

            // if we are still on a PsiWhiteSpace, then go right
            while (psiElement.getLineNumber(false) == psiElement.nextSibling?.getLineNumber(false) && psiElement is PsiWhiteSpace) {
                psiElement = psiElement.nextSibling
            }

            // if there are multiple parent PsiElements on the same line, look for the top one
            val psiElementLineNumber = psiElement.getLineNumber(false)
            while (true) {
                if (psiElement.parent == null) break
                if (psiElement.parent is PsiCodeBlock || psiElement.parent is KtBlockExpression) break
                if (psiElementLineNumber != psiElement.parent.getLineNumber(false)) break
                psiElement = psiElement.parent
            }

            // move to next non-white space sibling
            while (psiElement is PsiWhiteSpace) {
                psiElement = psiElement.nextSibling
            }

            return psiElement
        }

        fun <T : PsiElement> getElementsOfTypeOnLine(
            file: PsiFile,
            editor: Editor,
            lineNumber: Int,
            clazz: Class<out T>,
        ): List<T> {
            val elementAtStartLine =
                PsiUtilBase.getElementAtOffset(
                    file,
                    editor.document.getLineStartOffset(lineNumber - 1),
                )

            val startOffset = editor.document.getLineStartOffset(lineNumber - 1)
            val endOffset = editor.document.getLineStartOffset(lineNumber)
            val foundElements =
                PsiTreeUtil
                    .findChildrenOfType(elementAtStartLine.parent, clazz)
                    .filter { it.startOffset in (startOffset..endOffset) }
            return foundElements
        }

        fun getMethodNameFromClass(
            outerClass: PsiElement?,
            methodName: String,
        ): PsiMethod? {
            var match: PsiMethod? = null

            class MethodFinder : JavaRecursiveElementVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    if (method.name == methodName) {
                        match = method
                    }
                }
            }
            if (outerClass != null) {
                outerClass.accept(MethodFinder())
            }
            return match
        }

        fun getAllMethodNameFromClass(
            outerClass: PsiElement?,
            methodName: String,
        ): List<PsiMethod> {
            val match = mutableListOf<PsiMethod>()

            class MethodFinder : JavaRecursiveElementVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    if (method.name == methodName) {
                        match.add(method)
                    }
                }
            }
            if (outerClass != null) {
                outerClass.accept(MethodFinder())
            }
            return match
        }

        fun getAllElementsOfName(
            outerClass: PsiElement?,
            nameToSearch: String,
        ): List<PsiElement> {
            val match = mutableSetOf<PsiElement>()

            class NameFinder : JavaRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if ((element as? PsiNameIdentifierOwner)?.name == nameToSearch) {
                        match.add(element)
                    }
                    if ((element as? PsiReferenceExpression) != null) {
                        print("found reference.")
//                        if (element.namedUnwrappedElement?.name == nameToSearch){
// //                            match.add((element as? PsiReferenceExpression)!!.namedUnwrappedElement!!)
//                            match.add(element)
//                        } else
                        if (element.resolve()?.namedUnwrappedElement?.name == nameToSearch) {
//                            match.add(element.resolve()!!)
                            match.add(element)
                        }
                    }
                    if ((element as? PsiJavaCodeReferenceElementImpl != null) &&
                        (element as PsiJavaCodeReferenceElementImpl).resolve()?.namedUnwrappedElement?.name == nameToSearch
                    ) {
//                        match.add((element as PsiJavaCodeReferenceElementImpl).resolve()!!)
                        match.add(element)
                    }
                }
            }
            outerClass?.accept(NameFinder())
            return match.toList()
        }

        fun getAllElementsWithNameMatching(
            outerClass: PsiElement?,
            nameToSearch: String,
        ): List<PsiElement> {
            val match = mutableSetOf<PsiElement>()

            class NameFinder : JavaRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if ((element as? PsiNameIdentifierOwner)?.name?.contains(nameToSearch) ?: false) {
                        match.add(element)
                    }
                    if ((element as? PsiReferenceExpression) != null) {
                        print("found reference.")
//                        if (element.namedUnwrappedElement?.name == nameToSearch){
// //                            match.add((element as? PsiReferenceExpression)!!.namedUnwrappedElement!!)
//                            match.add(element)
//                        } else
                        if (element
                                .resolve()
                                ?.namedUnwrappedElement
                                ?.name
                                ?.contains(nameToSearch) ?: false
                        ) {
//                            match.add(element.resolve()!!)
                            match.add(element)
                        }
                    }
                    if ((element as? PsiJavaCodeReferenceElementImpl != null) &&
                        (element as PsiJavaCodeReferenceElementImpl)
                            .resolve()
                            ?.namedUnwrappedElement
                            ?.name
                            ?.contains(nameToSearch)
                        ?: false
                    ) {
//                        match.add((element as PsiJavaCodeReferenceElementImpl).resolve()!!)
                        match.add(element)
                    }
                }
            }
            outerClass?.accept(NameFinder())
            return match.toList()
        }

        fun getQualifiedTypeInFile(
            psiFile: PsiFile,
            typeName: String,
        ): String? {
            var match: String? = null

            class TypeFinder : JavaRecursiveElementVisitor() {
                override fun visitTypeElement(type: PsiTypeElement) {
                    super.visitTypeElement(type)
                    if (type.text == typeName &&
                        (type.type as PsiClassReferenceType).resolve() != null
                    ) {
                        match = (type.type as PsiClassReferenceType).resolve()?.qualifiedName
                    }
                }

                override fun visitImportStatement(statement: PsiImportStatement) {
                    super.visitImportStatement(statement)

                    val childrenOfTypeReference = statement.childrenOfType<PsiJavaCodeReferenceElement>()
                    if (childrenOfTypeReference.isNotEmpty() &&
                        childrenOfTypeReference[0].referenceName == typeName
                    ) {
                        match = childrenOfTypeReference[0].qualifiedName
                    }
                }
            }
            psiFile.accept(TypeFinder())
            return match
        }

        fun isMethodStatic(psiMethod: PsiMethod): Boolean = runReadAction { psiMethod.modifierList.text.contains("static") }

        fun getVariableOfType(
            psiElement: PsiElement,
            typeName: String,
        ): PsiElement? {
            var match: PsiElement? = null

            class TypeFinder : JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    if (variable.type
                            ?.canonicalText
                            ?.split(".")
                            ?.last() == typeName
                    ) {
                        match = variable
                    }
                }

                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    super.visitReferenceExpression(expression)
                    if (expression.type
                            ?.canonicalText
                            ?.split(".")
                            ?.last() == typeName
                    ) {
                        match = expression.resolve()
                    }
                }

                override fun visitField(field: PsiField) {
                    super.visitField(field)
                    if (field.type.canonicalText
                            .split(".")
                            .last() == typeName
                    ) {
                        match = field
                    }
                }
            }
            psiElement.accept(TypeFinder())
            return match
        }

        fun searchForPsiElement(
            outerPsiElement: PsiElement,
            elementToSearchFor: PsiElement,
        ): PsiElement? {
            var match: PsiElement? = null
            val psiManager =
                PsiManager.getInstance(
                    outerPsiElement.project,
                )
            val elementHash = elementToSearchFor.text.filter { !it.isWhitespace() }

            class ElementFinder : JavaRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if (element.text.filter { !it.isWhitespace() } == elementHash) {
                        match = element
                    }
                }
            }

            outerPsiElement.accept(ElementFinder())
            return match
        }

        fun fetchClassesInPackage(
            containingClass: PsiClass,
            project: Project,
        ): List<PsiClass> {
            val javaFile = containingClass.containingFile as PsiJavaFile
            val psiPackage =
                JavaPsiFacade
                    .getInstance(project)
                    .findPackage(javaFile.packageName)

            if (psiPackage != null) {
                println("Number of classes in the package: ${psiPackage.classes.size}")
                return runReadAction { psiPackage.classes.toList() }
            }

            return emptyList()
        }

//        fun fetchUtilityClassesInProject(containingClass: PsiClass, project: Project): List<PsiClass> {
//            val utilityClasses: MutableList<PsiClass> = mutableListOf()
//
//            // Retrieve all classes in the project
//            val allClasses = PsiUtils.fetchClassesInProject(containingClass, project)
//
//            for (psiClass in allClasses) {
//                val className = psiClass.name?.toLowerCase() ?: ""
//                val fileName = psiClass.containingFile?.name?.toLowerCase() ?: ""
//
//                // Check if either the class name or file name contains "util" or "utility"
//                if (className.contains("util") || className.contains("utility") ||
//                    fileName.contains("util") || fileName.contains("utility")) {
//                    utilityClasses.add(psiClass)
//                }
//            }
//
//            println("Number of utility classes found: ${utilityClasses.size}")
//            return utilityClasses
//        }

        fun fetchPrioritizedClasses(
            containingClass: PsiClass,
            project: Project,
        ): List<PsiClass> {
            val classList = PsiUtils.fetchClassesInProject(containingClass, project)

            // Define weights for each criterion
            val STATIC_RATIO_WEIGHT = 1.0
            val PACKAGE_PROXIMITY_WEIGHT = 1.0
            val UTILITY_CLASS_WEIGHT = 2.0 // Higher weight to give more importance to utility classes

            // Source package name for package proximity calculation
            val sourcePackageName = (containingClass.containingFile as? PsiJavaFile)?.packageName ?: ""

            // Create a list to store classes with their computed weights
            val classWithWeights = mutableListOf<Pair<PsiClass, Double>>()

            for (psiClass in classList) {
                val className = psiClass.name?.lowercase() ?: ""
                val fileName = psiClass.containingFile?.name?.lowercase() ?: ""

                // Determine if the class is a utility class
                val isUtilityClass =
                    className.contains("util") || className.contains("utility") ||
                        fileName.contains("util") || fileName.contains("utility")

                // Skip classes without methods
                val classMethods = psiClass.methods
                if (classMethods.isEmpty()) continue

                // Calculate the static to instance method ratio
                val staticMethods = classMethods.count { PsiUtils.isMethodStatic(it) }
                val staticRatio = staticMethods.toDouble() / classMethods.size

                // Calculate package proximity
                val targetPackageName = (psiClass.containingFile as? PsiJavaFile)?.packageName ?: ""
                val packageProximity = calculatePackageProximity(sourcePackageName, targetPackageName)

                // Assign a utility class bonus if applicable
                val utilityBonus = if (isUtilityClass) UTILITY_CLASS_WEIGHT else 0.0

                // Combine weights: Static ratio, package proximity, and utility class bonus
                val combinedWeight =
                    (STATIC_RATIO_WEIGHT * staticRatio) +
                        (PACKAGE_PROXIMITY_WEIGHT * packageProximity) +
                        utilityBonus

                // Store the class and its combined weight
                classWithWeights.add(Pair(psiClass, combinedWeight))
            }

            // Sort classes by their combined weight in descending order
            return classWithWeights.sortedByDescending { it.second }.map { it.first }
        }

        private fun calculatePackageProximity(
            sourcePackage: String,
            targetPackage: String,
        ): Double {
            if (sourcePackage == targetPackage) return 1.0

            val sourceSegments = sourcePackage.split(".")
            val targetSegments = targetPackage.split(".")
            val commonSegments = sourceSegments.zip(targetSegments).count { it.first == it.second }

            // Normalize by the number of segments in the source package to get a value between 0.0 and 1.0
            return commonSegments.toDouble() / sourceSegments.size
        }

//        fun fetchPrioritizedClassesByStaticRatio(containingClass: PsiClass, project: Project): List<PsiClass> {
//            val classList = PsiUtils.fetchClassesInProject(containingClass, project)
//
//            // Create a map to store classes and their weights based on the two criteria
//            val classWithWeights = mutableListOf<Pair<PsiClass, Double>>()
//
//            val sourcePackageName = (containingClass.containingFile as? PsiJavaFile)?.packageName ?: ""
//
//            for (psiClass in classList) {
//                val classMethods = psiClass.methods
//                if (classMethods.isEmpty()) continue
//
//                // Calculate the static to instance method ratio
//                val staticMethods = classMethods.count { PsiUtils.isMethodStatic(it) }
// //                val instanceMethods = classMethods.size - staticMethods
//                val staticRatio = staticMethods.toDouble() / classMethods.size
//
//                // Calculate package proximity
//                val targetPackageName = (psiClass.containingFile as? PsiJavaFile)?.packageName ?: ""
//                val packageProximity = calculatePackageProximity(sourcePackageName, targetPackageName)
//
//                // Combine weights: Static ratio (higher is better) and package proximity (closer is better)
//                val combinedWeight = staticRatio + packageProximity
//
//                // Store class and combined weight
//                classWithWeights.add(Pair(psiClass, combinedWeight))
//            }
//
//            // Sort classes by their weight (higher weight at the top)
//            return classWithWeights.sortedByDescending { it.second }.map { it.first }
//        }
//
//        // Helper function to calculate package proximity (e.g., string similarity between package names)
//        private fun calculatePackageProximity(sourcePackage: String, targetPackage: String): Double {
//            if (sourcePackage == targetPackage) return 1.0
//
//            val sourceSegments = sourcePackage.split(".")
//            val targetSegments = targetPackage.split(".")
//            val commonSegments = sourceSegments.zip(targetSegments).count { it.first == it.second }
//
//            return commonSegments.toDouble() / sourceSegments.size
//        }

        internal class MyGlobalSearchScope(
            project: Project?,
        ) : GlobalSearchScope(project) {
            private val index = ProjectRootManager.getInstance(project!!).fileIndex

            init {
            }

            override fun isSearchInLibraries(): Boolean = false

            override fun contains(file: VirtualFile): Boolean = index.isInSourceContent(file)

            override fun isSearchInModuleContent(aModule: com.intellij.openapi.module.Module): Boolean = false
        }

        fun fetchClassesInProject(
            containingClass: PsiClass,
            project: Project,
        ): List<PsiClass> {
            val classList: MutableList<PsiClass> = mutableListOf()
            AllClassesSearch
                .search(MyGlobalSearchScope(project), project)
                .allowParallelProcessing()
                .forEach(
                    Processor<PsiClass> { psiClass: PsiClass ->
//                        psiClass: PsiClass-> classList.add(psiClass)
                        if (psiClass.methods.isNotEmpty() &&
                            !psiClass.isEnum &&
                            !psiClass.isInterface &&
                            !psiClass.hasModifierProperty(PsiModifier.ABSTRACT) &&
                            !psiClass.isAnnotationType
                        ) {
                            classList.add(psiClass)
                        }
                        true
                    },
                )
            return classList
        }

        fun getClassesByName(
            project: Project,
            name: String,
        ): List<PsiClass> {
            val classList: MutableList<PsiClass> = mutableListOf()
            AllClassesSearch
                .search(MyGlobalSearchScope(project), project)
                .allowParallelProcessing()
                .forEach(
                    Processor<PsiClass> { psiClass: PsiClass ->
                        if (psiClass.name == name) {
                            classList.add(psiClass)
                        }
                        true
                    },
                )
            return classList
        }

        fun fetchImportsInFile(
            file: PsiFile,
            project: Project,
        ): List<PsiClass> {
            return file
                .childrenOfType<PsiImportStatement>()
                .map {
                    if (it.qualifiedName == null) return@map null
                    if (isInProject(it.qualifiedName!!, project)) {
                        return@map findClassFromQualifier(it.qualifiedName!!, project)
                    }
                    null
                }.filterNotNull()
        }

        fun findClassFromQualifier(
            canonicalType: @NlsSafe String,
            project: Project,
        ): PsiClass? =
            JavaPsiFacade
                .getInstance(project)
                .findClass(canonicalType, project.projectScope())

        fun isInProject(
            qualifier: @NlsSafe String,
            project: Project,
        ): Boolean =
            JavaPsiFacade
                .getInstance(project)
                .findClass(qualifier, project.projectScope()) != null

        fun computeCosineSimilarity(
            psiMethod: PsiMethod,
            psiClass: PsiClass,
        ): Double {
            if (psiMethod.text == null) return 0.0
            if (psiClass.text == null) return 0.0
            val methodBody = psiMethod.text
            val classBody = psiClass.text

            return computeCosineSimilarity(methodBody, classBody)
        }

        private fun tokenize(text: String): List<String> = text.split("\\s+".toRegex()).map { it.lowercase() }

        private fun termFrequency(tokens: List<String>): Map<String, Int> = tokens.groupingBy { it }.eachCount()

        private fun vectorize(
            termFreq: Map<String, Int>,
            vocabulary: Set<String>,
        ): List<Double> =
            vocabulary.map {
                termFreq[it]?.toDouble() ?: 0.0
            }

        private fun cosineSimilarity(
            vectorA: List<Double>,
            vectorB: List<Double>,
        ): Double {
            val dotProduct = vectorA.zip(vectorB).sumOf { it.first * it.second }
            val magnitudeA = sqrt(vectorA.sumOf { it * it })
            val magnitudeB = sqrt(vectorB.sumOf { it * it })

            return if (magnitudeA != 0.0 && magnitudeB != 0.0) {
                dotProduct / (magnitudeA * magnitudeB)
            } else {
                0.0
            }
        }

        fun computeCosineSimilarity(
            textA: String,
            textB: String,
        ): Double {
            val tokensA = tokenize(textA)
            val tokensB = tokenize(textB)

            val vocabulary = (tokensA + tokensB).toSet()

            val vectorA = vectorize(termFrequency(tokensA), vocabulary)
            val vectorB = vectorize(termFrequency(tokensB), vocabulary)

            return cosineSimilarity(vectorA, vectorB)
        }

        fun getMethodWithSignatureFromClass(
            outerClass: PsiElement?,
            signature: MethodSignature,
        ): PsiMethod? {
            var match: PsiMethod? = null

            class MethodFinder : JavaRecursiveElementVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    if (method.name == signature.methodName &&
                        method.accessModifier() == signature.modifier &&
                        method.returnType.toString().split(":")[1] == signature.returnType &&
                        matchMethodParams(method, signature.paramsList)
                    ) {
                        match = method
                    }
                }
            }
            if (outerClass != null) {
                outerClass.accept(MethodFinder())
            }
            return match
        }

        fun matchMethodParams(
            psiMethod: PsiMethod,
            paramsList: List<Parameter>,
        ): Boolean {
            if (psiMethod.parameterList.parameters.size != paramsList.size) {
                return false
            }
            for (param in paramsList.withIndex()) {
                if (param.index >= psiMethod.parameterList.parameters.size) {
                    return false
                }
                if (psiMethod.parameterList.parameters[param.index].name != param.value.name) {
                    return false
                }
                if (psiMethod.parameterList.parameters[param.index]
                        .type
                        .toString()
                        .split(":")[1]
                        .replace(" ", "") != param.value.type
                ) {
                    if (!psiMethod.parameterList.parameters[param.index]
                            .type.canonicalText
                            .endsWith(param.value.type)
                    ) {
                        return false
                    }
                }
            }
            return true
        }

        fun getMethodParameter(
            methodPsi: PsiMethod,
            parameterToFind: Parameter,
        ): PsiParameter? {
            var match: PsiParameter? = null

            class MethodFinder : JavaRecursiveElementVisitor() {
                override fun visitParameter(parameter: PsiParameter) {
                    if (parameter.name == parameterToFind.name &&
                        parameter.type.toString().split(":")[1] == parameterToFind.type
                    ) {
                        match = parameter
                    }
                }
            }
            methodPsi.accept(MethodFinder())
            return match
        }

        fun getAllMethodsInClass(containingClass: PsiClass?): List<PsiMethod> {
            if (containingClass == null) {
                return emptyList()
            }

            val visitedMethods = mutableSetOf<PsiMethod>()

            class MethodFinder : JavaRecursiveElementVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    visitedMethods.add(method)
                }
            }
            containingClass.accept(MethodFinder())
            return visitedMethods.toList()
        }

        fun getAllReferenceExpressions(psiElement: PsiElement): List<PsiReferenceExpression> {
            val visitedReferences = mutableSetOf<PsiReferenceExpression>()

            class ReferenceFinder : JavaRecursiveElementVisitor() {
                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    super.visitReferenceExpression(expression)
                    if (expression.text.contains(".")) {
                        visitedReferences.add(expression)
                    }
                }
            }
            psiElement.accept(ReferenceFinder())
            return visitedReferences.toList()
        }

        fun getLinkedClasses(
            psiClass: PsiClass,
            project: Project,
        ): List<PsiClass> {
            val linkedClasses = mutableListOf<PsiClass>()
            psiClass.superClass?.let { linkedClasses.add(it) }
            psiClass.interfaces.map { linkedClasses.add(it) }
            psiClass.allFields
                .filter {
                    isInProject(it.type.canonicalText, project)
                }.map {
                    findClassFromQualifier(it.type.canonicalText, project)?.let { it1 ->
                        linkedClasses.add(it1)
                    }
                }

            psiClass.methods.forEach {
                linkedClasses.addAll(
                    getCallersCallees(project, it),
                )
            }
            return linkedClasses
        }

        fun getLinkedClasses(
            psiMethod: PsiMethod,
            project: Project,
        ): List<PsiClass> {
            val linkedClasses = mutableListOf<PsiClass>()
            if (psiMethod.text.contains("@Override")) {
                print("found one")
                // add the ovveriding class.
                val containingClass = psiMethod.containingClass
                if (containingClass != null) {
                    val superClass = containingClass.superClass
                    if (superClass != null &&
                        superClass.methods.filter { it.name == psiMethod.name }.isNotEmpty()
                    ) {
                        linkedClasses.add(superClass)
                    }

                    containingClass.interfaces.forEach {
                        if (it.methods.filter { it2 -> it2.name == psiMethod.name }.isNotEmpty()) {
                            linkedClasses.add(it)
                        }
                    }
                }
            }

            linkedClasses.addAll(
                getCallersCallees(project, psiMethod),
            )
            return linkedClasses
        }

        fun getLinkedElements(
            psiMethod: PsiMethod,
            project: Project,
        ): List<PsiElement> {
            psiMethod.startOffsetSkippingComments

            val linkedMethods = mutableListOf<PsiElement>()
            if (psiMethod.text.contains("@Override")) {
                print("found one")
                // add the ovveriding class.
                linkedMethods.addAll(
                    psiMethod.findSuperMethods(),
                )
            }

            psiMethod.forEachOverridingMethod {
                linkedMethods.add(it)
            }

            linkedMethods.addAll(
                getCallerCalleeElements(project, psiMethod),
            )
            return linkedMethods
        }

        private fun getCallersCallees(
            project: Project,
            psiMethod: PsiMethod,
        ): List<PsiClass> =
            getCallerCalleeElements(project, psiMethod).map {
                (it.containingFile as PsiJavaFile).classes.first()
            }

        private fun getCallerCalleeElements(
            project: Project,
            psiMethod: PsiMethod,
        ): MutableList<PsiElement> {
            val linkedElements = mutableListOf<PsiElement>()
            val scope = GlobalSearchScope.projectScope(project)
            ReferencesSearch.search(psiMethod, scope).forEach { reference ->
                linkedElements.add(reference.element)
            }

            getAllReferenceExpressions(psiMethod)
                .map {
                    it.resolve()
                }.filterNotNull()
                .map { linkedElements.add(it) }

            getAllTypes(psiMethod).forEach {
                val javaClass = (it.firstChild as? PsiJavaCodeReferenceElement)?.resolve() as? PsiClass
                if (javaClass != null &&
                    javaClass !in linkedElements
                ) {
                    linkedElements.add(javaClass)
                }
            }
            return linkedElements
        }

        private fun getAllTypes(psiElement: PsiElement): List<PsiTypeElement> {
            val visitedReferences = mutableSetOf<PsiTypeElement>()

            class ReferenceFinder : JavaRecursiveElementVisitor() {
                override fun visitTypeElement(type: PsiTypeElement) {
                    super.visitTypeElement(type)
                    visitedReferences.add(type)
                }
            }
            psiElement.accept(ReferenceFinder())
            return visitedReferences.toList()
        }

        fun getElementMatchingText(
            outerElement: PsiElement,
            matchingText: String,
        ): List<PsiElement> {
            var foundElements: MutableSet<PsiElement> = mutableSetOf()

            class TextFinder : JavaRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if (element.text == matchingText) {
                        foundElements.add(element)
                    }
                }
            }
            outerElement.accept(TextFinder())
            return foundElements.toList()
        }

        fun getElementMatchingTextNoWhiteSpace(
            outerElement: PsiElement,
            matchingText: String,
        ): List<PsiElement> {
            var foundElements: MutableSet<PsiElement> = mutableSetOf()
            val matchWithoutSpace = matchingText.replace("\\s".toRegex(), "")

            class TextFinder : JavaRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if (element.text.replace("\\s".toRegex(), "") == matchWithoutSpace) {
                        foundElements.add(element)
                    }
                }
            }
            outerElement.accept(TextFinder())
            return foundElements.toList()
        }

        fun getAllComments(outerElement: PsiElement): List<PsiComment> {
            var foundElements: MutableSet<PsiComment> = mutableSetOf()

            class CommentFinder : JavaRecursiveElementVisitor() {
                override fun visitComment(comment: PsiComment) {
                    super.visitComment(comment)
                    foundElements.add(comment)
                }
            }
            outerElement.accept(CommentFinder())
            return foundElements.toList()
        }

        fun createPsiElementFromText(
            text: String,
            project: Project,
        ): PsiElement? {
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            val newElement: PsiElement? =
                when {
                    looksLikeImportStatic(text) -> null

                    looksLikeImport(text) ->
                        elementFactory.createImportStatementOnDemand(
                            text
                                .removePrefix("import ")
                                .removeSuffix(";")
                                .trim()
                                .split(" ")
                                .last(),
                        )

                    looksLikeExpression(text) ->
                        elementFactory.createExpressionFromText(text, null)

                    looksLikeStatement(text) ->
                        elementFactory.createStatementFromText(text, null)

                    looksLikeMethod(text) ->
                        elementFactory.createMethodFromText(text, null)

                    looksLikeField(text) ->
                        elementFactory.createFieldFromText(text, null)

                    else -> null
                }
            return newElement
        }

        fun looksLikeImport(text: String): Boolean = text.trim().startsWith("import ")

        fun looksLikeImportStatic(text: String): Boolean = text.trim().startsWith("import static")

        fun looksLikeExpression(text: String): Boolean = !text.contains(";") && !text.contains("{") && !text.contains("class")

        fun looksLikeStatement(text: String): Boolean = text.trim().endsWith(";")

        fun looksLikeMethod(text: String): Boolean = text.contains("(") && text.contains(")") && text.contains("{")

        fun looksLikeField(text: String): Boolean = text.trim().matches(Regex("""(public|private|protected)?\s*\w+\s+\w+\s*(=.*)?;"""))

        fun findAllOverridingMethods(psiMethod: PsiMethod): List<PsiMethod> {
            val overridingMethods = mutableListOf<PsiMethod>(psiMethod)
            psiMethod.forEachOverridingMethod {
                overridingMethods.add(it)
                overridingMethods.addAll(findAllOverridingMethods(it))
                true
            }
            return overridingMethods
        }

        fun getEndLine(it: PsiElement) = it.endLine(it.containingFile.fileDocument)

        fun getStartLine(it: PsiElement): Int {
            var count = 0
            it.containingFile.fileDocument
                .getText(it.textRangeWithoutComments)
                .split("\n")
                .map { it.trimStart().startsWith("@") }
                .forEach {
                    if (it) {
                        count += 1
                    } else {
                        return@forEach
                    }
                }
            return it.containingFile.fileDocument.getLineNumber(it.startOffsetSkippingComments) + count
        }

        fun isCodeElementType(
            it: PsiElement,
            elementType: String,
        ): Boolean {
            val selectedType =
                when (elementType) {
                    "method" -> PsiMethod::class.java
                    "parameter" -> PsiParameter::class.java
//                "class" -> PsiClass::class.java
//                "field" -> PsiField::class.java
//                "variable" -> PsiVariable::class.java

                    else -> null
                }
            if (selectedType == null) {
                return it !is PsiMethod && it !is PsiParameter
            }
            return selectedType.isInstance(it)
        }

        fun getElementTypeStr(it: PsiElement): String? {
            if (it is PsiMethod) {
                return "method"
            } else if (it is PsiClass) {
                return "class"
            } else if (it is PsiField) {
                return "field"
            } else if (it is PsiParameter) {
                return "parameter"
            } else if (it is PsiVariable) {
                return "variable"
            }
            return null
        }

        fun getElementStartEnd(psiElement: PsiElement): Pair<Int, Int> {
            if (psiElement is PsiMethod) {
                return Pair(psiElement.nameIdentifier?.startOffset?:psiElement.startOffset, psiElement.nameIdentifier?.endOffset?:psiElement.endOffset)
            } else if (psiElement is PsiClass) {
                return Pair(psiElement.nameIdentifier?.startOffset?:psiElement.startOffset, psiElement.nameIdentifier?.endOffset?:psiElement.endOffset)
            } else if (psiElement is PsiField) {
                return Pair(psiElement.nameIdentifier.startOffset, psiElement.nameIdentifier.endOffset)
            } else{
                return Pair(psiElement.startOffset, psiElement.endOffset)
            }
        }
    }
}
