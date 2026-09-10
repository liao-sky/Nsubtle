package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import com.nsubtle.list.EnchantmentList;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.nsubtle.Nsubtle.MOD_ID;
import static com.nsubtle.datagen.DataToolKit.*;

public class ZnChLangProvider extends LanguageProvider {
    public ZnChLangProvider(PackOutput output) {
        super(output,MOD_ID , "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add(RegistryEvents.ItemRegistry.soup.get(), "羹");
        add(RegistryEvents.ItemRegistry.gold_head.get(), "金头");
        add(RegistryEvents.ItemRegistry.explosive_powder.get(),"爆裂粉末");
        add(RegistryEvents.EffectRegistry.BROKEN_DEFENSE.value(),"破防");
        add(RegistryEvents.BlockRegistry.altar_frame.get(),"祭坛框架");

        add("item.minecraft.potion.effect.broken_defense","破防药水");
        add("item.minecraft.splash_potion.effect.broken_defense","喷溅的破防药水");
        add("item.minecraft.lingering_potion.effect.broken_defense","滞留的破防药水");
        add("item.minecraft.tipped_arrow.effect.broken_defense", "破防之箭");

        addEnchantment(this, EnchantmentList.TOUGH,"坚韧");
        addEnchantmentDescription(this,EnchantmentList.TOUGH,"你的盔甲将更加坚硬，不再容易破防");


        add(("itemGroup."+MOD_ID+".nsubtle"), "Nsubtle");
        add("effect.nsubtle.broken_defense.hurt","§4受到伤害x%s§4");
        add("effect.nsubtle.broken_defense.break_again","§d破防+1!§d §4受到伤害x%s(有概率)§4");
        add("effect.nsubtle.broken_defense.warning","§4!警告，破防层数过高!§4");
        add("effect.nsubtle.broken_defense.armor_break","§4!你的甲不堪重负!§4");
        add("effect.nsubtle.broken_defense.break","§d!?(被)破防?!§d");

    }
}
