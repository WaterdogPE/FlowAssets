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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.views.AssetsView;

import java.util.Arrays;

public class AssetsForm extends AbstractEditForm<FlowAsset> {

    private final AssetsRepository assetsRepository;
    private final AssetsView parent;
    private FlowAsset asset = new FlowAsset();

    TextField assetName = new TextField("Name");
    TextField assetLocation = new TextField("Asset Location");
    ComboBox<String> assetRepository = new ComboBox<>("Repository");

    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);

    Binder<FlowAsset> binder = new BeanValidationBinder<>(FlowAsset.class);

    public AssetsForm(AssetsView parent, AssetsRepository assetsRepository) {
        super();
        this.parent = parent;
        this.assetsRepository = assetsRepository;
        this.binder.bindInstanceFields(this);

        this.assetLocation.setReadOnly(true);
        this.assetRepository.setItems(Arrays.asList("S3 Cloud", "Localhost")); // TODO:
        this.assetRepository.setItemLabelGenerator(asset -> asset);

        // TODO:
        /*singleFileUpload.addStartedListener(event -> {
            System.out.println("uploaded " + event.getFileName());
            try {
                System.out.println(new String(memoryBuffer.getInputStream().readAllBytes()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/

        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));
        this.add(this.assetName, this.assetLocation, this.singleFileUpload, this.assetRepository);
        this.addParentComponents();
    }

    @Override
    protected void onSaveButton(ClickEvent<Button> event, FlowAsset value) {
        try {
            this.binder.writeBean(value);
            this.assetsRepository.save(value);
            this.parent.updateList("");
            this.closeForm();
        } catch (ValidationException e) {
            Notification notification = new Notification("Error: " + e.getLocalizedMessage(), 3000);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            notification.open();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDeleteButton(ClickEvent<Button> event, FlowAsset value) {
        this.assetsRepository.remove(value);
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
}
