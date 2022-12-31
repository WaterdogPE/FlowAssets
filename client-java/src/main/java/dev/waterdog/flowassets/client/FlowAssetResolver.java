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

package dev.waterdog.flowassets.client;

import com.google.gson.Gson;
import dev.waterdog.flowassets.client.structure.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class FlowAssetResolver {
    private static final Gson GSON = new Gson();

    private final String serverAddress;
    private final String serverToken;
    private final CloseableHttpClient client;

    public FlowAssetResolver(String serverAddress, String serverToken) {
        this.serverAddress = serverAddress;
        this.serverToken = serverToken;
        this.client = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(10, TimeUnit.SECONDS)
                        .setResponseTimeout(30, TimeUnit.SECONDS)
                        .build())
                .build();
    }

    public void close() {
        try {
            this.client.close();
        } catch (IOException e) {
        }
    }

    private void resolveAsset(AssetDownloadData assetData, Path downloadPath) throws IOException {
        if (assetData == null || !assetData.isValid() || !assetData.isFound()) {
            throw new IllegalArgumentException("Invalid asset data " + assetData);
        }

        String deployPath = "";
        if (assetData.getDeployPath() != null && !assetData.getDeployPath().trim().isEmpty()) {
            deployPath = assetData.getDeployPath();
        }

        Path path;
        if (downloadPath == null) {
            path = Paths.get(deployPath).resolve(assetData.getFileName());
        } else if (Files.isDirectory(downloadPath)) {
            path = downloadPath.resolve(assetData.getFileName());
        } else {
            path = downloadPath;
        }

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        this.client.execute(new HttpGet(assetData.getDownloadLink()), response -> {
            if (response.getCode() >= 400 || response.getEntity() == null) {
                throw new IllegalStateException("Request failed responseCode=" + response.getCode());
            }

            HttpEntity entity = response.getEntity();
            try (OutputStream stream = Files.newOutputStream(path, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                entity.writeTo(stream);
            }
            return null;
        });
    }

    public CompletableFuture<Void> downloadAsset(String assetName, Path downloadPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpGet get = new HttpGet(this.serverAddress + "/api/asset/name/" + assetName);
                get.addHeader("flow-auth-token", this.serverToken);
                return this.client.execute(get, response -> {
                    if (response.getCode() >= 400 || response.getEntity() == null) {
                        throw new IllegalStateException("Request failed responseCode=" + response.getCode());
                    }
                    try (InputStream content = response.getEntity().getContent()) {
                        return GSON.fromJson(new InputStreamReader(content), AssetDownloadData.class);
                    }
                });
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve asset " + assetName, e);
            }
        }).thenAccept(assetData -> {
            try {
                this.resolveAsset(assetData, downloadPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to download asset " + assetData, e);
            }
        });
    }

    public CompletableFuture<GroupDownload> downloadGroup(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpGet get = new HttpGet(this.serverAddress + "/api/group/" + groupName);
                get.addHeader("flow-auth-token", this.serverToken);
                return this.client.execute(get, response -> {
                    if (response.getCode() >= 400 || response.getEntity() == null) {
                        throw new IllegalStateException("Request failed responseCode=" + response.getCode());
                    }
                    try (InputStream content = response.getEntity().getContent()) {
                        return GSON.fromJson(new InputStreamReader(content), GroupDownloadData.class);
                    }
                });
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve group " + groupName, e);
            }
        }).thenApply(groupData -> {
            if (!groupData.isFound() || groupData.getAssets() == null) {
                throw new IllegalArgumentException("Unknown group " + groupName);
            }

            Collection<GroupDownload.Download> downloads = new ArrayList<>();
            for (AssetDownloadData asset : groupData.getAssets()) {
                long startTime = System.currentTimeMillis();
                CompletableFuture<Void> future = CompletableFuture.runAsync(unsafe(() -> this.resolveAsset(asset, null)));
                downloads.add(new GroupDownload.Download(future, asset, startTime));
            }
            return new GroupDownload(downloads);
        });
    }


    public CompletableFuture<UploadResponseData> uploadAsset(String assetName, String repositoryName, Path filePath, String uploadFileName) {
        return CompletableFuture.supplyAsync(unsafe(() -> {
            if (!Files.isRegularFile(filePath)) {
                throw new IllegalArgumentException("File " + filePath + " was not found");
            }

            try (InputStream stream = Files.newInputStream(filePath)) {
                HttpEntity httpEntity = MultipartEntityBuilder.create()
                        .setContentType(ContentType.MULTIPART_FORM_DATA)
                        .addTextBody("asset_name", assetName)
                        .addTextBody("repository", repositoryName)
                        .addBinaryBody("attachment", stream, ContentType.APPLICATION_OCTET_STREAM, uploadFileName)
                        .build();

                HttpPost post = new HttpPost(this.serverAddress + "/api/asset/upload");
                post.addHeader("flow-auth-token", this.serverToken);
                post.setEntity(httpEntity);

                return this.client.execute(post, response -> {
                    if (response.getCode() >= 400 || response.getEntity() == null) {
                        throw new IllegalStateException("Request failed responseCode=" + response.getCode());
                    }

                    try (InputStream content = response.getEntity().getContent()) {
                        return GSON.fromJson(new InputStreamReader(content), UploadResponseData.class);
                    }
                });
            }
        }));
    }

    public CompletableFuture<UpdateResponseData> updateAsset(String assetName, Path filePath) {
        return CompletableFuture.supplyAsync(unsafe(() -> {
            if (!Files.isRegularFile(filePath)) {
                throw new IllegalArgumentException("File " + filePath + " was not found");
            }

            try (InputStream stream = Files.newInputStream(filePath)) {
                HttpEntity httpEntity = MultipartEntityBuilder.create()
                        .setContentType(ContentType.MULTIPART_FORM_DATA)
                        .addTextBody("asset_name", assetName)
                        .addBinaryBody("attachment", stream, ContentType.APPLICATION_OCTET_STREAM, filePath.getFileName().toString())
                        .build();

                HttpPost post = new HttpPost(this.serverAddress + "/api/asset/update");
                post.addHeader("flow-auth-token", this.serverToken);
                post.setEntity(httpEntity);

                return this.client.execute(post, response -> {
                    if (response.getCode() >= 400 || response.getEntity() == null) {
                        throw new IllegalStateException("Request failed responseCode=" + response.getCode());
                    }

                    try (InputStream content = response.getEntity().getContent()) {
                        return GSON.fromJson(new InputStreamReader(content), UpdateResponseData.class);
                    }
                });
            }
        }));
    }

    private static <T> Supplier<T> unsafe(UnsafeSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Runnable unsafe(UnsafeRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private interface UnsafeSupplier<T> {
        T get() throws Exception;
    }

    private interface UnsafeRunnable {
        void run() throws Exception;
    }
}
