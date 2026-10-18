package com.cyberfarm.registry;

import com.cyberfarm.CyberFarm;
import com.cyberfarm.HandbookGifting;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CyberFarm.MODID);

    // Lists every registered CyberFarm item, so new items show up without touching this class.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cyberfarm"))
            .icon(() -> ModItems.DRONE_CORE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                ItemStack handbook = HandbookGifting.createHandbook();
                if (!handbook.isEmpty()) {
                    output.accept(handbook);
                }
                ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
            })
            .build());
}
