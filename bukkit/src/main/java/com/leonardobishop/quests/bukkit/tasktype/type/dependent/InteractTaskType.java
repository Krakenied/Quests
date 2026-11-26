package com.leonardobishop.quests.bukkit.tasktype.type.dependent;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.bukkit.util.chat.Chat;
import com.leonardobishop.quests.bukkit.util.constraint.TaskConstraintSet;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.tasktype.key.TaskSchema;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class InteractTaskType<T> extends BukkitTaskType<NpcInteractTask<T>> {

    public InteractTaskType(String type, String author, String description, TaskSchema<NpcInteractTask<T>> schema) {
        super(type, author, description, schema);
    }

    public void handle(Player player, T npcId, String npcName, BukkitQuestsPlugin plugin) {
        if (!player.isOnline()) {
            return;
        }

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (qPlayer == null) {
            return;
        }

        boolean nameCorrected = false;

        for (TaskUtils.PendingTask<NpcInteractTask<T>> pendingTask : TaskUtils.getApplicableTasks(player, qPlayer, this, TaskConstraintSet.ALL)) {
            Quest quest = pendingTask.quest();
            NpcInteractTask<T> task = pendingTask.task();
            TaskProgress taskProgress = pendingTask.taskProgress();

            super.debug("Player clicked NPC", quest.getId(), task.getId(), player.getUniqueId());

            List<T> configNPCId = task.npcIds;
            if (configNPCId != null) {
                if (!configNPCId.contains(npcId)) {
                    super.debug("NPC id " + npcId + " does not match required id, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                    continue;
                }
            } else {
                String configNPCName = task.npcName;
                if (configNPCName != null) {
                    if (npcName == null) {
                        super.debug("NPC name is empty and does not match required name, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                        continue;
                    }

                    if (!nameCorrected) {
                        npcName = Chat.legacyStrip(Chat.legacyColor(npcName));
                        nameCorrected = true;
                    }

                    if (!configNPCName.equals(npcName)) {
                        super.debug("NPC name " + npcName + " does not match required name, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                        continue;
                    }
                }
            }

            super.debug("Marking task as complete", quest.getId(), task.getId(), player.getUniqueId());
            taskProgress.setCompleted(true);
        }
    }
}
