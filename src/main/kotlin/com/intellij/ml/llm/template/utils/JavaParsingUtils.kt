package com.intellij.ml.llm.template.utils

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import java.nio.file.Path


class JavaParsingUtils {
    companion object{

        fun parseFile(path: String) {
            val parsed = StaticJavaParser.parse(Path.of(path))
            parsed.findAll(MethodDeclaration::class.java)
                .forEach { println("method: ${it.signature}, static=${it.isStatic}") }
        }

        fun isMethodStatic(filePath: Path, methodSignature: MethodSignature): Boolean{
            val parsed = JavaParser(ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)).parse(filePath)
            return parsed.result.get().findAll(MethodDeclaration::class.java)
                .filter {
                    it.isStatic && methodSignature.compare(it.signature)
                }.isNotEmpty()
        }

        fun isMethodStatic(filePath: Path, methodSignature: String): Boolean{
            val signature = MethodSignature.getMethodSignatureParts(methodSignature) ?: return false
            return isMethodStatic(filePath, signature)
        }

        fun findFieldTypes(path: Path, className: String): List<ClassField> {

            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            ).parse(path)
            return parsed.result.get()
                .findAll(ClassOrInterfaceDeclaration::class.java)
                .filter {
                    it.fullyQualifiedName.get() == className
                }
                .map {
                    it.fields.map {
                        ClassField(it.variables[0].nameAsString, it.elementType.asString(), it.toString())
                    }
                }.reduce { acc, classFields -> acc + classFields }
        }

//        fun findFieldTypes2(path: Path, className: String): List<ClassField> {

//        val typeSolver = CombinedTypeSolver()
//        typeSolver.add(ReflectionTypeSolver())
//        val symbolSolver = JavaSymbolSolver(typeSolver)
//            val parsed = JavaParser(ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)).parse(path)
//            return parsed.result.get()
//                .findAll(ClassOrInterfaceDeclaration::class.java)
//                .filter {
//                    it.fullyQualifiedName.get() == className
//                }
//                .map {
//                    it.resolve().allFields.map {
//                        ClassField(it.variables[0].nameAsString, it.elementType.asString(), it.toString())
//                    }
//                }
//                .reduce { acc, classFields -> acc + classFields }
//        }


    }
}