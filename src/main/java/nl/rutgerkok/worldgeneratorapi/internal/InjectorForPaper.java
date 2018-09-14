package nl.rutgerkok.worldgeneratorapi.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import nl.rutgerkok.worldgeneratorapi.WorldGeneratorApi;

final class InjectorForPaper {

    static void inject(String worldName, ChunkProviderServer chunkProvider) throws ReflectiveOperationException {
        // Additional injections for Paper. We need to tell Paper that our generation is
        // NOT a custom generator, as we want to run async.

        Field isCustomGenerator = ReflectionUtil.getFieldByName(chunkProvider,
                "isCustomGenerator");
        if (!isCustomGenerator.getBoolean(chunkProvider)) {
            return;
        }

        // Don't register as a custom generator
        isCustomGenerator.setBoolean(chunkProvider, false);

        // Kick generation executor threads into live
        // (If isCustomGenerator == true, then chunk generation happens on the main
        // thread. However, by changing that value to false after the ChunkProvider was
        // created, we need to spawn the thread ourselves: otherwise the server will
        // wait forever on a thread that doesn't exist.)
        Field generationExecutor = ReflectionUtil.getFieldByName(chunkProvider, "generationExecutor");
        Constructor<?> constructor = generationExecutor.getType().getConstructor(String.class, int.class);
        generationExecutor.set(chunkProvider,
                constructor.newInstance(WorldGeneratorApi.class.getSimpleName() + "-" + worldName, 1));
    }
}
