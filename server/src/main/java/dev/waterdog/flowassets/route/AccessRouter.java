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

import dev.waterdog.flowassets.repositories.SecretTokensRepository;
import dev.waterdog.flowassets.structure.SecretToken;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class AccessRouter {
    public static final String HEADER_AUTH_TOKEN = "flow-auth-token";

    @Inject
    SecretTokensRepository tokensRepository;

    public boolean authorize(RoutingContext ctx) {
        boolean authed = this.isAuthenticated(ctx);
        if (authed) {
            ctx.next();
        } else {
            ctx.response().setStatusCode(Response.Status.UNAUTHORIZED.getStatusCode()).end("Unauthorized");
        }
        return authed;
    }

    public boolean isAuthenticated(RoutingContext ctx) {
        String authToken = ctx.request().getHeader(HEADER_AUTH_TOKEN);
        if (authToken == null || authToken.trim().isEmpty()) {
            return false;
        }

        SecretToken token = this.tokensRepository.getCachedToken(authToken);
        return token != null;
    }
}
