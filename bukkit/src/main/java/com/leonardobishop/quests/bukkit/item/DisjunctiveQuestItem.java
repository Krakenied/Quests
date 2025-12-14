package com.leonardobishop.quests.bukkit.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class DisjunctiveQuestItem extends QuestItem {

    // There is no single item for that.
    private static final ItemStack ITEM = new ItemStack(Material.STONE, 1);

    private final List<QuestItem> items;

    public DisjunctiveQuestItem(final List<QuestItem> items) {
        super("disjunctive", null);
        this.items = items;
    }

    @Override
    public ItemStack getItemStack() {
        return ITEM;
    }

    @Override
    public boolean compareItemStack(final ItemStack other, final boolean exactMatch) {
        for (final QuestItem item : this.items) {
            if (item.compareItemStack(other, exactMatch)) {
                return true;
            }
        }
        return false;
    }
}
