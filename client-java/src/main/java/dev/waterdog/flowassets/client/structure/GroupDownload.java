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

package dev.waterdog.flowassets.client.structure;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
public class GroupDownload {
    private final Collection<Download> downloads;

    public CompletableFuture<Void> getFuture() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        this.downloads.forEach(download -> futures.add(download.getFuture()));
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Data
    public static class Download {
        private final CompletableFuture<Void> future;
        private final AssetDownloadData assetData;
        private final long startTime;
    }
}