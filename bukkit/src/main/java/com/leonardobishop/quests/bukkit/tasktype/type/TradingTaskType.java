package com.leonardobishop.quests.bukkit.tasktype.type;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.item.QuestItem;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.bukkit.util.constraint.TaskConstraintSet;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.quest.Task;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TradingTaskType extends BukkitTaskType {

    private final BukkitQuestsPlugin plugin;
    private final Table<String, String, QuestItem> fixedQuestItemCache = HashBasedTable.create();
    private final Table<String, String, QuestItem> fixedQuestFirstIngredientCache = HashBasedTable.create();
    private final Table<String, String, QuestItem> fixedQuestSecondIngredientCache = HashBasedTable.create();

    public TradingTaskType(BukkitQuestsPlugin plugin) {
        super("trading", TaskUtils.TASK_ATTRIBUTION_STRING, "Trade with a Villager or Wandering Trader.");
        this.plugin = plugin;

        super.addConfigValidator(TaskUtils.useRequiredConfigValidator(this, "amount"));
        super.addConfigValidator(TaskUtils.useIntegerConfigValidator(this, "amount"));
        super.addConfigValidator(TaskUtils.useEntityListConfigValidator(this, "mob", "mobs"));
        super.addConfigValidator(TaskUtils.useItemStackConfigValidator(this, "item"));
        super.addConfigValidator(TaskUtils.useItemStackConfigValidator(this, "first-ingredient"));
        super.addConfigValidator(TaskUtils.useItemStackConfigValidator(this, "second-ingredient"));
        super.addConfigValidator(TaskUtils.useIntegerConfigValidator(this, "data"));
        super.addConfigValidator(TaskUtils.useBooleanConfigValidator(this, "exact-match"));
        super.addConfigValidator(TaskUtils.useBooleanConfigValidator(this, "first-ingredient-exact-match"));
        super.addConfigValidator(TaskUtils.useBooleanConfigValidator(this, "second-ingredient-exact-match"));
    }

    @Override
    public void onReady() {
        fixedQuestItemCache.clear();
        fixedQuestFirstIngredientCache.clear();
        fixedQuestSecondIngredientCache.clear();
    }

    @SuppressWarnings({"SizeReplaceableByIsEmpty"}) // for readability
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTrade(final @NotNull PlayerTradeEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC")) {
            return;
        }

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (qPlayer == null) {
            return;
        }

        AbstractVillager villager = event.getVillager();
        MerchantRecipe recipe = event.getTrade();
        ItemStack item = recipe.getResult();
        int itemAmount = item.getAmount();

        List<ItemStack> ingredients = recipe.getIngredients();
        ItemStack firstIngredient = ingredients.size() >= 1 ? ingredients.get(0) : null;
        ItemStack secondIngredient = ingredients.size() >= 2 ? ingredients.get(1) : null;

        for (TaskUtils.PendingTask pendingTask : TaskUtils.getApplicableTasks(player, qPlayer, this, TaskConstraintSet.ALL)) {
            Quest quest = pendingTask.quest();
            Task task = pendingTask.task();
            TaskProgress taskProgress = pendingTask.taskProgress();

            if (!TaskUtils.matchEntity(this, pendingTask, villager, player.getUniqueId())) {
                super.debug("Continuing...", quest.getId(), task.getId(), player.getUniqueId());
                continue;
            }

            // TODO: add villager-type and villager-profession options
            // not that simple especially after the change in 1.21

            if (task.hasConfigKey("item")) {
                QuestItem qi;
                if ((qi = fixedQuestItemCache.get(quest.getId(), task.getId())) == null) {
                    QuestItem fetchedItem = TaskUtils.getConfigQuestItem(task, "item", "data");
                    fixedQuestItemCache.put(quest.getId(), task.getId(), fetchedItem);
                    qi = fetchedItem;
                }

                super.debug("Player traded " + itemAmount + " item of type " + item.getType(), quest.getId(), task.getId(), player.getUniqueId());

                boolean exactMatch = TaskUtils.getConfigBoolean(task, "exact-match", true);
                if (!qi.compareItemStack(item, exactMatch)) {
                    super.debug("Item does not match required item, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            }

            if (task.hasConfigKey("first-ingredient")) {
                QuestItem qi;
                if ((qi = fixedQuestFirstIngredientCache.get(quest.getId(), task.getId())) == null) {
                    QuestItem fetchedItem = TaskUtils.getConfigQuestItem(task, "first-ingredient", "data");
                    fixedQuestFirstIngredientCache.put(quest.getId(), task.getId(), fetchedItem);
                    qi = fetchedItem;
                }

                super.debug("First ingredient was of type " + (firstIngredient != null ? firstIngredient.getType() : null), quest.getId(), task.getId(), player.getUniqueId());

                boolean exactMatch = TaskUtils.getConfigBoolean(task, "first-ingredient-exact-match", true);
                if (firstIngredient == null || !qi.compareItemStack(firstIngredient, exactMatch)) {
                    super.debug("First ingredient does not match required item, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            }

            if (task.hasConfigKey("second-ingredient")) {
                QuestItem qi;
                if ((qi = fixedQuestSecondIngredientCache.get(quest.getId(), task.getId())) == null) {
                    QuestItem fetchedItem = TaskUtils.getConfigQuestItem(task, "second-ingredient", "data");
                    fixedQuestSecondIngredientCache.put(quest.getId(), task.getId(), fetchedItem);
                    qi = fetchedItem;
                }

                super.debug("Second ingredient was of type " + (secondIngredient != null ? secondIngredient.getType() : null), quest.getId(), task.getId(), player.getUniqueId());

                boolean exactMatch = TaskUtils.getConfigBoolean(task, "second-ingredient-exact-match", true);
                if (secondIngredient == null || !qi.compareItemStack(secondIngredient, exactMatch)) {
                    super.debug("Second ingredient does not match required item, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            }

            int progress = TaskUtils.incrementIntegerTaskProgress(taskProgress, itemAmount);
            super.debug("Incrementing task progress (now " + progress + ")", quest.getId(), task.getId(), player.getUniqueId());

            int amount = (int) task.getConfigValue("amount");

            if (progress >= amount) {
                super.debug("Marking task as complete", quest.getId(), task.getId(), player.getUniqueId());
                taskProgress.setCompleted(true);
            }

            TaskUtils.sendTrackAdvancement(player, quest, task, pendingTask, amount);
        }
    }
}
