package nl.rutgerkok.worldgeneratorapi.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.block.Biome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.schemas.Schema;

import net.minecraft.server.v1_15_R1.BiomeLayoutOverworldConfiguration;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.WorldChunkManager;
import net.minecraft.server.v1_15_R1.WorldChunkManagerOverworld;
import net.minecraft.server.v1_15_R1.WorldData;
import nl.rutgerkok.worldgeneratorapi.BiomeGenerator;
import nl.rutgerkok.worldgeneratorapi.internal.BiomeGeneratorImpl;
import nl.rutgerkok.worldgeneratorapi.internal.InjectedBiomeGenerator;

public class BiomeGeneratorImplTest {

    private static class DummyDataFixer implements DataFixer {

        @Override
        public Schema getSchema(int var1) {
            return new Schema(var1, null);
        }

        @Override
        public <T> Dynamic<T> update(TypeReference var1, Dynamic<T> var2, int var3, int var4) {
            return var2;
        }

    }

    @BeforeAll
    public static void bootstrap() {
        TestFactory.activateTestServer();
    }

    @Test
    public void isGetStructureBiomesInSync() {
        // Create vanilla biome generator
        WorldData worldData = new WorldData(new NBTTagCompound(), new DummyDataFixer(), 0, null);
        WorldChunkManager worldChunkManager = new WorldChunkManagerOverworld(
                new BiomeLayoutOverworldConfiguration(worldData));

        // Check the structures
        BiomeGenerator biomeGenerator = new BiomeGeneratorImpl(worldChunkManager);
        assertEquals(BiomeGenerator.VANILLA_OVERWORLD_STRUCTURE_BIOMES, biomeGenerator.getStructureBiomes());
    }

    @Test
    public void noDoubleWrapping() {
        BiomeGenerator ours = (x, y, z) -> Biome.PLAINS;

        assertThrows(IllegalArgumentException.class, () -> new BiomeGeneratorImpl(new InjectedBiomeGenerator(ours)));
    }
}
