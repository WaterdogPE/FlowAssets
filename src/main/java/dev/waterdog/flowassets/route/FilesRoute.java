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

import dev.waterdog.flowassets.repositories.LocalFileRepository;
import dev.waterdog.flowassets.structure.FileSnapshot;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.jbosslog.JBossLog;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.util.concurrent.CompletableFuture;

@JBossLog
@ApplicationScoped
@RouteBase(path = "api")
public class FilesRoute {
    // TODO: token authentication

    @Inject
    LocalFileRepository localFileRepository;

    @Route(path = "/file/:uuid/:file_name", methods = Route.HttpMethod.GET)
    void serveLocalFile(RoutingContext ctx) {
        String uuid = ctx.pathParam("uuid");
        String fileName = ctx.pathParam("file_name");

        CompletableFuture<FileSnapshot> future = this.localFileRepository.loadSnapshot(uuid, fileName);
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
}
