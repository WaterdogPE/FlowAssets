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

package dev.waterdog.flowassets.route;

import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.repositories.storage.S3StorageRepository;
import dev.waterdog.flowassets.repositories.storage.StorageRepositoryImpl;
import dev.waterdog.flowassets.repositories.storage.StoragesRepository;
import dev.waterdog.flowassets.structure.FileSnapshot;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.structure.RepositoryType;
import dev.waterdog.flowassets.structure.rest.AssetInfoData;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.reactive.RestPath;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.CompletableFuture;

@JBossLog
@Path("api/")
@RouteBase(path = "api")
public class FilesRoute {
    // TODO: token authentication

    @Inject
    StoragesRepository storages;

    @Inject
    AssetsRepository assetsRepository;

    @Route(path = "/file/:uuid/:file_name", methods = Route.HttpMethod.GET)
    void serveLocalFile(RoutingContext ctx) {
        String uuid = ctx.pathParam("uuid");
        String fileName = ctx.pathParam("file_name");

        CompletableFuture<FileSnapshot> future = this.storages.getLocalStorage().loadSnapshot(uuid, fileName);
        future.whenComplete(((snapshot, throwable) -> this.serveSnapshot(ctx, snapshot, throwable)));
    }

    private void serveSnapshot(RoutingContext ctx, FileSnapshot snapshot, Throwable error) {
        if (error != null) {
            ctx.response()
                    .setStatusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .end();
            log.error("Failed to server file", error);
            return;
        }

        if (snapshot == null) {
            ctx.response()
                    .setStatusCode(Status.NOT_FOUND.getStatusCode())
                    .end();
            return;
        }

        ctx.response()
                .putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + snapshot.getFileName())
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                .end(snapshot.getContent());
    }

    @GET
    @Path("asset/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<AssetInfoData> assetInfo(@RestPath String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            FlowAsset asset = this.assetsRepository.findByUuid(uuid);
            if (asset == null) {
                return AssetInfoData.notFound(uuid);
            }

            AssetInfoData response = AssetInfoData.fromAsset(asset);
            StorageRepositoryImpl storage = this.storages.getStorageRepository(asset.getAssetRepository());
            if (storage == null) {
                response.setValid(false);
            } else if (storage.getType() == RepositoryType.LOCAL) {
                response.setDownloadLink("/" + asset.getAssetLocation());
            } else {
                String[] namespace = asset.getAssetLocation().split("/");
                String fileName = namespace[namespace.length - 1];
                response.setDownloadLink(((S3StorageRepository) storage).createDownloadUrl(asset.getUuid().toString(), fileName).toString());
            }
            return response;
        });

    }
}
