package com.nsubtle.datagen;

import com.nsubtle.events.RegistryEvents;
import com.nsubtle.list.EnchantmentList;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.nsubtle.Nsubtle.MOD_ID;
import static com.nsubtle.datagen.DataToolKit.addEnchantment;
import static com.nsubtle.datagen.DataToolKit.addEnchantmentDescription;

public class ZnChLangProvider extends LanguageProvider {

    public ZnChLangProvider(PackOutput output) {
        super(output, MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add(RegistryEvents.ItemRegistry.SOUP.get(), "羹");
        add(RegistryEvents.ItemRegistry.GOLD_HEAD.get(), "金头");
        add(RegistryEvents.ItemRegistry.EXPLOSIVE_POWDER.get(), "爆裂粉末");
        add(RegistryEvents.EffectRegistry.BROKEN_DEFENSE.value(), "破防");
        add(RegistryEvents.BlockRegistry.ALTAR_FRAME.get(), "祭坛框架");

        add("item.minecraft.potion.effect.broken_defense", "破防药水");
        add("item.minecraft.splash_potion.effect.broken_defense", "喷溅的破防药水");
        add("item.minecraft.lingering_potion.effect.broken_defense", "滞留的破防药水");
        add("item.minecraft.tipped_arrow.effect.broken_defense", "破防之箭");

        addEnchantment(this, EnchantmentList.TOUGH, "坚韧");
        addEnchantmentDescription(this, EnchantmentList.TOUGH, "你的盔甲将更加坚硬，更能抵抗破防");

        addEnchantment(this, EnchantmentList.VITALITY, "生机");
        addEnchantmentDescription(this, EnchantmentList.VITALITY,
                "树苗炸弹不再需要也不会消耗骨粉，弩可以直接装树苗");

        add("itemGroup." + MOD_ID + ".nsubtle", "Nsubtle");

        add("advancements." + MOD_ID + ".tree_boom.title", "催生？");
        add("advancements." + MOD_ID + ".tree_boom.description", "违反自然，那么代价是？");

        add("effect.nsubtle.broken_defense.hurt", "§4受到伤害x%s§4");
        add("effect.nsubtle.broken_defense.break_again", "§d破防+1!§d §4受到伤害x%s(有概率)§4");
        add("effect.nsubtle.broken_defense.warning", "§4!警告，破防层数过高!§4");
        add("effect.nsubtle.broken_defense.armor_break", "§4!你的甲不堪重负!§4");
        add("effect.nsubtle.broken_defense.break", "§d!?(被)破防?!§d");

        add("entity." + MOD_ID + ".sapling_projectile", "树苗炸弹");
        add("tooltip.nsubtle.sapling_loaded", "已装填：%s");
        add("tooltip.nsubtle.sapling_ready", "骨粉已就绪 —— 右键发射");
        add("tooltip.nsubtle.powder_loaded", "已装入骨粉");
        add("tooltip.nsubtle.powder_needs_sapling", "还需要装入一枚树苗");
        add("tooltip.nsubtle.bow_sapling", "副手树苗 + 骨粉：松开弓弦发射树苗炸弹");
    }
}
