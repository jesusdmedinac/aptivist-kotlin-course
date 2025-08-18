
package com.aptivist.kotlin.plugins

import com.aptivist.kotlin.plugins.commands.*
import com.aptivist.kotlin.plugins.examples.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * 🧪 PLUGIN SYSTEM TESTS - PRUEBAS DEL SISTEMA DE PLUGINS (Phase 2.2)
 * 
 * Esta clase demuestra testing de sistemas complejos con coroutines,
 * mostrando conceptos de testing asíncrono y verification de comportamiento.
 * 
 * CONCEPTOS KOTLIN DEMOSTRADOS:
 * • Unit testing con JUnit 5 y kotlin.test
 * • Testing de suspend functions con runBlocking
 * • Setup y teardown con @BeforeEach y @AfterEach
 * • Assertions con kotlin.test functions
 * • Testing de concurrent systems
 * • Mock implementations para testing
 */

class PluginSystemTest {
    
    private lateinit var pluginManager: PluginManager
    private lateinit var commandRegistry: CommandRegistry
    private lateinit var echoPlugin: EchoPlugin
    private lateinit var utilityPlugin: UtilityPlugin
    
    @BeforeEach
    fun setup() {
        pluginManager = PluginManager()
        commandRegistry = CommandRegistry()
        echoPlugin = EchoPlugin()
        utilityPlugin = UtilityPlugin()
    }
    
    @AfterEach
    fun cleanup() = runBlocking {
        pluginManager.shutdown()
        commandRegistry.clear()
    }
    
    @Test
    fun `test plugin initialization and activation`() = runBlocking {
        // PED: Test plugin lifecycle
        val initResult = echoPlugin.initialize()
        assertTrue(initResult.isSuccess, "Plugin initialization should succeed")
        
        val activateResult = echoPlugin.activate()
        assertTrue(activateResult.isSuccess, "Plugin activation should succeed")
        
        assertTrue(echoPlugin.isActive(), "Plugin should be active after activation")
    }
    
    @Test
    fun `test command registry operations`() = runBlocking {
        // PED: Test command registration
        val testCommand = buildCommand {
            metadata(
                name = "test",
                description = "Test command",
                category = CommandCategory.DEVELOPMENT
            )
            
            execute { context ->
                CommandResult.Success("Test executed with args: ${context.arguments}")
            }
        }
        
        val registerResult = commandRegistry.registerCommand(testCommand)
        assertTrue(registerResult.isSuccess, "Command registration should succeed")
        
        // PED: Test command execution
        val executeResult = commandRegistry.executeCommand(
            "test", 
            listOf("arg1", "arg2").toCommandContext()
        )
        
        assertTrue(executeResult is CommandResult.Success, "Command execution should succeed")
        assertTrue(
            (executeResult as CommandResult.Success).output.contains("arg1"), 
            "Command should process arguments"
        )
    }
    
    @Test
    fun `test system commands functionality`() = runBlocking {
        // PED: Register system commands
        SystemCommands.registerSystemCommands(commandRegistry)
        
        // PED: Test help command
        val helpResult = commandRegistry.executeCommand("help", emptyList<String>().toCommandContext())
        assertTrue(helpResult is CommandResult.Help, "Help command should return help result")
        
        // PED: Test version command
        val versionResult = commandRegistry.executeCommand("version", emptyList<String>().toCommandContext())
        assertTrue(versionResult is CommandResult.Success, "Version command should succeed")
        
        // PED: Test status command
        val statusResult = commandRegistry.executeCommand("status", emptyList<String>().toCommandContext())
        assertTrue(statusResult is CommandResult.Success, "Status command should succeed")
    }
    
    @Test
    fun `test echo plugin commands`() = runBlocking {
        // PED: Initialize and activate plugin
        echoPlugin.initialize()
        echoPlugin.activate()
        
        // PED: Register plugin commands
        commandRegistry.registerCommands(echoPlugin.getPluginCommands(), echoPlugin.metadata.id)
        
        // PED: Test echo command
        val echoResult = commandRegistry.executeCommand(
            "echo", 
            listOf("Hello", "World").toCommandContext()
        )
        
        assertTrue(echoResult is CommandResult.Success, "Echo command should succeed")
        assertTrue(
            (echoResult as CommandResult.Success).output.contains("Hello World"), 
            "Echo should contain the message"
        )
        
        // PED: Test plugin-info command
        val infoResult = commandRegistry.executeCommand("plugin-info", emptyList<String>().toCommandContext())
        assertTrue(infoResult is CommandResult.Success, "Plugin info command should succeed")
    }
    
