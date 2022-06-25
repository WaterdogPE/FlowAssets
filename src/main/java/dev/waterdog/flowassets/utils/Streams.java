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

package dev.waterdog.flowassets.utils;

import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.io.InputStream;

public class Streams {

    public static Buffer readToBuffer(InputStream stream) throws IOException {
        Buffer buffer = Buffer.buffer();
        byte[] buf = new byte[1024 * 8];
        while (stream.available() > 0) {
            int read = stream.read(buf);
            if (read < 1) {
                break;
            }
            buffer.appendBytes(buf, 0, read);
        }
        return buffer;
    }
}
