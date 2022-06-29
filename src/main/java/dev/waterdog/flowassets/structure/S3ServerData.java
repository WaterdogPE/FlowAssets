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

package dev.waterdog.flowassets.structure;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@ToString(callSuper = true)
@Getter
@Setter
@Cacheable
@Table(name = "s3_servers")
public class S3ServerData extends PanacheEntity {

    @Column(name = "name")
    private String serverName;

    @Column(name = "bucket_url")
    private String bucketUrl;

    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name = "access_key")
    private String accessKey;

    @Column(name = "secret_key")
    private String secretkey;

    @Column(name = "region_name")
    private String regionName;
}
