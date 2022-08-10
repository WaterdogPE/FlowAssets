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

import dev.waterdog.flowassets.structure.FlowAsset;
import lombok.Data;

@Data
public class AssetInfoData {
    private boolean valid;
    private boolean found;
    private String uuid;
    private String assetName;
    private String assetRepository;
    private String downloadLink;
    private String deployPath;

    public static AssetInfoData notFound(String uuid) {
        AssetInfoData response = new AssetInfoData();
        response.setFound(false);
        response.setValid(false);
        response.setUuid(uuid);
        return response;
    }

    public static AssetInfoData notFoundName(String assetName) {
        AssetInfoData response = new AssetInfoData();
        response.setFound(false);
        response.setValid(false);
        response.setAssetName(assetName);
        return response;
    }

    public static AssetInfoData fromAsset(FlowAsset asset) {
        AssetInfoData response = new AssetInfoData();
        response.setValid(true);
        response.setFound(true);
        response.setUuid(asset.getUuid().toString());
        response.setAssetName(asset.getAssetName());
        response.setAssetRepository(asset.getAssetRepository());
        if (asset.getDeployPath() != null) {
            response.setDeployPath(asset.getDeployPath().getPath());
        }
        return response;
    }
}
