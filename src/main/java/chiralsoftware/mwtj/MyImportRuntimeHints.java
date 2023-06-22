package chiralsoftware.mwtj;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Component;

/**
 * Give a hint to native image builder to include the magic numbers resource 
 * needed to determine file types. This works!
 */
@Component
@ImportRuntimeHints(MyImportRuntimeHints.MyRuntimeHints.class)
public class MyImportRuntimeHints {

    static class MyRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("magic.gz");
        }
    }

}
