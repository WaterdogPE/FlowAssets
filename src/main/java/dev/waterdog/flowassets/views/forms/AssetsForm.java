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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.*;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.repositories.S3ServersRepository;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.structure.RepositoryData;
import dev.waterdog.flowassets.structure.RepositoryType;
import dev.waterdog.flowassets.structure.S3ServerData;
import dev.waterdog.flowassets.utils.Notifications;
import dev.waterdog.flowassets.views.AssetsView;
import lombok.extern.jbosslog.JBossLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JBossLog
public class AssetsForm extends AbstractEditForm<FlowAsset> {

    private final AssetsRepository assetsRepository;
    private final S3ServersRepository serversRepository;
    private final AssetsView parent;

    private volatile boolean uploaded;
    private String uploadName;

    private FlowAsset asset = new FlowAsset();

    TextField assetName = new TextField("Name");
    TextField assetLocation = new TextField("Asset Location");
    ComboBox<RepositoryData> assetRepository = new ComboBox<>("Repository");

    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);

    Binder<FlowAsset> binder = new BeanValidationBinder<>(FlowAsset.class);

    public AssetsForm(AssetsView parent, AssetsRepository assetsRepository, S3ServersRepository serversRepository) {
        super();
        this.parent = parent;
        this.assetsRepository = assetsRepository;
        this.serversRepository = serversRepository;

        List<RepositoryData> servers = new ArrayList<>();
        for (S3ServerData serverData : serversRepository.getAll()) {
            servers.add(new RepositoryData(RepositoryType.REMOTE_S3, serverData.getServerName()));
        }
        servers.add(RepositoryData.LOCAL);
        this.assetRepository.setItems(servers);
        this.assetRepository.setItemLabelGenerator(RepositoryData::getRepositoryName);

        this.assetLocation.setReadOnly(true);

        this.binder.forField(this.assetRepository).bind(asset -> this.createRepositoryFromName(asset.getAssetRepository(), serversRepository),
                (asset, repository) -> asset.setAssetRepository(repository.getRepositoryName()));

        this.singleFileUpload.addSucceededListener(this::onUpload);

        this.binder.bindInstanceFields(this);
        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid() && this.uploaded));
        this.add(this.assetName, this.assetLocation, this.singleFileUpload, this.assetRepository);
        this.addParentComponents();
    }

    private void onUpload(SucceededEvent event) {
        this.uploaded = true;
        this.uploadName = event.getFileName();
        this.save.setEnabled(this.binder.isValid());
    }

    @Override
    protected void onSaveButton(ClickEvent<Button> event, FlowAsset value) {
        if (!this.uploaded) {
            Notifications.error("Upload has not finished yet! Please wait");
            return;
        }

        try {
            this.binder.writeBean(value);
        } catch (ValidationException e) {
            Notifications.error("Error: " + e.getLocalizedMessage());
            log.error("Failed validating AssetsForm", e);
            return;
        }

        RepositoryData repositoryData = this.assetRepository.getValue();
        String baseUrl = "/";
        if (repositoryData.getType() == RepositoryType.REMOTE_S3) {
            // TODO: base url
        }

        // TODO: more randomize
        baseUrl += UUID.nameUUIDFromBytes(value.getAssetName().getBytes()) + "/";
        String[] namespaces = this.uploadName.split("\\.");
        baseUrl += value.getAssetName().toLowerCase() + "." + namespaces[namespaces.length - 1];

        // Manually sync url
        value.setAssetLocation(baseUrl);
        this.assetLocation.setValue(baseUrl);

        this.assetsRepository.save(value);
        this.parent.updateList();
        this.closeForm();
    }

    @Override
    protected void onDeleteButton(ClickEvent<Button> event, FlowAsset value) {
        this.assetsRepository.remove(value);
        this.parent.updateList();
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
        this.singleFileUpload.clearFileList(); // TODO: verify if it clears buff too
    }

    @Override
    public FlowAsset getValue() {
        return this.asset;
    }

    public void setValue(FlowAsset value, boolean create) {
        this.setValue(value);
        this.assetRepository.setReadOnly(!create);
    }

    @Override
    public void setValue(FlowAsset value) {
        this.asset = value;
        this.binder.readBean(value);
    }

    private RepositoryData createRepositoryFromName(String name, S3ServersRepository repository) {
        if (name == null || name.equals(RepositoryType.LOCAL.getName())) {
            return RepositoryData.LOCAL;
        }

        S3ServerData serverData;
        if ((serverData = repository.findByName(name)) == null) {
            return null;
        }
        return new RepositoryData(RepositoryType.REMOTE_S3, serverData.getServerName());
    }
}
