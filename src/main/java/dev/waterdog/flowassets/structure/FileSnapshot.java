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

import dev.waterdog.flowassets.utils.Streams;
import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;

@Data
@AllArgsConstructor
public class FileSnapshot {
    private String uuid;
    private String fileName;
    private Buffer content;

    public static FileSnapshot createSkeleton(String fileName, InputStream stream) throws IOException {
        return new FileSnapshot(null, fileName, Streams.readToBuffer(stream));
    }

    public static FileSnapshot createSkeleton(String fileName, Buffer content) {
        return new FileSnapshot(null, fileName, content);
    }
}
