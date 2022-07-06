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

package dev.waterdog.flowassets.structure;

import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.repositories.storage.StorageRepositoryImpl;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Entity
@ToString(callSuper = true)
@Getter @Setter
@Table(name = "assets")
public class FlowAsset extends PanacheEntityBase {

    @Id
    @Type(type = "uuid-char")
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "uuid", unique = true, columnDefinition = "varchar(36)")
    private UUID uuid;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "asset_location")
    private String assetLocation;

    @Column(name = "asset_repository")
    private String assetRepository;

    public static CompletableFuture<FlowAsset> uploadAsset(FlowAsset skeleton, FileSnapshot fileSnapshot,
                                                      AssetsRepository assetsRepository, StorageRepositoryImpl storageRepository) {
        return CompletableFuture.supplyAsync(() -> assetsRepository.save(skeleton)).thenApply(asset -> {
            fileSnapshot.setUuid(asset.getUuid().toString());
            return Pair.of(asset, fileSnapshot);
        }).thenCompose(pair -> storageRepository.saveFileSnapshot(pair.getRight()).thenApply(i -> pair))
                .thenApply(pair -> {
                    pair.getLeft().setAssetLocation(pair.getLeft().getUuid() + "/" + fileSnapshot.getFileName());
                    assetsRepository.save(pair.getLeft());
                    return pair.getLeft();
                });
    }

    public static CompletableFuture<Void> deleteAsset(FlowAsset asset, AssetsRepository assetsRepository, StorageRepositoryImpl storageRepository) {
        return CompletableFuture.runAsync(() -> storageRepository.deleteSnapshots(asset.getUuid().toString()))
                .thenRun(() -> assetsRepository.remove(asset));
    }
}
