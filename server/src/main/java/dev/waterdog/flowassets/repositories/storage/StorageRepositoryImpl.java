/*
 * Copyright 2022 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.flowassets.repositories.storage;

import dev.waterdog.flowassets.structure.FileSnapshot;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.structure.RepositoryType;

import java.util.concurrent.CompletableFuture;

public interface StorageRepositoryImpl {

    CompletableFuture<Void> saveFileSnapshot(FileSnapshot snapshot);
    CompletableFuture<FileSnapshot> loadSnapshot(String uuid, String fileName);
    CompletableFuture<Void> deleteSnapshots(String uuid);
    RepositoryType getType();

    static String createDownloadUrl(FlowAsset asset, StorageRepositoryImpl storage) {
        return switch (storage.getType()) {
            case LOCAL -> "/api/file/" + asset.getAssetLocation();
            case REMOTE_S3 -> {
                String fileName = getAssetFileName(asset);
                yield ((S3StorageRepository) storage).createDownloadUrl(asset.getUuid().toString(), fileName).toString();
            }
        };

    }

    static String getAssetFileName(FlowAsset asset) {
        String[] namespace = asset.getAssetLocation().split("/");
        return namespace[namespace.length - 1];
    }
}
