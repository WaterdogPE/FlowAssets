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

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.ValueProvider;
import dev.waterdog.flowassets.repositories.DeployPathsRepository;
import dev.waterdog.flowassets.structure.DeployPath;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.utils.Helper;
import dev.waterdog.flowassets.views.DeployPathsView;
import lombok.extern.jbosslog.JBossLog;

import javax.transaction.Transactional;
import java.nio.file.Paths;

@JBossLog
public class DeployPathsForm extends AbstractEditForm<DeployPath> {

    private final DeployPathsRepository repository;
    private final DeployPathsView parent;
    private DeployPath deployPath = new DeployPath();

    TextField pathName = new TextField("Name");
    TextField pathField = new TextField("Deploy Path");
    Binder<DeployPath> binder = new BeanValidationBinder<>(DeployPath.class);

    private boolean editMode;

    public DeployPathsForm(DeployPathsView parent, DeployPathsRepository repository) {
        this.parent = parent;
        this.repository = repository;

        this.validateNotEmpty(this.pathName, DeployPath::getName, DeployPath::setName);
        this.validateNotEmptyPath(this.pathField, DeployPath::getPath, DeployPath::setPath);

        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));
        this.add(this.pathName, this.pathField);
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

        this.pathName.setReadOnly(this.editMode);
    }

    @Override
    protected void onSaveButton(ClickEvent<Button> event, DeployPath value) {
        try {
            this.binder.writeBean(value);
        } catch (ValidationException e) {
            Helper.errorNotif("Error: " + e.getLocalizedMessage());
            log.error("Failed validating RepositoriesForm", e);
            return;
        }

        Helper.successNotif("Created new DeployPath: " + value.getName());

        this.repository.save(value);
        this.parent.updateList();
        this.setValue(null);
    }

    @Override @Transactional
    protected void onDeleteButton(ClickEvent<Button> event, DeployPath value) {
        DeployPath deployPath = this.repository.getInitialized(value);
        if (deployPath.getAssets() != null && !deployPath.getAssets().isEmpty()) {
            StringBuilder text = new StringBuilder("Deploy path " + deployPath.getName() + " is still used in FlowAssets:");
            for (FlowAsset asset : deployPath.getAssets()) {
                text.append("\n- ").append(asset.getAssetName());
            }
            Helper.infoDialog("Path still used", text.toString(), e -> {});
            return;
        }

        Helper.successNotif("DeployPath " + deployPath.getName() + " removed");
        this.repository.remove(deployPath);
        this.setValue(null);
        this.parent.updateList();
    }

    @Override
    protected void onExitButton(ClickEvent<Button> event) {
        this.setValue(null);
    }

    @Override
    public DeployPath getValue() {
        return this.deployPath;
    }

    public void setValue(DeployPath value, boolean editMode) {
        this.editMode = editMode;
        this.setValue(value);
    }

    @Override
    protected void setValue0(DeployPath value) {
        this.deployPath = value;
        this.binder.readBean(value);
    }

    protected void validateNotEmpty(AbstractSinglePropertyField<?, String> component, ValueProvider<DeployPath, String> getter, Setter<DeployPath, String> setter) {
        this.binder.forField(component)
                .withValidator(str -> str != null && !str.trim().isEmpty(), "Can not be empty")
                .bind(getter, setter);
    }

    protected void validateNotEmptyPath(AbstractSinglePropertyField<?, String> component, ValueProvider<DeployPath, String> getter, Setter<DeployPath, String> setter) {
        this.binder.forField(component)
                .withValidator(str -> str != null && !str.trim().isEmpty() && this.validatePath(str), "Can not be empty")
                .bind(getter, setter);
    }

    private boolean validatePath(String path) {
        try {
            Paths.get(path);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
