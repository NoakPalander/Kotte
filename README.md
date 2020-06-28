# Discord Kotte
![Kotte-logo](.kotte.jpg)

Kotte is a simple discord bot easily configured by the user via the json files. It's developed with Kotlin and utilizes certain Java-frameworks, such as 
 - JDA
 - Jackson JSON

### Current release
Works on all platforms that has Java, Kotlin and Gradle support.
Tested on
 - Kubuntu 19.04
 - Manjaro 20.0.3
 - Java 11
 - Gradle 6.4.1

#### Building
Building is easy on linux, just as running
```bash
gradle build                                    # Invokes gradle-build
gradle run                                      # Invokes gradle-run
```

##### Configuring and running
To run the bot, edit the "configurations.json" file and enter valid paths, etc.. Edit the resource/ServerConfig.json and resource/BotConfig.json.

License
----
MIT License

Copyright (c) [2020] [Noak Palander]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