    @Test
    fun `test utility plugin commands`() = runBlocking {
        // PED: Initialize and activate plugin
        utilityPlugin.initialize()
        utilityPlugin.activate()
        
        // PED: Register plugin commands
        commandRegistry.registerCommands(utilityPlugin.getPluginCommands(), utilityPlugin.metadata.id)
        
        // PED: Test datetime command
        val dateResult = commandRegistry.executeCommand("datetime", emptyList<String>().toCommandContext())
        assertTrue(dateResult is CommandResult.Success, "DateTime command should succeed")
        
        // PED: Test random command
        val randomResult = commandRegistry.executeCommand(
            "random", 
            listOf("int", "1", "10").toCommandContext()
        )
        assertTrue(randomResult is CommandResult.Success, "Random command should succeed")
        
        // PED: Test calc command
        val calcResult = commandRegistry.executeCommand(
            "calc", 
            listOf("5", "+", "3").toCommandContext()
        )
        assertTrue(calcResult is CommandResult.Success, "Calc command should succeed")
        assertTrue(
            (calcResult as CommandResult.Success).output.contains("8"), 
            "Calc should return correct result"
        )
    }
    
    @Test
    fun `test command registry statistics`() = runBlocking {
        // PED: Register some commands
        SystemCommands.registerSystemCommands(commandRegistry)
        
        val stats = commandRegistry.getStatistics()
        assertTrue(stats.totalCommands > 0, "Should have registered commands")
        assertTrue(stats.commandsByCategory.isNotEmpty(), "Should have commands by category")
    }
    
    @Test
    fun `test plugin state management`() = runBlocking {
        // PED: Test initial state
        assertTrue(echoPlugin.currentState is PluginState.Unloaded, "Plugin should start unloaded")
        
        // PED: Test initialization
        echoPlugin.initialize()
        assertTrue(echoPlugin.currentState is PluginState.Loaded, "Plugin should be loaded after init")
        
        // PED: Test activation
        echoPlugin.activate()
        assertTrue(echoPlugin.currentState is PluginState.Active, "Plugin should be active after activation")
        
        // PED: Test deactivation
        echoPlugin.deactivate()
        assertTrue(echoPlugin.currentState is PluginState.Loaded, "Plugin should be loaded after deactivation")
        
        // PED: Test shutdown
        echoPlugin.shutdown()
        assertTrue(echoPlugin.currentState is PluginState.Unloaded, "Plugin should be unloaded after shutdown")
    }
    
    @Test
    fun `test command validation`() = runBlocking {
        val commandWithArgs = buildCommand {
            metadata(
                name = "test-validation",
                description = "Test command with validation",
                category = CommandCategory.DEVELOPMENT
            )
            
            argument(
                name = "required-arg",
                description = "Required argument",
                required = true
            )
            
            argument(
                name = "optional-arg",
                description = "Optional argument",
                required = false,
                defaultValue = "default"
            )
            
            execute { context ->
                CommandResult.Success("Args: ${context.arguments}")
            }
        }
        
        commandRegistry.registerCommand(commandWithArgs)
        
        // PED: Test with missing required argument
        val errorResult = commandRegistry.executeCommand(
            "test-validation", 
            emptyList<String>().toCommandContext()
        )
        assertTrue(errorResult is CommandResult.Error, "Should fail with missing required argument")
        
        // PED: Test with valid arguments
        val successResult = commandRegistry.executeCommand(
            "test-validation", 
            listOf("value1").toCommandContext()
        )
        assertTrue(successResult is CommandResult.Success, "Should succeed with valid arguments")
    }
    
    @Test
    fun `test concurrent command execution`() = runBlocking {
        // PED: Register a command that takes time
        val slowCommand = buildCommand {
            metadata(
                name = "slow",
                description = "Slow command for testing",
                category = CommandCategory.DEVELOPMENT
            )
            
            execute { context ->
                kotlinx.coroutines.delay(100) // PED: Simulate slow operation
                CommandResult.Success("Slow command completed")
            }
        }
        
        commandRegistry.registerCommand(slowCommand)
        
        // PED: Execute multiple commands concurrently
        val jobs = (1..5).map { i ->
            kotlinx.coroutines.async {
                commandRegistry.executeCommand("slow", listOf(i.toString()).toCommandContext())
            }
        }
        
        val results = jobs.map { it.await() }
        
        // PED: All should succeed
        assertTrue(results.all { it is CommandResult.Success }, "All concurrent executions should succeed")
    }
}
