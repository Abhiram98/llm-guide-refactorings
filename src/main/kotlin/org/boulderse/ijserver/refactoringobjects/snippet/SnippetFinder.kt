package org.boulderse.ijserver.refactoringobjects.snippet

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class SnippetFinder(
    val file: PsiFile,
    val psiElement: PsiElement,
) {
    fun getSnippet(): String {
        val packageStatement =
            runReadAction { PsiTreeUtil.getChildOfType(file, PsiPackageStatement::class.java)?.text?.plus("\n") ?: "" }
        return packageStatement +
            runReadAction {
                when (psiElement) {
                    is PsiClass -> {
                        snippetizeClass(psiElement, ignoreMethodsAndFields = false)
                    }

                    is PsiField -> {
                        val outerClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
                        var snippet = snippetizeField(psiElement)
                        snippet += "\n"
                        if (outerClass != null) {
                            snippetizeClass(outerClass).replace("// class body here", snippet)
                        } else {
                            snippet
                        }
                    }

                    is PsiParameter, is PsiVariable -> {
                        val outerClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
                        var snippet =
                            PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java)?.text ?: psiElement.text
                        snippet += "\n"
                        if (outerClass != null) {
                            snippetizeClass(outerClass).replace("// class body here", snippet)
                        } else {
                            snippet
                        }
                    }

                    else -> {
                        val outerClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
                        var snippet = psiElement.text
                        snippet += "\n"
                        if (outerClass != null) {
                            snippetizeClass(outerClass).replace("// class body here", snippet)
                        } else {
                            snippet
                        }
                    }
                }
            }
    }

    fun snippetizeClass(
        psiClass: PsiClass,
        ignoreMethodsAndFields: Boolean = true,
    ): String {
        // todo: perhaps get a signature of class + signature of all methods inside it.
        //  Something like this:
        //  class X {
        //  int <field_name>; // fields
        //  public int get_field(); // methods
        //  class Inner extends XYZ { ... }
        //  ...
        //  }
        var signatureText = ""
        for (child in psiClass.children) {
            if (child is PsiJavaToken && child.text == "{") {
                signatureText += "{\n\n"
                break
            }
            signatureText += child.text
        }
        if (!ignoreMethodsAndFields) {
            for (field in psiClass.fields) {
                signatureText += field.text + "\n"
            }
            for (method in psiClass.methods) {
                signatureText += getMethodSignature(method) + "\n"
            }
        } else {
            signatureText += "// class body here\n"
        }

        signatureText += "}"
        return signatureText
    }

    fun snippetizeField(psiField: PsiField): String {
        return psiField.text // todo: perhaps get a small signature of the containing class
    }

    fun getMethodSignature(psiMethod: PsiMethod): String {
        var signatureText = ""
        for (child in psiMethod.children) {
            if (child is PsiJavaToken && child.text == "{") {
                signatureText += "{ ... }"
                break
            }
            signatureText += child.text
        }
        return signatureText
    }
}
