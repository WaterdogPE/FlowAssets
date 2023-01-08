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
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class AssetsRepository extends AbstractRepository<FlowAsset> {

    @Override
    public String nameIdentifier() {
        return "asset_name";
    }

    @Transactional
    public FlowAsset findByUuid(String uuid) {
        return this.find("uuid", UUID.fromString(uuid)).firstResult();
    }

    @Transactional
    @Override
    public void remove(FlowAsset value) {
        if (!this.isPersistent(value)) {
            value = this.getEntityManager().merge(value);
        }

        if (!value.getGroups().isEmpty()) {
            value.setGroups(null);
            this.persist(value);
        }
        this.delete(value);
    }
}
