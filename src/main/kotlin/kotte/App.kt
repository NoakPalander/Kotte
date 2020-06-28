package kotte

import kotlin.Exception

fun main() {
    val bot = Bot()
    val resources = ResourceConfig()

    val handles = Handles(bot, resources)

    while (true) {
        try {
            print("[Kotte-CLI]$ ")
            val command: String? = readLine()
            val handle = handles.getHandles()[command]

            if (handle != null)
                handle()
            else
                println("No handle found for the key: $command")
        }
        catch (e: Exception) {
            println("Error: ${e.message}!")
        }
    }
}