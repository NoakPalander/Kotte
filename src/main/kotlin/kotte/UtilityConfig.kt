package kotte;

data class ServerConfig(val censor: String, val welcomeMessage: String, val goodbyeMessage: String,
                        val admins: ArrayList<String>?, val banned: List<String>?) {

    constructor() : this("", "Welcome", "Goodbye", null, null);
}

data class BotConfig(val name: String, val secret: String, val id: Long, val invite: String, val token: String, val yutoken: String) {
    constructor() : this("", "", 0L, "", "", "");
}
