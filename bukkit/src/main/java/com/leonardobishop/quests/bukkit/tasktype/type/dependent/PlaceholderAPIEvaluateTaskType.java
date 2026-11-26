package com.leonardobishop.quests.bukkit.tasktype.type.dependent;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.scheduler.WrappedRunnable;
import com.leonardobishop.quests.bukkit.scheduler.WrappedTask;
import com.leonardobishop.quests.bukkit.tasktype.BukkitTaskType;
import com.leonardobishop.quests.bukkit.util.TaskUtils;
import com.leonardobishop.quests.bukkit.util.constraint.TaskConstraintSet;
import com.leonardobishop.quests.common.player.QPlayer;
import com.leonardobishop.quests.common.player.questprogressfile.TaskProgress;
import com.leonardobishop.quests.common.quest.Quest;
import com.leonardobishop.quests.common.quest.Task;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public final class PlaceholderAPIEvaluateTaskType extends BukkitTaskType<PlaceholderAPIEvaluateTask> {

    private final BukkitQuestsPlugin plugin;
    private final WeakHashMap<Player, WeakHashMap<Task, Integer>> refreshTicksMap = new WeakHashMap<>();
    private WrappedTask poll;

    public PlaceholderAPIEvaluateTaskType(BukkitQuestsPlugin plugin) {
        super("placeholderapi_evaluate", TaskUtils.TASK_ATTRIBUTION_STRING, "Evaluate the result of a placeholder", PlaceholderAPIEvaluateTask.SCHEMA);
        this.plugin = plugin;
    }

    @Override
    public void onReady() {
        int refreshTicks = plugin.getConfig().getInt("options.placeholderapi-global-refresh-ticks", 30);
        this.poll = new WrappedRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    handle(player);
                }
            }
        }.runTaskTimer(plugin.getScheduler(), refreshTicks, refreshTicks);
    }

    private void handle(Player player) {
        if (player.hasMetadata("NPC")) {
            return;
        }

        QPlayer qPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (qPlayer == null) {
            return;
        }

        for (TaskUtils.PendingTask<PlaceholderAPIEvaluateTask> pendingTask : TaskUtils.getApplicableTasks(player, qPlayer, this, TaskConstraintSet.ALL)) {
            Quest quest = pendingTask.quest();
            PlaceholderAPIEvaluateTask task = pendingTask.task();
            TaskProgress taskProgress = pendingTask.taskProgress();

            super.debug("Polling PAPI for player", quest.getId(), task.getId(), player.getUniqueId());

            Integer refreshTicks = task.refreshTicks;
            if (refreshTicks != null) {
                int currentTick = Bukkit.getCurrentTick();

                WeakHashMap<Task, Integer> playerRefreshTicksMap = refreshTicksMap.computeIfAbsent(player, k -> new WeakHashMap<>());
                Integer lastRefreshTicks = playerRefreshTicksMap.get(task);

                if (lastRefreshTicks != null) {
                    int ticksSinceLastRefresh = currentTick - lastRefreshTicks;

                    if (ticksSinceLastRefresh < refreshTicks) {
                        super.debug("Ticks since last refresh are lower than specified, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                        continue;
                    }
                }

                playerRefreshTicksMap.put(task, currentTick);
            }

            String placeholder = task.placeholder;
            String evaluatesString = task.evaluates;
            Operator operator = task.operator;

            super.debug("Operator = " + operator, quest.getId(), task.getId(), player.getUniqueId());

            boolean async = task.async;
            CompletableFuture<String> future = evaluate(player, placeholder, async);

            future.thenAccept(evaluatedString -> {
                super.debug("Evaluation = '" + evaluatedString + "'", quest.getId(), task.getId(), player.getUniqueId());

                if (operator != null) {
                    Double evaluatesNumber = task.evaluatesNumber;
                    if (evaluatesNumber == null) {
                        super.debug("Numeric operator was specified but configured string to evaluate to cannot be parsed into a double, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                        return;
                    }
                    double evaluates = evaluatesNumber;

                    double evaluated;
                    try {
                        evaluated = Double.parseDouble(evaluatedString);
                    } catch (NumberFormatException ignored) {
                        super.debug("Numeric operator was specified but evaluated string to cannot be parsed into a double, continuing...", quest.getId(), task.getId(), player.getUniqueId());
                        return;
                    }

                    taskProgress.setProgress(evaluated);

                    if (operator.compare(evaluated, evaluates)) {
                        super.debug("Marking task as complete", quest.getId(), task.getId(), player.getUniqueId());
                        taskProgress.setCompleted(true);
                    }

                    // Do not send track advancement for this task type as it behaves really weird
                    //TaskUtils.sendTrackAdvancement(player, quest, task, pendingTask, evaluates);
                } else if (evaluatedString.equals(evaluatesString)) {
                    super.debug("Marking task as complete", quest.getId(), task.getId(), player.getUniqueId());
                    taskProgress.setCompleted(true);
                }
            });
        }
    }

    private CompletableFuture<String> evaluate(Player player, String placeholder, boolean async) {
        if (!async) {
            String evaluated = PlaceholderAPI.setPlaceholders(player, placeholder);
            return CompletableFuture.completedFuture(evaluated);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        plugin.getScheduler().runTaskAsynchronously(() -> {
            String evaluated = PlaceholderAPI.setPlaceholders(player, placeholder);
            plugin.getScheduler().runTaskAtEntity(player, () -> future.complete(evaluated));
        });

        return future;
    }

    @Override
    public void onDisable() {
        if (this.poll != null) {
            this.poll.cancel();
        }
    }

    enum Operator {
        GREATER_THAN {
            @Override
            public boolean compare(double evaluated, double evaluates) {
                return evaluated > evaluates;
            }
        },
        LESS_THAN {
            @Override
            public boolean compare(double evaluated, double evaluates) {
                return evaluated < evaluates;
            }
        },
        GREATER_THAN_OR_EQUAL_TO {
            @Override
            public boolean compare(double evaluated, double evaluates) {
                return evaluated >= evaluates;
            }
        },
        LESS_THAN_OR_EQUAL_TO {
            @Override
            public boolean compare(double evaluated, double evaluates) {
                return evaluated <= evaluates;
            }
        };

        public abstract boolean compare(double evaluated, double evaluates);
    }

}
