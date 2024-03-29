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
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.*;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import dev.waterdog.flowassets.repositories.AbstractRepository;
import dev.waterdog.flowassets.repositories.AssetGroupRepository;
import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.repositories.DeployPathsRepository;
import dev.waterdog.flowassets.repositories.storage.StorageRepositoryImpl;
import dev.waterdog.flowassets.repositories.storage.StoragesRepository;
import dev.waterdog.flowassets.structure.*;
import dev.waterdog.flowassets.utils.DataProviderBuilder;
import dev.waterdog.flowassets.utils.Helper;
import dev.waterdog.flowassets.views.AssetsView;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@JBossLog
public class AssetsForm extends AbstractEditForm<FlowAsset> {
    private static final DeployPathWrapper NO_PATH_DATA = new DeployPathWrapper("None", null);

    private final AssetsRepository assetsRepository;
    private final StoragesRepository storages;
    private final DeployPathsRepository pathsRepository;
    private final AssetGroupRepository assetsGroupRepository;

    private final AssetsView parent;
    private FlowAsset asset = new FlowAsset();
    private boolean editMode = false;

    private boolean uploaded;
    private String uploadName;

    // Editable fields
    TextField assetName = new TextField("Name");
    ComboBox<RepositoryWrapper> assetRepository = new ComboBox<>("Repository");
    ComboBox<DeployPathWrapper> deployPath = new ComboBox<>("Deploy Path");
    MultiSelectComboBox<AssetGroup> assetGroup = new MultiSelectComboBox<>("Groups");

    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);

    // Read-only fields
    TextField assetLocation = new TextField("Remote Asset Location");

    Binder<FlowAsset> binder = new BeanValidationBinder<>(FlowAsset.class);

    public AssetsForm(AssetsView parent, AssetsRepository assetsRepository,
                      StoragesRepository storages, DeployPathsRepository pathsRepository, AssetGroupRepository assetsGroupRepository) {
        super();
        this.parent = parent;
        this.assetsRepository = assetsRepository;
        this.storages = storages;
        this.pathsRepository = pathsRepository;
        this.assetsGroupRepository = assetsGroupRepository;

        // Init components
        this.assetLocation.setReadOnly(true);

        this.deployPath.setPlaceholder("Select deploy path");
        this.deployPath.setItemLabelGenerator(DeployPathWrapper::getPathName);

        DataProviderBuilder<DeployPath, DeployPathWrapper> builder = DataProviderBuilder.create();
        builder.repository(this.pathsRepository);
        builder.mapper(stream -> stream.map(path -> new DeployPathWrapper(path.getName(), path)));
        builder.extraValues(NO_PATH_DATA);
        this.deployPath.setItems(builder.build());

        this.assetGroup.setPlaceholder("Select group");
        this.assetGroup.setItemLabelGenerator(AssetGroup::getName);
        this.assetGroup.setItems(AbstractRepository.createDataProviderMapped(this.assetsGroupRepository, AssetGroup::getName));

        this.assetRepository.setPlaceholder("Select repository");
        this.assetRepository.setItemLabelGenerator(RepositoryWrapper::getRepositoryName);
        this.assetRepository.setItems(AbstractRepository.createDataProvider(this.storages.getS3ConfigRepository(), stream -> {
            Stream<RepositoryWrapper> repositoryStream = stream
                    .map(serverData -> new RepositoryWrapper(RepositoryType.REMOTE_S3, serverData.getServerName()));
            return Stream.concat(repositoryStream, Stream.of(RepositoryWrapper.LOCAL));
        }));

        this.singleFileUpload.setDropAllowed(false);
        this.singleFileUpload.addSucceededListener(this::onUpload);
        this.singleFileUpload.setMaxFileSize(1024 * 1024 * 100);

        // Bind form
        this.binder.forField(this.assetRepository).bind(asset -> this.createRepositoryFromName(asset.getAssetRepository()),
                (asset, repository) -> asset.setAssetRepository(repository.getRepositoryName()));

        this.binder.forField(this.deployPath).bind(this::createDeployPathWrapper,
                (asset, path) -> asset.setDeployPath(path.getDeployPath()));

        this.binder.forField(this.assetGroup).bind(FlowAsset::getGroups, FlowAsset::setGroups);

        this.binder.bindInstanceFields(this);
        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid() && (this.editMode || this.uploaded)));
        this.add(this.assetName, this.deployPath, this.assetGroup, this.assetLocation, this.singleFileUpload, this.assetRepository);
        this.addParentComponents();
    }

    @Override
    protected void adjustComponents() {
        if (this.getValue() == null) {
            this.setVisible(false);
            this.parent.removeClassName("editing");
        } else {
            this.setVisible(true);
            this.parent.addClassName("editing");
        }

        this.singleFileUpload.clearFileList();
        this.assetRepository.setReadOnly(this.editMode); // Can not change repository once created
        this.assetLocation.setVisible(this.editMode); // Only show location when file is uploaded
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

        if (this.editMode) {
            this.finishSave(this.assetsRepository.save(value));
            return;
        }

        if (this.assetsRepository.getByName(value.getAssetName()) != null) {
            Helper.errorNotif("Asset with name '"+ value.getAssetName() + "' already exists!");
            return;
        }

        StorageRepositoryImpl storageRepository = this.storages.getStorageRepository(asset.getAssetRepository());
        if (storageRepository == null) {
            Helper.errorNotif("Unknown repository: " + asset.getAssetRepository());
            return;
        }

        FileSnapshot snapshot;
        try {
            snapshot = FileSnapshot.createSkeleton(this.uploadName, this.memoryBuffer.getInputStream());
        } catch (IOException e) {
            log.error("Can not create buffer", e);
            Helper.errorNotif("Unable to create asset");
            return;
        }

        UI ui = UI.getCurrent();
        CompletableFuture<FlowAsset> future = FlowAsset.uploadAsset(value, snapshot, this.assetsRepository, storageRepository);
        future.whenComplete((i, error) -> {
            if (error != null) {
                Helper.push(ui, () -> Helper.errorNotif("Can not save asset"));
                log.error("Can not save asset", error);
            } else {
                Helper.push(ui, () -> this.finishSave(asset));
            }
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
        FlowAsset.deleteAsset(asset, this.assetsRepository, storageRepository).whenComplete((v, error) -> {
            ui.access(this.parent::updateList);
            if (error == null) {
                Helper.push(ui, () -> Helper.successNotif("Deleted asset: " + value.getAssetName()));
            } else {
                Helper.push(ui, () -> Helper.errorNotif("Can not delete asset: " + error.getLocalizedMessage()));
                log.error("Can not delete asset", error);
            }
        });
        this.closeForm();
    }

    @Override
    protected void onExitButton(ClickEvent<Button> event) {
        this.closeForm();
    }

    public void closeForm() {
        this.setValue(null);
    }

    @Override
    public FlowAsset getValue() {
        return this.asset;
    }

    public void setValue(FlowAsset value, boolean create) {
        this.editMode = !create;
        this.setValue(value);
    }

    @Override
    protected void setValue0(FlowAsset value) {
        this.asset = value;
        this.binder.readBean(value);
    }

    private RepositoryWrapper createRepositoryFromName(String name) {
        if (name == null || RepositoryType.getTypeFromName(name) == RepositoryType.LOCAL) {
            return RepositoryWrapper.LOCAL;
        }

        S3ServerData serverData;
        if ((serverData = this.storages.getS3ConfigRepository().getByName(name)) == null) {
            return null;
        }
        return new RepositoryWrapper(RepositoryType.REMOTE_S3, serverData.getServerName());
    }

    @Transactional
    private DeployPathWrapper createDeployPathWrapper(FlowAsset asset) {
        DeployPath deployPath = asset.getDeployPath();
        return deployPath == null ? NO_PATH_DATA :
                new DeployPathWrapper(deployPath.getName(), deployPath);
    }

    @Data
    private static class DeployPathWrapper {
        private final String pathName;
        private final DeployPath deployPath;
    }
}
