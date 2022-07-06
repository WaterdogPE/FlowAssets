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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.eclipse.microprofile.jwt.JsonWebToken;

public class Helper {

    public static void push(UI ui, Runnable runnable) {
        ui.access(() -> {
            runnable.run();
            ui.push();
        });
    }

    public static void errorNotif(String message) {
        Notification notification = new Notification(message, 3000);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    public static void successNotif(String message) {
        Notification notification = new Notification(message, 3000);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    public static String getUserName(JsonWebToken token) {
        if (token == null) {
            return "";
        }

        String name = token.getName();
        if (name == null) {
            name = token.getClaim("given_name");
        }
        return name == null ? "" : name;
    }

    public static String error(String message) {
        return "{\"status\":\"error\",\"message\":\""+message+"\"}";
    }

    public static String success(String message) {
        return "{\"status\":\"ok\",\"message\":\""+message+"\"}";
    }

    public static String success(String message, String result) {
        return "{\"status\":\"ok\",\"message\":\""+message+"\",\"result\":\""+result+"\"}";
    }

}
