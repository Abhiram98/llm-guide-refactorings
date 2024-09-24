package com.intellij.ml.llm.template.utils

import com.github.javaparser.*
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults
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
            val parsedResult = parsed.result.get()
            val matchedMethods = parsedResult.findAll(MethodDeclaration::class.java)
                .filter {
                    methodSignature.compare(it.signature)
                }
            val matchedConstructors = parsedResult.findAll(ConstructorDeclaration::class.java)
                .filter { methodSignature.compare(it.signature) }
            if (matchedMethods.isEmpty() && matchedConstructors.isEmpty()) throw Exception("Couldn't find method in class.")
            return matchedMethods.union(matchedConstructors).filter { it.isStatic }.isNotEmpty()
        }

        fun isMethodStatic(filePath: Path, methodSignature: String): Boolean{
            val signature = MethodSignature.getMethodSignatureParts(methodSignature) ?: throw Exception("Unable to parse method signature.")
            return isMethodStatic(filePath, signature)
        }

        fun findFieldTypes(path: Path, className: String): List<ClassField> {

            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            ).parse(path)
            val parsedResult = parsed.result.get()
            val matchedClasses = parsedResult
                .findAll(ClassOrInterfaceDeclaration::class.java)
                .filter {
                    it.fullyQualifiedName.get() == className
                }
            val matchedEnums = parsedResult.findAll(EnumDeclaration::class.java).filter { it.fullyQualifiedName.get()==className }
            val matchedRecords = parsedResult.findAll(RecordDeclaration::class.java).filter { it.fullyQualifiedName.get()==className }
            if (matchedClasses.isEmpty() && matchedEnums.isEmpty() && matchedRecords.isEmpty()) throw Exception("class not found.")
            return matchedClasses.union(matchedEnums).union(matchedRecords)
                .map {
                    it.fields.map {
                        ClassField(it.variables[0].nameAsString, it.elementType.asString(), it.toString())
                    }
                }.reduce { acc, classFields -> acc + classFields }
        }

        fun isClassStatic(path: Path, className: String): Boolean {

            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            ).parse(path)
            val matchedClasses = parsed.result.get()
                .findAll(ClassOrInterfaceDeclaration::class.java)
                .filter {
                    it.fullyQualifiedName.get() == className
                }
                .map {
                    it.isStatic
                }
            if (matchedClasses.isEmpty()) throw Exception("Couldn't find class.")
            return matchedClasses[0]
        }

        fun doesClassExist(path: Path, className: String): Boolean {
            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            ).parse(path)
            val parsedResult = parsed.result.get()
            val matchedClasses = parsedResult
                .findAll(ClassOrInterfaceDeclaration::class.java)
                .filter {
                    it.fullyQualifiedName.isPresent && it.fullyQualifiedName.get() == className
                }
            val matchedEnums = parsedResult.findAll(EnumDeclaration::class.java).filter { it.fullyQualifiedName.isPresent && it.fullyQualifiedName.get()==className }
            val matchedRecords = parsedResult.findAll(RecordDeclaration::class.java).filter { it.fullyQualifiedName.isPresent && it.fullyQualifiedName.get()==className }
            return matchedClasses.isNotEmpty() || matchedRecords.isNotEmpty() || matchedEnums.isNotEmpty()
        }


        class RangeFinder : VoidVisitorWithDefaults<Range?>() {
            var nodesFound: MutableList<Node> = ArrayList<Node>()


            override fun defaultAction(n: Node?, givenRange: Range?) {
                if (n==null) return
                if (givenRange==null) return
                //Range of element in your code
                val rangeOfNode: Range = n.range.get()

                //If your given two lines contain this node, add it
                if (givenRange.contains(rangeOfNode)) nodesFound.add(n)
                else if (givenRange.overlapsWith(rangeOfNode)) {
                    n.getChildNodes().forEach { child -> child.accept(this, givenRange) }
                }
            }
        }

        fun findTypesInRange(path: Path, startLine: Int, endLine: Int){
            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            ).parse(path)
            val parsedResult = parsed.result.get()
            val rangeFinder = RangeFinder()
            rangeFinder.defaultAction(parsedResult, Range(Position(startLine, 0), Position(endLine, 0)))
            val x =rangeFinder.nodesFound.filter { it is NameExpr }.map{(it as NameExpr)}
        }

    }
}