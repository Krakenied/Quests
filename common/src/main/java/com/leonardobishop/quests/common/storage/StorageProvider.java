package com.leonardobishop.quests.common.storage;

import com.leonardobishop.quests.common.player.questprogressfile.QuestProgressFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The storage provider is responsible for obtaining a QuestProgressFile for a specified UUID and for
 * writing a QuestProgressFile.
 */
public interface StorageProvider {

    String getName();

    void init();

    void shutdown();

    /**
     * Load a QuestProgressFile from the data source by a specific UUID
     *
     * @param uuid the UUID to load
     * @return {@link QuestProgressFile} or null
     */
    @Nullable Map.Entry<QuestProgressFile, String> loadProgressFile(@NotNull UUID uuid);

    /**
     * Save a QuestProgressFile to the data source with a specific UUID
     *
     * @param uuid the uuid to match the file to
     * @param questProgressFile the file to save
     */
    boolean saveProgressFile(@NotNull UUID uuid, @NotNull QuestProgressFile questProgressFile, @Nullable String trackedQuestId);

    /**
     * Load all QuestProgressFiles
     *
     * @return {@link List<QuestProgressFile>}
     */
    @NotNull List<Map.Entry<QuestProgressFile, String>> loadAllProgressFiles();

    /**
     * Save a list of QuestProgressFiles
     *
     * @param files the list of QuestProgressFile to save
     **/
    void saveAllProgressFiles(List<Map.Entry<QuestProgressFile, String>> files);

    /**
     * Whether this provider is 'similar' to another one.
     * Similarity is determined if the provider effectively points to the same data source.
     *
     * @param provider the provider to compare to
     * @return true if similar, false otherwise
     */
    boolean isSimilar(StorageProvider provider);

}
