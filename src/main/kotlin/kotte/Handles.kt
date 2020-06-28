package kotte

import java.io.File
import kotlin.system.exitProcess
import com.fasterxml.jackson.module.kotlin.*

data class ResourceConfig(val path: String, val botconf: String, val serverconf: String) {
    constructor() : this("", "", "")
}

class Handles(private val bot: Bot, private var config: ResourceConfig) {
    private var isConfigured = false

    private val input = { msg: String -> print(msg); readLine().toString(); }

    private fun isFileValid(name: String): Boolean = File(name).exists()
    private fun isFolderValid(name: String): Boolean = File(name).exists() && File(name).isDirectory

    private fun loadJsonFile(name: String) {
        config = jacksonObjectMapper().readValue(File(name).readLines().joinToString("\n"))
        isConfigured = true
    }

    private fun configure() {
        if (!isConfigured) {
            // Ask if a json file wants to be loaded
            var isChecking = true
            while (isChecking) {
                when (input("json[j] / files[f]$ ").toLowerCase()[0]) {
                    'j' -> {
                        while(true) {
                            var file = input("Json configuration file$ ")
                            if (!file.endsWith(".json")) file += ".json"

                            if (isFileValid(file)) {
                                loadJsonFile(file)
                                isChecking = false
                                break
                            }
                            else
                                println("-- That file doesn't appear to exist!")
                        }
                    }
                    'f' -> {
                        var path: String
                        var botconf: String
                        var serverconf: String

                        while (true) {
                            var file = input("Resource (path)$ ")
                            if (!file.endsWith("/")) file += "/"

                            if (isFolderValid(file)) {
                                path = file
                                break
                            }
                            else
                                println("-- That file doesn't appear to exist!")
                        }
                        while (true) {
                            val file = input("Bot config (json)$ ")
                            if (isFileValid(file)) {
                                botconf = file
                                break
                            }
                            else
                                println("-- That file doesn't appear to exist!")
                        }
                        while (true) {
                            val file = input("Server config (json)$ ")
                            if (isFileValid(file)) {
                                serverconf = file
                                break
                            }
                            else
                                println("-- That file doesn't appear to exist!")
                        }

                        config = ResourceConfig(path, botconf, serverconf)
                        isConfigured = true
                    }
                    else -> println("Invalid option! Please choose either json(j) or files(f)!")
                }
            }
        }
        else
            println("Already configured!")
    }

    private fun boot() {
        if (isConfigured)
            bot.boot(config.path, config.botconf, config.serverconf)
        else
            println("Please configure the resources before trying to boot!")
    }

    private fun reload() {
        configure()
        bot.reload(config.path, config.botconf, config.serverconf)
    }

    private fun reboot() {
        bot.reboot()
    }

    private fun shutdown() {
        bot.shutdown()
    }

    private fun kill(force: Boolean) {
        if (isConfigured) {
            if (!force) {
                if (input("Do you want to kill the bot? Y\\n: ").toLowerCase()[0] == 'y')
                    bot.kill()
            }
            else
                bot.kill()
        }
        else
            println("Can't kill the bot if it's not online!")
    }

    private fun status() {
        if (isConfigured && bot.isOnline)
            println("The bot appears to be online and running!")
        else
            println("The bot does not appear to be online!")
    }

    private fun commands(): String {
        return arrayOf(
                "\n\t--configure/-c: Configures the bot with all the required files",
                "\t--boot/-b: Boots the bot, requires the bot to be configured first",
                "\t--reload/-rl: Reloads the resource files without disconnecting the bot",
                "\t--reboot/-rb: Reboots the bot",
                "\t--shutdown/-sd: Shuts the bot down",
                "\t--kill/-k: Emergency kills the bot after confirmation",
                "\t--kill --force/-k -f: Emergency kills the bot without confirmation",
                "\t--quit/-q: Quits the application",
                "\t--status/-st: Checks if the bot is running or not",
                "\t--help/-h: Displays all commands"
        ).joinToString("\n") + "\n"
    }

    fun getHandles(): HashMap<String, () -> Unit> {
        val out = HashMap<String, () -> Unit>()

        // Configure: Configures the path and files
        out["--configure"] = { configure(); }
        out["-c"] = { configure(); }

        // Boot: Boots the bot
        out["--boot"] = this::boot
        out["-b"] = this::boot

        // Reload: Reload the bot's resources
        out["--reload"] = this::reload
        out["-rl"] = this::reload

        // Reboot: Reboots the bot
        out["--reboot"] = this::reboot
        out["-rb"] = this::reboot

        // Shutdown: Shutdowns the bot
        out["--shutdown"] = this::shutdown
        out["-sd"] = this::shutdown

        // Kill: Emergency kills the bot
        out["--kill"] = { kill(false); }
        out["-k"] = { kill(false); }

        // Kill: Emergency kills the bot with the force command (won't ask for acceptance)
        out["--kill --force"] = { kill(true); }
        out["-k -f"] = { kill(true); }

        // Quit: Quits the program
        out["--quit"] = { exitProcess(0); }
        out["-q"] = { exitProcess(0); }

        // Status: Gets the running status of the bot
        out["--status"] = this::status
        out["-st"] = this::status

        // Help: Gets the available commands
        out["--help"] = { println(commands()); }
        out["-h"] = { println(commands()); }

        return out
    }
}