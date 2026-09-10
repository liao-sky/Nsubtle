package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import com.nsubtle.list.EnchantmentList;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.nsubtle.Nsubtle.MOD_ID;
import static com.nsubtle.datagen.DataToolKit.addEnchantment;
import static com.nsubtle.datagen.DataToolKit.addEnchantmentDescription;

public class EnUsLangProvider extends LanguageProvider {
    public EnUsLangProvider(PackOutput output) {
        super(output, MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(RegistryEvents.ItemRegistry.soup.get(), "Soup");
        add(RegistryEvents.ItemRegistry.gold_head.get(), "Gold Head");
        add(RegistryEvents.ItemRegistry.explosive_powder.get(),"Explosive Powder");
        add(RegistryEvents.EffectRegistry.BROKEN_DEFENSE.value(),"Broken Defense");
        add(RegistryEvents.BlockRegistry.altar_frame.get(),"Altar Frame");

        add("item.minecraft.potion.effect.broken_defense","Potion of Broken Defense");
        add("item.minecraft.splash_potion.effect.broken_defense", "Splash Potion of Broken Defense");
        add("item.minecraft.lingering_potion.effect.broken_defense", "Lingering Potion of Broken Defense");
        add("item.minecraft.tipped_arrow.effect.broken_defense", "Arrow of Broken Defense");

        addEnchantment(this, EnchantmentList.TOUGH,"tough");
        addEnchantmentDescription(this,EnchantmentList.TOUGH,"Your armor will be tougher and won't break as easily.");

        add(("itemGroup."+MOD_ID+".nsubtle"), "Nsubtle");
        add("effect.nsubtle.broken_defense.hurt","§4 TAKE DAMAGE x%s§4");
        add("effect.nsubtle.broken_defense.break_again","§d BROKEN DEFENSE +1!§d §4 DAMAGE x%s (Probably)§4");
        add("effect.nsubtle.broken_defense.warning","§4! WARNING, BROKEN DEFENSE LAYERS IS TOO HIGH!§4");
        add("effect.nsubtle.broken_defense.armor_break","§4!YOUR ARMOR IS OVERWHELMED!§4");
        add("effect.nsubtle.broken_defense.break","§d!?(BE) BROKEN DEFENSE?!§d");
    }
}
