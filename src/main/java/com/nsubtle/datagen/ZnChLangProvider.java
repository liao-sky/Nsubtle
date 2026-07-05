package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.nsubtle.Nsubtle.MOD_ID;

public class ZnChLangProvider extends LanguageProvider {
    public ZnChLangProvider(PackOutput output) {
        super(output,MOD_ID , "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add(RegistryEvents.ItemRegistry.soup.get(), "羹");
        add(RegistryEvents.ItemRegistry.gold_head.get(), "金头");
        add(RegistryEvents.EffectRegistry.BROKEN_DEFENSE.value(),"破防");

        add(("itemGroup."+MOD_ID+".nsubtle"), "Nsubtle");
        add("effect.nsubtle.broken_defense.hurt","§4受到伤害x%s§4");
        add("effect.nsubtle.broken_defense.break_again","§d破防+1!§d §4受到伤害x%s(有概率)§4");
        add("effect.nsubtle.broken_defense.warning","§4!警告，破防层数过高!§4");
        add("effect.nsubtle.broken_defense.armor_break","§4!你的甲不堪重负!§4");
        add("effect.nsubtle.broken_defense.break","§d!?(被)破防?!§d");

    }
}
