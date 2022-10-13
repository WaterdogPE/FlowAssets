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

package dev.waterdog.flowassets.structure.rest;

import lombok.Data;

import java.util.List;

@Data
public class GroupInfoData {
    private boolean found;
    private String groupName;
    private List<AssetInfoData> assets;

    public static GroupInfoData notFound(String groupName) {
        GroupInfoData response = new GroupInfoData();
        response.setFound(false);
        response.setGroupName(groupName);
        return response;
    }
}
