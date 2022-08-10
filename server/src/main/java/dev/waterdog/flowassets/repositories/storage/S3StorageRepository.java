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
import dev.waterdog.flowassets.structure.RepositoryType;
import dev.waterdog.flowassets.structure.S3ServerData;
import io.vertx.core.buffer.Buffer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class S3StorageRepository implements StorageRepositoryImpl {

    private final S3ServerData serverData;
    private final S3AsyncClient client;
    private final S3Presigner presigner;

    public S3StorageRepository(S3ServerData serverData) {
        this.serverData = serverData;

        Region region = Region.US_EAST_1;
        if (serverData.getRegionName() != null) {
            for (Region rregion : Region.regions()) {
                if (rregion.id().equals(serverData.getRegionName())) {
                    region = rregion;
                    break;
                }
            }
        }

        S3Configuration configuration = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .pathStyleAccessEnabled(true)
                .build();

        this.client = S3AsyncClient.builder()
                .region(region)
                .credentialsProvider(() -> AwsBasicCredentials.create(serverData.getAccessKey(), serverData.getSecretkey()))
                .endpointOverride(URI.create(serverData.getBucketUrl()))
                .serviceConfiguration(configuration)
                .build();

        this.presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(() -> AwsBasicCredentials.create(serverData.getAccessKey(), serverData.getSecretkey()))
                .endpointOverride(URI.create(serverData.getBucketUrl()))
                .serviceConfiguration(configuration)
                .build();
    }

    @Override
    public CompletableFuture<Void> saveFileSnapshot(FileSnapshot snapshot) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(this.serverData.getBucketName())
                .contentLength((long) snapshot.getContent().length())
                .key(snapshot.getUuid() + "/" + snapshot.getFileName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .build();

        ByteBuffer buffer = snapshot.getContent().getByteBuf().nioBuffer();

        return this.client.putObject(request, AsyncRequestBody.fromByteBuffer(buffer))
                .thenAccept(action -> {});
    }

    @Override
    public CompletableFuture<FileSnapshot> loadSnapshot(String uuid, String fileName) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(this.serverData.getBucketName())
                .key(uuid + "/" + fileName)
                .build();

        return this.client.getObject(request, AsyncResponseTransformer.toBytes())
                .thenApply(response -> new FileSnapshot(uuid, fileName, Buffer.buffer(response.asByteArray())));
    }

    @Override
    public CompletableFuture<Void> deleteSnapshots(String uuid) {
        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                .bucket(this.serverData.getBucketName())
                .prefix(uuid)
                .build();

        return this.client.listObjects(listRequest).thenApply(action -> {
            List<ObjectIdentifier> objects = new ArrayList<>();
            for (S3Object content : action.contents()) {
                objects.add(ObjectIdentifier.builder()
                        .key(content.key())
                        .build());
            }

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(this.serverData.getBucketName())
                    .delete(Delete.builder().objects(objects).build())
                    .build();

            return this.client.deleteObjects(request);
        }).thenAccept(resp -> {});
    }

    public URL createDownloadUrl(String uuid, String fileName) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(this.serverData.getBucketName())
                .key(uuid + "/" + fileName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(request)
                .build();
        return presigner.presignGetObject(presignRequest).url();
    }

    public String getServerName() {
        return this.serverData.getServerName();
    }

    @Override
    public RepositoryType getType() {
        return RepositoryType.REMOTE_S3;
    }
}
