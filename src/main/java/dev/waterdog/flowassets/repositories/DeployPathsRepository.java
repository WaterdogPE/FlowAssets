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

import dev.waterdog.flowassets.structure.DeployPath;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.hibernate.Hibernate;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class DeployPathsRepository implements PanacheRepository<DeployPath> {

    @Transactional
    public List<DeployPath> getAll() {
        return this.listAll();
    }

    @Transactional
    public List<DeployPath> findByName(String name) {
        return this.find("name like ?1", "%" + name.trim() + "%").list();
    }

    @Transactional
    public DeployPath getByName(String name) {
        return find("name", name.trim()).firstResult();
    }

    @Transactional
    public void save(DeployPath deployPath) {
        if (this.isPersistent(deployPath)) {
            this.persist(deployPath);
        } else {
            this.persist(this.getEntityManager().merge(deployPath));
        }
    }

    @Transactional
    public void remove(DeployPath deployPath) {
        if (this.isPersistent(deployPath)) {
            this.delete(deployPath);
        } else {
            this.delete(this.getEntityManager().merge(deployPath));
        }
    }

    @Transactional
    public DeployPath getInitialized(DeployPath deployPath) {
        DeployPath merge = this.getEntityManager().merge(deployPath);
        Hibernate.initialize(merge.getAssets());
        return merge;
    }
}
