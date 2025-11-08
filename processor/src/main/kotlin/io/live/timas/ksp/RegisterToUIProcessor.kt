package io.live.timas.ksp

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

class RegisterToUIProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("io.live.timas.annotations.RegisterToUI")
        val validSymbols = symbols.filter { it.validate() }.toList()
        
        if (validSymbols.isEmpty()) {
            return emptyList()
        }

        val hookClasses = validSymbols
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { it.qualifiedName?.asString() }
            .toList()

        if (hookClasses.isEmpty()) {
            return emptyList()
        }

        generateHookRegistry(hookClasses)
        
        return emptyList()
    }

    private fun generateHookRegistry(hookClasses: List<String>) {
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = "io.live.timas.hook.generated",
            fileName = "HookRegistry"
        )

        OutputStreamWriter(file).use { writer ->
            // 生成导入语句
            val imports = hookClasses.joinToString("\n") { classFullName ->
                "import $classFullName"
            }
            
            writer.write(
                """
                |package io.live.timas.hook.generated
                |
                |import io.live.timas.hook.base.SwitchHook
                |
                |$imports
                |
                |/**
                | * 自动生成的 Hook 注册表
                | * 由 KSP 处理器生成，请勿手动修改
                | */
                |object HookRegistry {
                |    /**
                |     * 所有注册的 Hook 实例列表
                |     * 直接引用 Hook object 实例，无需反射
                |     */
                |    val hookInstances: List<SwitchHook> = listOf(
                |${hookClasses.joinToString(",\n") { "        ${it.substringAfterLast(".")}" }}
                |    )
                |}
                |
                """.trimMargin()
            )
        }

        logger.info("Generated HookRegistry with ${hookClasses.size} hooks")
    }
}

class RegisterToUIProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return RegisterToUIProcessor(
            environment.codeGenerator,
            environment.logger
        )
    }
}

