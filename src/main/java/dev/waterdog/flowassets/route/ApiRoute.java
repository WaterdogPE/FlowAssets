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
import dev.waterdog.flowassets.repositories.storage.StorageRepositoryImpl;
import dev.waterdog.flowassets.repositories.storage.StoragesRepository;
import dev.waterdog.flowassets.structure.FileSnapshot;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.structure.rest.UploadFormData;
import dev.waterdog.flowassets.structure.rest.AssetInfoData;
import dev.waterdog.flowassets.structure.rest.UploadResponseData;
import dev.waterdog.flowassets.utils.Helper;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.nio.file.Files;

@JBossLog
@Path("api/")
@RouteBase(path = "api")
public class ApiRoute {

    @Inject
    AccessRouter accessRouter;

    @Inject
    StoragesRepository storages;

    @Inject
    AssetsRepository assetsRepository;

    @Route(path = "*", order = 0, type = Route.HandlerType.BLOCKING)
    public void secureRoute(RoutingContext ctx) {
        this.accessRouter.authorize(ctx);
    }

    @Route(path = "/file/:uuid/:file_name", methods = Route.HttpMethod.GET)
    public void serveLocalFile(RoutingContext ctx) {
        String uuid = ctx.pathParam("uuid");
        String fileName = ctx.pathParam("file_name");

        Uni.createFrom().completionStage(() -> this.storages.getLocalStorage().loadSnapshot(uuid, fileName))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().with(snapshot -> {
                    if (snapshot == null) {
                        ctx.response()
                                .setStatusCode(Status.NOT_FOUND.getStatusCode())
                                .end();
                    } else {
                        ctx.response()
                                .putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + snapshot.getFileName())
                                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                                .end(snapshot.getContent());
                    }
                }, err -> {
                    ctx.response()
                            .setStatusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
                            .end();
                    log.error("Failed to server file", err);
                });
    }

    @GET
    @Path("asset/uuid/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<AssetInfoData> assetInfoUuid(@RestPath String uuid) {
        return Uni.createFrom().item(() -> this.assetsRepository.findByUuid(uuid))
                .flatMap(asset -> {
                    if (asset == null) {
                        return Uni.createFrom().item(AssetInfoData.notFound(uuid));
                    } else {
                        return this.serverAssetInfo(asset);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("asset/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<AssetInfoData> assetInfoName(@RestPath String name) {
        return Uni.createFrom().item(() -> this.assetsRepository.getByName(name))
                .flatMap(asset -> {
                    if (asset == null) {
                        return Uni.createFrom().item(AssetInfoData.notFoundName(name));
                    } else {
                        return this.serverAssetInfo(asset);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private Uni<AssetInfoData> serverAssetInfo(FlowAsset asset) {
        return Uni.createFrom().item(() -> this.storages.getStorageRepository(asset.getAssetRepository()))
                .map(storage -> {
                    AssetInfoData response = AssetInfoData.fromAsset(asset);
                    if (storage == null) {
                        response.setValid(false);
                    } else {
                        response.setDownloadLink(StorageRepositoryImpl.createDownloadUrl(asset, storage));
                    }
                    return response;
                });
    }

    @POST
    @Path("asset/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> assetCreate(@MultipartForm UploadFormData form) {
        return Uni.createFrom().item(() -> this.storages.getStorageRepository(form.getRepositoryName())).flatMap(storage -> {
            if (storage == null) {
                return Uni.createFrom().item(Response.ok(UploadResponseData.error("invalid storage")).build());
            }

            return Uni.createFrom().item(Unchecked.supplier(() -> FileSnapshot.createSkeleton(form.getAttachment().fileName(), Files.newInputStream(form.getAttachment().filePath()))))
                    .flatMap(snapshot -> {
                        FlowAsset skeleton = new FlowAsset();
                        skeleton.setAssetName(form.getAssetName());
                        skeleton.setAssetRepository(form.getRepositoryName());
                        return Uni.createFrom().completionStage(FlowAsset.uploadAsset(skeleton, snapshot, this.assetsRepository, storage))
                                .map(asset -> Response.ok(UploadResponseData.ok(asset.getAssetName(), asset.getUuid().toString())).build());
                    });
        }).onFailure().invoke(err -> Response.status(Status.INTERNAL_SERVER_ERROR).build())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("asset/delete")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> assetDelete(@RestQuery String uuid) {
         return Uni.createFrom().item(() -> this.assetsRepository.findByUuid(uuid))
                 .onItem().ifNotNull().transform(asset -> Tuple2.of(asset, this.storages.getStorageRepository(asset.getAssetRepository())))
                 .onItem().ifNotNull().transformToUni(tuple ->
                         Uni.createFrom().completionStage(FlowAsset.deleteAsset(tuple.getItem1(), this.assetsRepository, tuple.getItem2()))
                                 .map(v -> Response.ok(Helper.success(uuid, tuple.getItem1().getAssetName())).build()))
                 .onItem().ifNull().continueWith(() -> Response.ok(Helper.error("not found")).build())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

    }
}
