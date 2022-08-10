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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Entity
@Getter @Setter
@ToString(callSuper = true)
@Table(name = "secret_tokens")
public class SecretToken extends PanacheEntity {

    private static final int SECRET_LENGTH = 16;

    @Column(name = "name")
    private String tokenName;

    private String description;

    @Column(name = "token_hash")
    private String tokenHash;

    // Only set when generating new token
    @Transient
    @Setter(AccessLevel.PRIVATE)
    private String tokenString;

    public static SecretToken generateToken(String name, String description) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_LENGTH];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder().encodeToString(Base64.getEncoder().encode(bytes));
        String tokenHash = createTokenHash(token);

        SecretToken secretToken = new SecretToken();
        secretToken.setTokenName(name);
        secretToken.setDescription(description);
        secretToken.setTokenHash(tokenHash);
        secretToken.setTokenString(token);
        return secretToken;
    }

    public static String createTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

}
