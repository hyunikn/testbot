java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n -ea -d64 -cp target/testbot-java.jar \
                    io.github.hyunikn.testbot.Main $@
