java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n -ea -d64 -cp target/test-driver-java.jar \
                    io.github.hyunikn.testbot.Main $@
