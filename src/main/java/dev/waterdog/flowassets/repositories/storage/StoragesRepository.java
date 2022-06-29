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

import dev.waterdog.flowassets.repositories.S3ServersRepository;
import dev.waterdog.flowassets.structure.RepositoryType;
import dev.waterdog.flowassets.structure.S3ServerData;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class StoragesRepository {

    @Inject
    LocalStorageRepository localStorage;

    @Inject
    S3ServersRepository s3ConfigRepository;

    private final Map<String, S3StorageRepository> s3Servers = new ConcurrentHashMap<>();

    public StorageRepositoryImpl getStorageRepository(String storageName) {
        if (RepositoryType.getTypeFromName(storageName) == RepositoryType.LOCAL) {
            return this.localStorage;
        }
        return this.s3Servers.computeIfAbsent(storageName, this::createS3Storage);
    }

    private S3StorageRepository createS3Storage(String name) {
        S3ServerData serverData = this.s3ConfigRepository.findByName(name);
        if (serverData == null) {
            return null;
        }
        return new S3StorageRepository(serverData);
    }

    public S3ServersRepository getS3ConfigRepository() {
        return this.s3ConfigRepository;
    }

    public LocalStorageRepository getLocalStorage() {
        return this.localStorage;
    }
}
