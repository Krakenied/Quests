package com.leonardobishop.quests.bukkit.tasktype.type;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.item.QuestItem;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.bukkit.util.constraint.TaskConstraintSet;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TradingTaskType extends BukkitTaskType<TradingTask> {

    private final BukkitQuestsPlugin plugin;

    public TradingTaskType(BukkitQuestsPlugin plugin) {
        super("trading", TaskUtils.TASK_ATTRIBUTION_STRING, "Trade with a Villager or Wandering Trader.", TradingTask.SCHEMA);
        this.plugin = plugin;
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
        ItemStack result = recipe.getResult();
        int resultAmount = result.getAmount();

        List<ItemStack> ingredients = recipe.getIngredients();
        ItemStack firstIngredient, secondIngredient;
        int firstIngredientAmount, secondIngredientAmount;

        if (ingredients.size() >= 1) {
            if (ingredients.size() >= 2) {
                secondIngredient = ingredients.get(1);
                secondIngredientAmount = secondIngredient.getAmount();
            } else {
                secondIngredient = null;
                secondIngredientAmount = 0;
            }

            firstIngredient = ingredients.get(0);
            firstIngredientAmount = firstIngredient.getAmount();
        } else {
            firstIngredient = null;
            firstIngredientAmount = 0;

            secondIngredient = null;
            secondIngredientAmount = 0;
        }

        for (TaskUtils.PendingTask<TradingTask> pendingTask : TaskUtils.getApplicableTasks(player, qPlayer, this, TaskConstraintSet.ALL)) {
            Quest quest = pendingTask.quest();
            TradingTask task = pendingTask.task();
            TaskProgress taskProgress = pendingTask.taskProgress();

            if (!TaskUtils.matchEntity(this, pendingTask, task.mobs, villager, player.getUniqueId())) {
                super.debug("Continuing...", quest.getId(), task.getId(), player.getUniqueId());
                continue;
            }

            // TODO: add villager-type and villager-profession options
            // not that simple especially after the change in 1.21

            if (task.item != null) {
                QuestItem qi = task.item;
                super.debug("Player traded " + resultAmount + " items of type " + result.getType(), quest.getId(), task.getId(), player.getUniqueId());

                boolean exactMatch = task.exactMatch;
                if (!qi.compareItemStack(result, exactMatch)) {
                    super.debug("Item does not match required item, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            }

            if (task.firstIngredient != null) {
                QuestItem qi = task.firstIngredient;
                super.debug("First ingredient was " + (firstIngredient != null ? (firstIngredient.getAmount() + " of type " + firstIngredient.getType()) : null), quest.getId(), task.getId(), player.getUniqueId());

                boolean exactMatch = task.firstIngredientExactMatch;
                if (firstIngredient == null || !qi.compareItemStack(firstIngredient, exactMatch)) {
                    super.debug("First ingredient does not match required item, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            }

            if (task.secondIngredient != null) {
                QuestItem qi = task.secondIngredient;
                super.debug("Second ingredient was " + (secondIngredient != null ? (secondIngredient.getAmount() + " of type " + secondIngredient.getType()) : null), quest.getId(), task.getId(), player.getUniqueId());

                boolean exactMatch = task.secondIngredientExactMatch;
                if (secondIngredient == null || !qi.compareItemStack(secondIngredient, exactMatch)) {
                    super.debug("Second ingredient does not match required item, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            }

            Mode mode = task.mode;

            int itemAmount = switch (mode) {
                case RESULT -> resultAmount;
                case FIRST_INGREDIENT -> firstIngredientAmount;
                case SECOND_INGREDIENT -> secondIngredientAmount;
                case COUNT -> 1;
            };

            int progress = TaskUtils.incrementIntegerTaskProgress(taskProgress, itemAmount);
            super.debug("Incrementing task progress (now " + progress + ")", quest.getId(), task.getId(), player.getUniqueId());

            int amount = task.amount;

            if (progress >= amount) {
                super.debug("Marking task as complete", quest.getId(), task.getId(), player.getUniqueId());
                taskProgress.setCompleted(true);
            }

            TaskUtils.sendTrackAdvancement(player, quest, task, pendingTask, amount);
        }
    }

    enum Mode {
        RESULT,
        FIRST_INGREDIENT,
        SECOND_INGREDIENT,
        COUNT;

        static final Map<String, TradingTaskType.Mode> STRING_MODE_MAP = new HashMap<>() {{
            for (final TradingTaskType.Mode mode : TradingTaskType.Mode.values()) {
                this.put(mode.name().toLowerCase(Locale.ROOT), mode);
            }
        }};
    }
}
