package com.intellij.ml.llm.template.cli

import com.google.gson.Gson
import com.intellij.ml.llm.template.utils.JavaParsingUtils
import kotlinx.cli.*
import java.nio.file.Files
import kotlin.io.path.Path


@ExperimentalCli
fun main(args: Array<String>){

    println("Welcome to my JavaParsing Utils CLI.")
    val parser = ArgParser("example")
    val input by parser.option(ArgType.String, shortName = "i", description = "Input file").required()
    val output by parser.option(ArgType.String, shortName = "o", description = "Output file name").required()
    class CheckIfStatic: Subcommand("checkIfStatic", "Check if method is static?") {
        val methodSignatureString by option(ArgType.String, "methodSignature", "s", "method signature").required()

        override fun execute() {
            Files.write(
                Path(output),
                JavaParsingUtils.isMethodStatic(
                    Path(input), methodSignatureString
                ).toString().toByteArray()
            )
        }
    }
    class FindFieldTypes: Subcommand("findFieldTypes", "Find field names and types for a given class") {
        val className by option(ArgType.String, shortName = "c", description = "Class name").required()
        override fun execute() {
            Files.write(
                Path(output),
                Gson().toJson(
                        JavaParsingUtils.findFieldTypes(Path(input), className)
                ).toByteArray()
            )
        }
    }
    class CheckIfClassStatic: Subcommand("checkIfClassStatic", "Check if class is static?") {
        val className by option(ArgType.String, shortName = "c", description = "Class name").required()

        override fun execute() {
            try {
                Files.createFile(Path(output))
            } catch (e: Exception) {
                print("file exists.")
            }
            Files.write(
                Path(output),
                JavaParsingUtils.isClassStatic(
                    Path(input), className
                ).toString().toByteArray()
            )
        }
    }

    class FindTypesInRange: Subcommand("findTypesInRange", "Find the types of variables used in line ranges.") {
        val lineStart by option(ArgType.Int, shortName = "s", description = "start line").required()
        val lineEnd by option(ArgType.Int, shortName = "e", description = "end line").required()

        override fun execute() {
            try {
                Files.createFile(Path(output))
            } catch (e: Exception) {
                print("file exists.")
            }
            Files.write(
                Path(output),
                JavaParsingUtils.findTypesInRange(
                    Path(input), lineStart, lineEnd
                ).toString().toByteArray()
            )
        }
    }

    class CheckIfClassExists: Subcommand("checkIfClassExists", "Check if class is exists") {
        val className by option(ArgType.String, shortName = "c", description = "Class name").required()

        override fun execute() {
            try {
                Files.createFile(Path(output))
            } catch (e: Exception) {
                print("file exists.")
            }
            Files.write(
                Path(output),
                Gson().toJson(
                    JavaParsingUtils.doesClassExist(
                        Path(input), className
                    )
                ).toByteArray()
            )
        }
    }

    parser.subcommands(
        CheckIfStatic(),
        FindFieldTypes(),
        CheckIfClassStatic(),
        CheckIfClassExists())
    parser.parse(args)

}