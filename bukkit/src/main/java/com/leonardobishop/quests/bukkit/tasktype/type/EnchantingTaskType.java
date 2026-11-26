package com.leonardobishop.quests.bukkit.tasktype.type;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.item.QuestItem;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.bukkit.util.constraint.TaskConstraintSet;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.enchantment.EnchantItemEvent;

import java.util.List;

public final class EnchantingTaskType extends BukkitTaskType<EnchantingTask> {

    private final BukkitQuestsPlugin plugin;

    public EnchantingTaskType(BukkitQuestsPlugin plugin) {
        super("enchanting", TaskUtils.TASK_ATTRIBUTION_STRING, "Enchant a certain amount of items.", EnchantingTask.SCHEMA);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        if (e.getEnchanter().hasMetadata("NPC")) return;

        Player player = e.getEnchanter();

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (qPlayer == null) {
            return;
        }

        for (TaskUtils.PendingTask<EnchantingTask> pendingTask : TaskUtils.getApplicableTasks(player, qPlayer, this, TaskConstraintSet.ALL)) {
            Quest quest = pendingTask.quest();
            EnchantingTask task = pendingTask.task();
            TaskProgress taskProgress = pendingTask.taskProgress();

            super.debug("Player enchanted item", quest.getId(), task.getId(), player.getUniqueId());

            QuestItem qi = task.item;
            if (qi != null) {
                if (!qi.getItemStack().getType().equals(e.getItem().getType())) {
                    super.debug("Item does not match, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            }

            boolean hasEnchantment = true;

            List<Enchantment> enchantments = task.enchantments;
            if (enchantments != null) {
                hasEnchantment = false;

                for (Enchantment enchantmentObject : enchantments) {
                    String enchantment = enchantmentObject.getName();
                    if (e.getEnchantsToAdd().containsKey(enchantmentObject)) {
                        super.debug("Enchantments to add contains enchantment '" + enchantment + "'", quest.getId(), task.getId(), player.getUniqueId());
                        Integer minLevel = task.minLevel;
                        if (minLevel != null) {
                            int level = minLevel;
                            super.debug("Minimum level of " + level + " is specified", quest.getId(), task.getId(), player.getUniqueId());
                            if (e.getEnchantsToAdd().get(enchantmentObject) >= level) {
                                hasEnchantment = true;
                                super.debug("Item has minimum required level", quest.getId(), task.getId(), player.getUniqueId());
                                break;
                            } else {
                                super.debug("Item does not have minimum level (level = " + e.getEnchantsToAdd().get(enchantmentObject) + ")", quest.getId(), task.getId(), player.getUniqueId());
                            }
                        } else {
                            hasEnchantment = true;
                            break;
                        }
                    } else {
                        super.debug("Enchantments to add does not contains enchantment '" + enchantment + "'", quest.getId(), task.getId(), player.getUniqueId());
                    }
                }
            }

            if (!hasEnchantment) {
                super.debug("Applied enchantments does not contain any in required enchantments, skipping...", quest.getId(), task.getId(), player.getUniqueId());
                continue;
            }

            int enchantsNeeded = task.amount;

            int progress = TaskUtils.incrementIntegerTaskProgress(taskProgress);
            super.debug("Incrementing task progress (now " + progress + ")", quest.getId(), task.getId(), player.getUniqueId());

            if (progress >= enchantsNeeded) {
                super.debug("Marking task as complete", quest.getId(), task.getId(), player.getUniqueId());
                taskProgress.setCompleted(true);
            }

            TaskUtils.sendTrackAdvancement(player, quest, task, pendingTask, enchantsNeeded);
        }
    }
}
