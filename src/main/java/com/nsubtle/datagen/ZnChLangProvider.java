package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.nsubtle.Nsubtle.MOD_ID;

public class ZnChLangProvider extends LanguageProvider {
    public ZnChLangProvider(PackOutput output) {
        super(output,MOD_ID , "zh_CN");
    }

    @Override
    protected void addTranslations() {
        add(RegistryEvents.ItemRegistry.soup.get(), "¸þ");
        add(RegistryEvents.ItemRegistry.gold_head.get(), "½ðÍ·");

        add(("itemGroup."+MOD_ID+".nsubtle"), "Nsubtle");
    }
}
