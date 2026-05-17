package de.artemis.matterworks.common.matter;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public record MatterRuleConfig(
        Set<ResourceLocation> valueBlacklist,
        Set<ResourceLocation> recyclerBlacklist,
        Set<ResourceLocation> patternBlacklist,
        boolean rejectContainerItems,
        boolean rejectCustomName,
        boolean rejectLore,
        boolean rejectWritableBooks,
        boolean rejectWrittenBooks
) {
    public static MatterRuleConfig defaults() {
        return new MatterRuleConfig(
                ids(
                        "matterworks:raw_matter_bucket",
                        "matterworks:refined_matter_bucket",
                        "matterworks:unstable_matter_bucket",
                        "matterworks:matter_sludge_bucket",
                        "matterworks:matter_dust",
                        "matterworks:empty_template",
                        "matterworks:encoded_template",
                        "matterworks:matter_recycler",
                        "matterworks:matter_stabilizer",
                        "matterworks:matter_analyzer",
                        "matterworks:matter_constructor",
                        "matterworks:matter_generator",
                        "matterworks:power_crystal_ore",
                        "matterworks:crimson_power_crystal",
                        "matterworks:azure_power_crystal",
                        "matterworks:verdant_power_crystal"
                ),
                new LinkedHashSet<>(),
                ids(
                        "matterworks:raw_matter_bucket",
                        "matterworks:refined_matter_bucket",
                        "matterworks:unstable_matter_bucket",
                        "matterworks:matter_sludge_bucket",
                        "matterworks:matter_dust",
                        "matterworks:empty_template",
                        "matterworks:encoded_template",
                        "matterworks:crimson_power_crystal",
                        "matterworks:azure_power_crystal",
                        "matterworks:verdant_power_crystal"
                ),
                true,
                true,
                true,
                true,
                true
        );
    }

    public MatterRuleConfig mergedWith(MatterRuleConfig defaults) {
        Set<ResourceLocation> mergedValueBlacklist = new LinkedHashSet<>(defaults.valueBlacklist());
        mergedValueBlacklist.addAll(valueBlacklist);

        Set<ResourceLocation> mergedRecyclerBlacklist = new LinkedHashSet<>(defaults.recyclerBlacklist());
        mergedRecyclerBlacklist.addAll(recyclerBlacklist);

        Set<ResourceLocation> mergedPatternBlacklist = new LinkedHashSet<>(defaults.patternBlacklist());
        mergedPatternBlacklist.addAll(patternBlacklist);

        return new MatterRuleConfig(
                mergedValueBlacklist,
                mergedRecyclerBlacklist,
                mergedPatternBlacklist,
                rejectContainerItems,
                rejectCustomName,
                rejectLore,
                rejectWritableBooks,
                rejectWrittenBooks
        );
    }

    private static Set<ResourceLocation> ids(String... ids) {
        Set<ResourceLocation> values = new LinkedHashSet<>();
        Arrays.stream(ids)
                .map(ResourceLocation::tryParse)
                .filter(java.util.Objects::nonNull)
                .forEach(values::add);
        return values;
    }
}
