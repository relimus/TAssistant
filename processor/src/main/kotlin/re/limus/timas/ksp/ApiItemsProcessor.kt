package re.limus.timas.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

class ApiItemsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("re.limus.timas.annotations.ApiItems")
        val validSymbols = symbols.filter { it.validate() }.toList()

        if (validSymbols.isEmpty()) {
            return emptyList()
        }

        val apiItemsClasses = validSymbols
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { it.qualifiedName?.asString() }
            .toList()

        if (apiItemsClasses.isEmpty()) {
            return emptyList()
        }

        generateApiItemsRegistry(apiItemsClasses)

        return emptyList()
    }

    private fun generateApiItemsRegistry(apiItemsClasses: List<String>) {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = "re.limus.timas.hook.generated",
            fileName = "ApiItemsRegistry"
        )

        OutputStreamWriter(file).use { writer ->
            val imports = apiItemsClasses.joinToString("\n") { classFullName ->
                "import $classFullName"
            }

            val instancesList = apiItemsClasses.joinToString(",\n") { "        ${it.substringAfterLast(".")}"
            }

            writer.write(
                """
                |package re.limus.timas.hook.generated
                |
                |import re.limus.timas.hook.base.XBridge
                |$imports
                |
                |/**
                | * 自动生成的 ApiItems 注册表
                | * 由 KSP 处理器生成，请勿手动修改
                | */
                |object ApiItemsRegistry {
                |    /**
                |     * 所有注册的 ApiItems 实例列表
                |     */
                |    val apiItemsInstances: List<XBridge> = listOf(
                |$instancesList
                |    )
                |}
                |
                """.trimMargin()
            )
        }

        logger.info("Generated ApiItemsRegistry with ${apiItemsClasses.size} hooks")
    }
}

class ApiItemsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ApiItemsProcessor(
            environment.codeGenerator,
            environment.logger
        )
    }
}