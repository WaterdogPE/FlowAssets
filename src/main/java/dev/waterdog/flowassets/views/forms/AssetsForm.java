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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.*;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.repositories.storage.StorageRepositoryImpl;
import dev.waterdog.flowassets.repositories.storage.StoragesRepository;
import dev.waterdog.flowassets.structure.*;
import dev.waterdog.flowassets.utils.Helper;
import dev.waterdog.flowassets.utils.Streams;
import dev.waterdog.flowassets.views.AssetsView;
import io.vertx.core.buffer.Buffer;
import lombok.extern.jbosslog.JBossLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JBossLog
public class AssetsForm extends AbstractEditForm<FlowAsset> {

    private final AssetsRepository assetsRepository;
    private final StoragesRepository storages;

    private final AssetsView parent;

    private FlowAsset asset = new FlowAsset();
    private boolean editMode = false;

    private boolean uploaded;
    private String uploadName;

    TextField assetName = new TextField("Name");
    TextField assetLocation = new TextField("Asset Location");
    ComboBox<RepositoryData> assetRepository = new ComboBox<>("Repository");

    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);

    Binder<FlowAsset> binder = new BeanValidationBinder<>(FlowAsset.class);

    public AssetsForm(AssetsView parent, AssetsRepository assetsRepository, StoragesRepository storages) {
        super();
        this.parent = parent;
        this.assetsRepository = assetsRepository;
        this.storages = storages;

        List<RepositoryData> servers = new ArrayList<>();
        for (S3ServerData serverData : storages.getS3ConfigRepository().getAll()) {
            servers.add(new RepositoryData(RepositoryType.REMOTE_S3, serverData.getServerName()));
        }
        servers.add(RepositoryData.LOCAL);
        this.assetRepository.setItems(servers);
        this.assetRepository.setItemLabelGenerator(RepositoryData::getRepositoryName);

        this.assetLocation.setReadOnly(true);
        this.singleFileUpload.addSucceededListener(this::onUpload);

        this.binder.forField(this.assetRepository).bind(asset -> this.createRepositoryFromName(asset.getAssetRepository()),
                (asset, repository) -> asset.setAssetRepository(repository.getRepositoryName()));
        this.binder.bindInstanceFields(this);
        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid() && (this.editMode || this.uploaded)));
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
        if (!this.editMode && !this.uploaded) {
            Helper.errorNotif("Upload has not finished yet! Please wait");
            return;
        }

        try {
            this.binder.writeBean(value);
        } catch (ValidationException e) {
            Helper.errorNotif("Error: " + e.getLocalizedMessage());
            log.error("Failed validating AssetsForm", e);
            return;
        }

        if (!this.assetsRepository.findByName(value.getAssetName()).isEmpty()) {
            Helper.errorNotif("Asset with name '"+ value.getAssetName() + "' already exists!");
            return;
        }

        FlowAsset asset = this.assetsRepository.save(value);
        if (this.editMode) {
            this.finishSave(asset);
            return;
        }

        StorageRepositoryImpl storageRepository = this.storages.getStorageRepository(asset.getAssetRepository());
        if (storageRepository == null) {
            Helper.errorNotif("Unknown repository: " + asset.getAssetRepository());
            return;
        }

        FileSnapshot snapshot = this.createFileSnapshot(asset.getUuid());
        if (snapshot == null) {
            Helper.errorNotif("Unable to create asset");
            return;
        }

        // TODO: handle error properly

        UI ui = UI.getCurrent();
        storageRepository.saveFileSnapshot(snapshot).whenComplete((i, error) -> {
            if (error != null) {
                Helper.push(ui, () -> Helper.errorNotif("Can not save asset"));
                log.error("Can not save asset", error);
                return;
            }
            String baseUrl = asset.getUuid() + "/" + this.uploadName;

            asset.setAssetLocation(baseUrl);
            this.assetsRepository.save(asset);
            Helper.push(ui, () -> this.finishSave(asset));
        });
    }

    private void finishSave(FlowAsset asset) {
        try {
            this.parent.updateList();
            this.closeForm();
            Helper.successNotif("Saved " + asset.getAssetName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDeleteButton(ClickEvent<Button> event, FlowAsset value) {
        StorageRepositoryImpl storageRepository = this.storages.getStorageRepository(asset.getAssetRepository());
        if (storageRepository == null) {
            Helper.errorNotif("Unknown repository: " + asset.getAssetRepository());
            return;
        }

        UI ui = UI.getCurrent();
        storageRepository.deleteSnapshots(value.getUuid().toString()).whenComplete((v, error) -> {
           if (error == null) {
               Helper.push(ui, () -> Helper.successNotif("Deleted asset files: " + value.getAssetName()));
           } else {
               Helper.push(ui, () -> Helper.errorNotif("Can not delete asset files: " + error.getLocalizedMessage()));
               log.error("Can not delete asset", error);
           }
        });

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
        this.singleFileUpload.clearFileList();
    }

    @Override
    public FlowAsset getValue() {
        return this.asset;
    }

    public void setValue(FlowAsset value, boolean create) {
        this.setValue(value);
        this.editMode = !create;
        this.assetRepository.setReadOnly(this.editMode);
    }

    @Override
    public void setValue(FlowAsset value) {
        this.asset = value;
        this.binder.readBean(value);
    }

    private RepositoryData createRepositoryFromName(String name) {
        if (name == null || RepositoryType.getTypeFromName(name) == RepositoryType.LOCAL) {
            return RepositoryData.LOCAL;
        }

        S3ServerData serverData;
        if ((serverData = this.storages.getS3ConfigRepository().findByName(name)) == null) {
            return null;
        }
        return new RepositoryData(RepositoryType.REMOTE_S3, serverData.getServerName());
    }

    private FileSnapshot createFileSnapshot(UUID uuid) {
        Buffer buffer;
        try {
            buffer = Streams.readToBuffer(this.memoryBuffer.getInputStream());
        } catch (IOException e) {
            log.error("Can not create buffer", e);
            return null;
        }
        return new FileSnapshot(uuid.toString(), this.uploadName, buffer);
    }
}
