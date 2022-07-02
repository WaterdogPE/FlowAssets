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

import dev.waterdog.flowassets.structure.S3ServerData;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class S3ServersRepository implements PanacheRepository<S3ServerData> {

    @Transactional
    public List<S3ServerData> getAll() {
        return this.listAll();
    }

    @Transactional
    public S3ServerData findByName(String name) {
        return find("name", name.trim()).firstResult();
    }

    @Transactional
    public void save(S3ServerData serverData) {
        if (this.isPersistent(serverData)) {
            this.persist(serverData);
        } else {
            this.persist(this.getEntityManager().merge(serverData));
        }
    }

    @Transactional
    public void remove(S3ServerData serverData) {
        if (this.isPersistent(serverData)) {
            this.delete(serverData);
        } else {
            this.delete(this.getEntityManager().merge(serverData));
        }
    }
}
