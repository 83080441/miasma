#!/usr/bin/env node
/**
 * Generate element datapack JSON for all vanilla block-items and living mobs.
 *
 * Prerequisites (once per MC version): extract from the NeoForge sources jar:
 *   jar xf build/moddev/artifacts/minecraft-patched-*-sources.jar \
 *     net/minecraft/references/BlockItemIds.java \
 *     net/minecraft/world/entity/EntityTypeIds.java
 *
 * Run from repo root:
 *   node tools/generate_element_datapack.js
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "..");
const BLOCKS_SRC = path.join(ROOT, "net/minecraft/references/BlockItemIds.java");
const ENTITY_IDS_SRC = path.join(ROOT, "net/minecraft/world/entity/EntityTypeIds.java");
const OUT_ITEMS = path.join(ROOT, "src/main/resources/data/sdfg/element/items/minecraft");
const OUT_ENTITIES = path.join(ROOT, "src/main/resources/data/sdfg/element/entities/minecraft");

const SKIP_BLOCKS = new Set([
  "air", "cave_air", "void_air", "barrier", "structure_void", "light",
  "moving_piston", "end_gateway", "end_portal", "nether_portal", "bubble_column",
  "water", "lava", "fire", "soul_fire",
]);

const SKIP_ENTITIES = new Set([
  "player", "item", "experience_orb", "experience_bottle", "area_effect_cloud",
  "fishing_bobber", "lightning_bolt", "marker", "block_display", "item_display",
  "text_display", "interaction", "armor_stand", "painting", "item_frame",
  "glow_item_frame", "leash_knot", "evoker_fangs", "eye_of_ender", "firework_rocket",
  "llama_spit", "shulker_bullet", "small_fireball", "fireball", "dragon_fireball",
  "wither_skull", "arrow", "spectral_arrow", "trident", "snowball", "egg",
  "ender_pearl", "potion", "falling_block", "tnt", "end_crystal",
  "ominous_item_spawner", "breeze_wind_charge", "wind_charge", "mannequin",
]);

function clamp(n) {
  return Math.max(1, Math.min(100, n | 0));
}

function writeJson(file, elements) {
  const cleaned = {};
  for (const [k, v] of Object.entries(elements)) {
    if (v > 0) cleaned[k] = clamp(v);
  }
  if (Object.keys(cleaned).length === 0) cleaned.earth = 10;
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify({ elements: cleaned }, null, 2) + "\n", "utf8");
}

function parseCreates(file, regex) {
  const text = fs.readFileSync(file, "utf8");
  const out = new Set();
  let m;
  const re = new RegExp(regex, "g");
  while ((m = re.exec(text)) !== null) out.add(m[1]);
  return [...out].sort();
}

function hasAny(s, keys) {
  return keys.some((k) => s.includes(k));
}

function blockElements(bid) {
  const s = bid;

  if (hasAny(s, ["torch", "lantern", "campfire", "candle", "glowstone", "shroomlight", "froglight", "sea_lantern", "end_rod"])) {
    return { fire: 25, light: 55 };
  }
  if (hasAny(s, ["lava", "magma", "blaze"])) {
    return { fire: 70, darkness: 15 };
  }
  if (s.includes("soul") && hasAny(s, ["torch", "lantern", "campfire", "soil"])) {
    return { fire: 20, darkness: 35, light: 25 };
  }
  if (hasAny(s, ["ice", "snow", "powder_snow", "blue_ice", "packed_ice", "frosted", "kelp", "seagrass", "coral", "prismarine", "sea_pickle", "sponge", "conduit"])) {
    return { water: 60, aether: 10 };
  }
  if (hasAny(s, ["dripstone", "clay", "mud"])) {
    return { water: 25, earth: 40 };
  }
  if (hasAny(s, ["leaf", "leaves", "azalea", "vine", "moss", "wool", "carpet", "banner", "cobweb", "web"])) {
    return { wind: 45, earth: 15 };
  }
  if (hasAny(s, ["glass", "pane"])) {
    return { wind: 30, light: 20, aether: 10 };
  }
  if (hasAny(s, ["sculk", "obsidian", "crying_obsidian", "blackstone", "basalt", "end_stone", "purpur", "chorus", "netherite", "ancient_debris", "reinforced_deepslate"])) {
    return { darkness: 55, miasma: 20, earth: 15 };
  }
  if (hasAny(s, ["deepslate", "bedrock", "coal_block", "coal_ore"])) {
    return { darkness: 30, earth: 40 };
  }
  if (hasAny(s, ["warped", "crimson", "nether", "nylium", "shroom", "mushroom", "spore", "wither", "infested", "sulfur"])) {
    return { miasma: 45, fire: 15, darkness: 20 };
  }
  if (hasAny(s, ["diamond", "emerald", "amethyst", "beacon", "enchanting", "end_portal_frame", "dragon_egg", "spawner", "trial_spawner", "vault", "heavy_core"])) {
    return { aether: 50, light: 25, earth: 10 };
  }
  if (hasAny(s, ["gold", "lapis", "redstone_block", "quartz", "copper_block", "iron_block", "raw_"])) {
    return { aether: 20, earth: 35, fire: 10 };
  }
  if (hasAny(s, ["log", "wood", "plank", "stem", "hyphae", "bamboo", "mangrove", "cherry", "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "pale_oak", "fence", "door", "trapdoor", "sign", "shelf", "ladder", "chest", "barrel", "composter", "beehive", "bookshelf", "lectern"])) {
    return { earth: 40, wind: 15 };
  }
  if (hasAny(s, ["dirt", "grass", "podzol", "mycelium", "farmland", "rooted", "mossy", "muddy", "soil", "path", "sand", "gravel", "terracotta", "concrete"])) {
    return { earth: 55, water: 10 };
  }
  if (hasAny(s, ["wheat", "carrot", "potato", "beet", "pumpkin", "melon", "cocoa", "sugar_cane", "cactus", "berry", "flower", "tulip", "orchid", "lilac", "rose", "peony", "sunflower", "dandelion", "poppy", "allium", "cornflower", "lily", "petals", "wildflowers", "torchflower", "pitcher"])) {
    return { earth: 30, water: 20, light: 15 };
  }
  if (hasAny(s, ["redstone", "repeater", "comparator", "target", "note_block", "daylight", "tripwire", "lever", "button", "pressure_plate", "sensor"])) {
    return { earth: 25, aether: 20, light: 10 };
  }
  if (hasAny(s, ["stone", "cobble", "brick", "andesite", "diorite", "granite", "tuff", "calcite", "ore", "furnace", "smoker", "blast", "anvil", "cauldron", "hopper", "piston", "observer", "dispenser", "dropper", "rail", "wall", "stairs", "slab", "iron_", "copper_"])) {
    return { earth: 50 };
  }
  return { earth: 20 };
}

function entityElements(eid) {
  const s = eid;

  if (hasAny(s, ["cod", "salmon", "tropical", "puffer", "squid", "glow_squid", "dolphin", "turtle", "axolotl", "tadpole", "frog", "guardian", "elder_guardian", "drowned", "nautilus"])) {
    return { water: 65, earth: 10 };
  }
  if (hasAny(s, ["blaze", "magma", "ghast", "strider", "wither_skeleton", "hoglin", "zoglin", "piglin"])) {
    return { fire: 60, darkness: 20, miasma: 15 };
  }
  if (s === "wither") {
    return { darkness: 70, miasma: 50, fire: 20 };
  }
  if (s.includes("creaking")) {
    return { darkness: 55, earth: 30, miasma: 20 };
  }
  if (hasAny(s, ["enderman", "endermite", "shulker", "ender_dragon", "phantom"])) {
    return { darkness: 50, aether: 35, miasma: 15 };
  }
  if (s === "warden") {
    return { darkness: 85, miasma: 55, earth: 20 };
  }
  if (hasAny(s, ["zombie", "husk", "skeleton", "stray", "bogged", "parched", "vex", "camel_husk", "zombie_nautilus"])) {
    return { darkness: 55, miasma: 30, earth: 15 };
  }
  if (s.includes("creeper")) {
    return { earth: 30, miasma: 40, wind: 15 };
  }
  if (hasAny(s, ["spider", "silverfish"])) {
    return { darkness: 35, earth: 25, miasma: 20 };
  }
  if (hasAny(s, ["slime", "magma_cube", "sulfur_cube"])) {
    return { earth: 25, water: 30, miasma: 15 };
  }
  if (s.includes("breeze")) {
    return { wind: 70, aether: 20 };
  }
  if (hasAny(s, ["witch", "vindicator", "evoker", "pillager", "ravager", "illusioner"])) {
    return { darkness: 30, miasma: 25, earth: 20 };
  }
  if (s.includes("allay")) {
    return { aether: 45, wind: 30, light: 25 };
  }
  if (hasAny(s, ["bat", "parrot", "bee", "happy_ghast", "chicken"])) {
    return { wind: 50, light: 15 };
  }
  if (hasAny(s, ["snow_golem", "iron_golem", "copper_golem", "villager", "wandering_trader", "sniffer"])) {
    return { earth: 35, light: 25, aether: 10 };
  }
  if (hasAny(s, ["cow", "pig", "sheep", "horse", "donkey", "mule", "llama", "camel", "goat", "wolf", "cat", "ocelot", "fox", "panda", "polar_bear", "rabbit", "armadillo", "mooshroom"])) {
    return { earth: 45, water: 10 };
  }
  if (s === "ender_dragon") {
    return { darkness: 60, aether: 70, miasma: 30 };
  }
  if (s === "giant") {
    return { earth: 40, darkness: 30, miasma: 20 };
  }
  return { earth: 25, darkness: 10 };
}

function isSkippedEntity(id) {
  if (SKIP_ENTITIES.has(id)) return true;
  if (id.endsWith("_boat") || id.endsWith("_raft") || id.endsWith("_minecart")) return true;
  if (id.includes("boat") || id.includes("minecart") || id.includes("raft")) return true;
  return false;
}

function isSkippedEntity(id) {
  if (SKIP_ENTITIES.has(id)) return true;
  if (id.endsWith("_boat") || id.endsWith("_raft") || id.endsWith("_minecart")) return true;
  if (id.includes("boat") || id.includes("minecart") || id.includes("raft")) return true;
  if (id.endsWith("_fireball") || id.endsWith("_skull") || id.endsWith("_spit")) return true;
  if (id.endsWith("_charge") || id.includes("projectile")) return true;
  if (id.includes("potion") || id.includes("arrow")) return true;
  return false;
}

function main() {
  const blocks = parseCreates(BLOCKS_SRC, String.raw`BlockItemId\.create\("([a-z0-9_/]+)"\)`);
  const entityIds = parseCreates(ENTITY_IDS_SRC, String.raw`create\("([a-z0-9_/]+)"\)`);
  const entities = entityIds.filter((id) => !isSkippedEntity(id));

  let nBlocks = 0;
  for (const bid of blocks) {
    if (SKIP_BLOCKS.has(bid)) continue;
    writeJson(path.join(OUT_ITEMS, `${bid}.json`), blockElements(bid));
    nBlocks++;
  }

  // Clear entity dir so removed skips don't leave stale files
  fs.mkdirSync(OUT_ENTITIES, { recursive: true });
  for (const f of fs.readdirSync(OUT_ENTITIES)) {
    if (f.endsWith(".json")) fs.unlinkSync(path.join(OUT_ENTITIES, f));
  }

  let nEnts = 0;
  for (const eid of entities) {
    writeJson(path.join(OUT_ENTITIES, `${eid}.json`), entityElements(eid));
    nEnts++;
  }

  console.log(`Wrote ${nBlocks} block item files -> ${OUT_ITEMS}`);
  console.log(`Wrote ${nEnts} entity files -> ${OUT_ENTITIES}`);
  console.log(`Parsed ${blocks.length} blocks, ${entityIds.length} entity ids total, ${entities.length} kept`);
}

main();
