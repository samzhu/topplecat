package io.github.samzhu.topplecat.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/** Internal task that holds reviewer custody exclusively for one verification graph. */
public abstract class ToppleCatAcquireCustodyTask extends DefaultTask {
    @Internal
    public abstract Property<ToppleCatCustodyBuildService> getCustodyService();

    @TaskAction
    public void acquire() {
        getCustodyService().get().acquire();
    }
}
