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

@Data
public class UploadResponseData {
    private boolean success;
    private String message;
    private String assetName;
    private String assetUuid;

    public static UploadResponseData error(String message) {
        UploadResponseData response = new UploadResponseData();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public static UploadResponseData ok(String assetName, String assetUuid) {
        UploadResponseData response = new UploadResponseData();
        response.setSuccess(true);
        response.setAssetName(assetName);
        response.setAssetUuid(assetUuid);
        return response;
    }
}
