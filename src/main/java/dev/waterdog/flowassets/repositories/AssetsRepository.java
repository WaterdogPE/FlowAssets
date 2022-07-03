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

import dev.waterdog.flowassets.structure.FlowAsset;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AssetsRepository implements PanacheRepository<FlowAsset> {

    @Transactional
    public List<FlowAsset> findByName(String name) {
        return this.find("asset_name like ?1", "%" + name.trim() + "%").list();
    }

    @Transactional
    public FlowAsset getByName(String name) {
        return this.find("asset_name", name.trim()).firstResult();
    }

    @Transactional
    public FlowAsset findByUuid(String uuid) {
        return this.find("uuid", UUID.fromString(uuid)).firstResult();
    }

    @Transactional
    public FlowAsset save(FlowAsset asset) {
        if (!this.isPersistent(asset)) {
            asset = this.getEntityManager().merge(asset);;
        }

        this.persist(asset);
        return asset;
    }

    @Transactional
    public void remove(FlowAsset asset) {
        if (this.isPersistent(asset)) {
            this.delete(asset);
        } else {
            this.delete(this.getEntityManager().merge(asset));
        }
    }
}
