package com.intellij.ml.llm.template.utils

import com.github.javaparser.*
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidate
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
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
                    try{ methodSignature.compare(it.signature) }
                    catch (e: Exception){false}
                }
            val matchedConstructors = parsedResult.findAll(ConstructorDeclaration::class.java)
                .filter {
                   try { methodSignature.compare(it.signature) }
                   catch (e: Exception){false}
                }
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

        fun findTypesInRange(path: Path, startLine: Int, endLine: Int): List<String> {
            val typeSolver: TypeSolver = CombinedTypeSolver()
            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                    .setSymbolResolver(JavaSymbolSolver(typeSolver))
            ).parse(path)

            val parsedResult = parsed.result.get()

            val rangeFinder = RangeFinder()
            rangeFinder.defaultAction(parsedResult, Range(Position(startLine, 0), Position(endLine+1, 0)))
            val nodesInRange = rangeFinder.nodesFound
                .map{it.findAll(NameExpr::class.java)}
                .reduce { acc, nameExprs -> acc + nameExprs }
                .map {
                    try{ it.resolve().toAst().get() }
                    catch (e: Exception){null}
                }
                .filterNotNull()

            return nodesInRange.filter{ it is VariableDeclarationExpr }.map { (it as VariableDeclarationExpr).variables[0].typeAsString } +
            nodesInRange.filter{ it is FieldDeclaration }.map { (it as FieldDeclaration).variables[0].typeAsString }

        }

        fun findQualifiedTypesInClass(path: Path, className: String, vararg sourceDirs: String): List<ClassField> {
            val typeSolver: TypeSolver = CombinedTypeSolver(
                sourceDirs.map { JavaParserTypeSolver(it) }.toList()
                        + listOf(ReflectionTypeSolver())
            )
            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                    .setSymbolResolver(JavaSymbolSolver(typeSolver))
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
            val parserFacade = JavaParserFacade.get(typeSolver)
            return matchedClasses.union(matchedEnums).union(matchedRecords)
                .map {
                    it.fields.map {
                        val qualifiedName = try{

//                            val fieldType = parserFacade.convertToUsage(it.elementType)
                            val fieldType = it.elementType.resolve()
                            if (fieldType.isReferenceType)
                                fieldType.asReferenceType().qualifiedName
                            else
                                it.elementType.asString()
                        } catch (e: Exception) {
                            println("Failed to resolve type for -> ${it.elementType.asString()}")
                            it.elementType.asString()
                        }
                        ClassField(it.variables[0].nameAsString, qualifiedName, it.toString())
                    }
                }.reduce { acc, classFields -> acc + classFields }

        }

        fun getMethodCount(path: Path): Int {
            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            ).parse(path)
            val parsedResult = parsed.result.get()
            return parsedResult.findAll(MethodDeclaration::class.java).size
        }

        fun getMethodInformation(path: Path, className: String, vararg sourceDirs: String): List<MethodSignature> {
            val typeSolver: TypeSolver = CombinedTypeSolver(
                sourceDirs.map { JavaParserTypeSolver(it) }.toList()
                        + listOf(ReflectionTypeSolver())
            )
            val parsed = JavaParser(
                ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                    .setSymbolResolver(JavaSymbolSolver(typeSolver))
            ).parse(path)

            val parsedResult = parsed.result.get()

//            val matchedClasses = parsedResult
//                .findAll(ClassOrInterfaceDeclaration::class.java)
//                .filter {
//                    it.fullyQualifiedName.get() == className
//                }
//            val matchedEnums = parsedResult.findAll(EnumDeclaration::class.java).filter { it.fullyQualifiedName.get()==className }
//            val matchedRecords = parsedResult.findAll(RecordDeclaration::class.java).filter { it.fullyQualifiedName.get()==className }
//            if (matchedClasses.isEmpty() && matchedEnums.isEmpty() && matchedRecords.isEmpty()) throw Exception("class not found.")


            return parsedResult.findAll(MethodDeclaration::class.java).mapNotNull {
//                MethodSignature(methodName = it.nameAsString, paramsList = it.parameters.map{}, returnType=it., )
                MethodSignature(
                    methodName = it.nameAsString,
                    paramsList = it.parameters.map {
                        it2 ->
                        val qualType = try{
                            val paramType = it2.type.resolve()
                            if (paramType.isReferenceType) {
                                paramType.asReferenceType().qualifiedName
                            } else
                                it2.type.asString()
                        } catch (e: Exception) {
                            it2.type.asString()
                        }
                        Parameter(
                        it2.nameAsString, qualType)

                                                   },
                    modifier = if (it.modifiers.size>0) it.modifiers.get(0).toString() else "",
                    returnType = it.typeAsString,
                    startLineNum = it.begin.get().line,
                    methodNameStartLine = it.name.begin.get().line
                )
            }
        }


    }
}


class MyExtractMethodHelper: LightPlatformCodeInsightTestCase(){
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }
    fun extract(fileName: String, fileText: String, startLine: Int, endLine: Int){
        val document = configureFromFileText(fileName, fileText)

        val offsetStart = document.getLineEndOffset(startLine)
        val offsetEnd = document.getLineEndOffset(endLine)

        val isItExtractable = isCandidateExtractable(
            EFCandidate("test", offsetStart, offsetEnd, startLine, endLine),
            editor,
            file)
        print(isItExtractable)

    }
}