package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.nsubtle.Nsubtle.MOD_ID;

public class EnUsLangProvider extends LanguageProvider {
    public EnUsLangProvider(PackOutput output) {
        super(output, MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(RegistryEvents.ItemRegistry.soup.get(), "Soup");
        add(RegistryEvents.ItemRegistry.gold_head.get(), "Gold Head");

        add(("itemGroup."+MOD_ID+".nsubtle"), "Nsubtle");
    }
}
