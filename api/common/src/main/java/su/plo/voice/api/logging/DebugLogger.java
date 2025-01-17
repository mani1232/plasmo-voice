package su.plo.voice.api.logging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.Logger;

@RequiredArgsConstructor
@Accessors(fluent = true)
public final class DebugLogger {

    private final Logger logger;
    @Setter
    @Getter
    private boolean enabled;

    public void log(String message, Object... params) {
        if (!enabled) return;
        logger.info(message, params);
    }
}
