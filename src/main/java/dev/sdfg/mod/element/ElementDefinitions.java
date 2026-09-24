package dev.sdfg.mod.element;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * Loads element defaults from datapack JSON under {@code element/items} and {@code element/entities}.
 * Path layout: {@code data/<pack>/element/items/<namespace>/<path>.json}
 * maps to content id {@code namespace:path} (e.g. {@code minecraft/torch} → {@code minecraft:torch}).
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class ElementDefinitions {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Identifier ITEMS_LISTENER_ID =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "element_items");
    private static final Identifier ENTITIES_LISTENER_ID =
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "element_entities");

    private static volatile Map<Identifier, ElementAmounts> ITEMS = Map.of();
    private static volatile Map<Identifier, ElementAmounts> ENTITIES = Map.of();

    private ElementDefinitions() {
    }

    public static ElementAmounts forItem(Identifier id) {
        if (id == null) {
            return ElementAmounts.empty();
        }
        return ITEMS.getOrDefault(id, ElementAmounts.empty());
    }

    public static ElementAmounts forEntityType(Identifier id) {
        if (id == null) {
            return ElementAmounts.empty();
        }
        return ENTITIES.getOrDefault(id, ElementAmounts.empty());
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ITEMS_LISTENER_ID, new DirectoryListener("element/items", map -> {
            ITEMS = map;
            LOGGER.info("Loaded {} item element definition(s)", map.size());
        }));
        event.addListener(ENTITIES_LISTENER_ID, new DirectoryListener("element/entities", map -> {
            ENTITIES = map;
            LOGGER.info("Loaded {} entity element definition(s)", map.size());
        }));
    }

    /** {@code sdfg:minecraft/torch} → {@code minecraft:torch}. */
    static Identifier contentId(Identifier fileId) {
        String path = fileId.getPath();
        int slash = path.indexOf('/');
        if (slash <= 0 || slash >= path.length() - 1) {
            LOGGER.warn("Element datapack path '{}' is missing namespace/path; using as-is", fileId);
            return fileId;
        }
        return Identifier.fromNamespaceAndPath(path.substring(0, slash), path.substring(slash + 1));
    }

    private static final class DirectoryListener extends SimpleJsonResourceReloadListener<ElementAmounts> {
        private final Consumer<Map<Identifier, ElementAmounts>> applier;

        private DirectoryListener(String directory, Consumer<Map<Identifier, ElementAmounts>> applier) {
            super(ElementAmounts.CODEC, FileToIdConverter.json(directory));
            this.applier = applier;
        }

        @Override
        protected void apply(Map<Identifier, ElementAmounts> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<Identifier, ElementAmounts> remapped = new HashMap<>();
            for (Map.Entry<Identifier, ElementAmounts> entry : prepared.entrySet()) {
                Identifier id = contentId(entry.getKey());
                if (remapped.putIfAbsent(id, ElementAmounts.copyOf(entry.getValue())) != null) {
                    LOGGER.warn("Duplicate element definition for {}; keeping first", id);
                }
            }
            this.applier.accept(Collections.unmodifiableMap(remapped));
        }
    }
}
