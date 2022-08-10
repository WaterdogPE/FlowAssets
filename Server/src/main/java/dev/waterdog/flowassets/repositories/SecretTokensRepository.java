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

import dev.waterdog.flowassets.structure.SecretToken;
import dev.waterdog.flowassets.utils.CacheableMap;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.inject.Singleton;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class SecretTokensRepository implements PanacheRepository<SecretToken> {

    private final Map<String, SecretToken> tokens = CacheableMap.<String, SecretToken>builder()
            .timeout(10)
            .unit(TimeUnit.MINUTES)
            .build();

    @Transactional
    public List<SecretToken> loadAll() {
        return this.listAll();
    }

    @Transactional
    public SecretToken findByHash(String hash) {
        return this.find("token_hash", hash.trim()).firstResult();
    }

    @Transactional
    public void save(SecretToken token) {
        if (this.isPersistent(token)) {
            this.persist(token);
        } else {
            this.persist(this.getEntityManager().merge(token));
        }
        this.tokens.put(token.getTokenHash(), token);
    }

    @Transactional
    public void remove(SecretToken token) {
        if (this.isPersistent(token)) {
            this.delete(token);
        } else {
            this.delete(this.getEntityManager().merge(token));
        }
        this.tokens.remove(token.getTokenHash());
    }

    public SecretToken getCachedToken(String tokenString) {
        String tokenHash = SecretToken.createTokenHash(tokenString);
        if (tokenHash == null) {
            return null;
        }
        return this.tokens.computeIfAbsent(tokenHash, this::findByHash);
    }
}
