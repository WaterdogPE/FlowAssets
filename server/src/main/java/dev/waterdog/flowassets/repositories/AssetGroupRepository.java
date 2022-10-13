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

import dev.waterdog.flowassets.structure.AssetGroup;
import org.hibernate.Hibernate;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

@ApplicationScoped
public class AssetGroupRepository extends AbstractRepository<AssetGroup> {

    @Override
    public String nameIdentifier() {
        return "name";
    }

    @Transactional
    public AssetGroup getInitialized(String name) {
        AssetGroup merge = this.getByName(name);
        if (merge != null) {
            Hibernate.initialize(merge.getAssets());
        }
        return merge;
    }

    @Transactional
    public AssetGroup getInitialized(AssetGroup group) {
        AssetGroup merge = this.getEntityManager().merge(group);
        if (merge != null) {
            Hibernate.initialize(merge.getAssets());
        }
        return merge;
    }
}
