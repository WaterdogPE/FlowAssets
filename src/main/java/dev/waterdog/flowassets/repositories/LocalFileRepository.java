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

package dev.waterdog.flowassets.repositories;

import dev.waterdog.flowassets.structure.FileSnapshot;
import dev.waterdog.flowassets.utils.Streams;
import io.netty.buffer.ByteBufInputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

@Singleton
public class LocalFileRepository implements FileRepositoryImpl {

    @Inject
    @ConfigProperty(name = "flowassets.local-dir")
    String localPath;

    @Override
    public CompletableFuture<Void> saveFileSnapshot(FileSnapshot snapshot) {
        return CompletableFuture.runAsync(() -> {
            Path path = Paths.get(this.localPath).resolve(snapshot.getUuid() + "/").resolve(snapshot.getFileName());
            try (ByteBufInputStream inputStream = new ByteBufInputStream(snapshot.getContent().getByteBuf()) ) {
                if (!Files.exists(path)) {
                    Files.createDirectories(path.getParent());
                }

                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write " + snapshot.getUuid() + "/" + snapshot.getFileName());
            }
        });
    }

    @Override
    public CompletableFuture<FileSnapshot> loadSnapshot(String uuid, String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            Path path = Paths.get(this.localPath).resolve(uuid + "/").resolve(fileName);
            if (!Files.exists(path)) {
                return null;
            }

            try (InputStream stream = new BufferedInputStream(Files.newInputStream(path))) {
                return new FileSnapshot(uuid, fileName, Streams.readToBuffer(stream));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read " + uuid + "/" + fileName);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteSnapshots(String uuid) {
        return CompletableFuture.runAsync(() -> {
            Path path = Paths.get(this.localPath).resolve(uuid + "/");
            try {
                if (Files.exists(path)) {
                    Files.walk(path)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to delete " + uuid, e);
            }
        });
    }
}
