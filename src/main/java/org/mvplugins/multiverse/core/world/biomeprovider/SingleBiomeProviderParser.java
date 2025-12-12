package org.mvplugins.multiverse.core.world.biomeprovider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.jetbrains.annotations.NotNull;

/**
 * A parser for {@link SingleBiomeProvider}
 */
final class SingleBiomeProviderParser implements BiomeProviderParser {

	private List<String> biomes;

	@Override
	public BiomeProvider parseBiomeProvider(@NotNull String worldName, @NotNull String params) {
		final NamespacedKey biomeKey = NamespacedKey.fromString(StringUtils.lowerCase(params, Locale.ENGLISH));
		return new SingleBiomeProvider(Registry.BIOME.get(biomeKey));
	}

	@Override
	public Collection<String> suggestParams(@NotNull String currentInput) {
		if (biomes == null) {
			final List<String> result = new ArrayList<>();
			for (Biome biome : Registry.BIOME) {
				result.add(StringUtils.lowerCase(biome.getKey().getKey(), Locale.ENGLISH));
			}
			biomes = result;
		}
		return biomes;
	}

}