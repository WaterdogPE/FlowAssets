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

package dev.waterdog.flowassets.views.forms;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.GeneratedVaadinTextField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.ValueProvider;
import dev.waterdog.flowassets.repositories.S3ServersRepository;
import dev.waterdog.flowassets.structure.S3ServerData;
import dev.waterdog.flowassets.utils.Helper;
import dev.waterdog.flowassets.views.S3ServersView;
import lombok.extern.jbosslog.JBossLog;

import java.net.URL;

@JBossLog
public class S3ServersForm extends AbstractEditForm<S3ServerData> {

    private final S3ServersRepository repository;
    private final S3ServersView parent;
    private S3ServerData asset = new S3ServerData();

    TextField serverName = new TextField("Name");
    TextField bucketName = new TextField("Bucket Name");
    TextField bucketUrl = new TextField("Bucket URL");
    PasswordField accessKey = new PasswordField("Access Key");
    PasswordField secretkey = new PasswordField("Secret Key");
    Binder<S3ServerData> binder = new BeanValidationBinder<>(S3ServerData.class);

    public S3ServersForm(S3ServersView parent, S3ServersRepository assetsRepository) {
        super();
        this.parent = parent;
        this.repository = assetsRepository;
        this.binder.bindInstanceFields(this);

        this.binder.forField(this.bucketUrl)
                .withValidator(this::testUrl, "Invalid URL")
                .bind(S3ServerData::getBucketUrl, S3ServerData::setBucketUrl);
        this.validateNotEmpty(this.serverName, S3ServerData::getServerName, S3ServerData::setServerName);
        this.validateNotEmpty(this.bucketName, S3ServerData::getBucketName, S3ServerData::setBucketName);
        this.validateNotEmpty(this.accessKey, S3ServerData::getAccessKey, S3ServerData::setAccessKey);
        this.validateNotEmpty(this.secretkey, S3ServerData::getSecretkey, S3ServerData::setSecretkey);

        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));
        this.add(this.serverName, this.bucketName, this.bucketUrl, this.accessKey, this.secretkey);
        this.addParentComponents();
    }

    @Override
    protected void onSaveButton(ClickEvent<Button> event, S3ServerData value) {
        try {
            this.binder.writeBean(value);
            this.repository.save(value);
            this.parent.updateList();
            this.closeForm();
        } catch (ValidationException e) {
            Helper.errorNotif("Error: " + e.getLocalizedMessage());
            log.error("Failed validating RepositoriesForm", e);
        }
    }

    @Override
    protected void onDeleteButton(ClickEvent<Button> event, S3ServerData value) {
        this.repository.remove(value);
        this.closeForm();
    }

    @Override
    protected void onExitButton(ClickEvent<Button> event) {
        this.closeForm();
    }

    public void closeForm() {
        this.setValue(null);
        this.setVisible(false);
        this.parent.removeClassName("editing");
    }

    @Override
    public S3ServerData getValue() {
        return this.asset;
    }

    @Override
    public void setValue(S3ServerData value) {
        this.asset = value;
        this.binder.readBean(value);
    }

    protected void validateNotEmpty(GeneratedVaadinTextField<?, String> component, ValueProvider<S3ServerData, String> getter, Setter<S3ServerData, String> setter) {
        this.binder.forField(component)
                .withValidator(str -> !str.trim().isEmpty(), "Can not be empty")
                .bind(getter, setter);
    }

    private boolean testUrl(String address) {
        try {
            new URL(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
