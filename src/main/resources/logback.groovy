import ch.qos.logback.core.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender(name="CONSOLE", clazz=ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level | %date{ISO8601} | %msg\n"
    }
}

logger(name="com.calvin.streamy", level=DEBUG)
logger(name="com.calvin.streamy.NumbersSource", level=WARN)

root(level=INFO, appenderNames=["CONSOLE"])