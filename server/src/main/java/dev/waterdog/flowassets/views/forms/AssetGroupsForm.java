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
import dev.waterdog.flowassets.repositories.AssetGroupRepository;
import dev.waterdog.flowassets.structure.AssetGroup;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.utils.Helper;
import dev.waterdog.flowassets.views.AssetGroupsView;
import lombok.extern.jbosslog.JBossLog;

import javax.transaction.Transactional;

@JBossLog
public class AssetGroupsForm extends AbstractEditForm<AssetGroup> {

    private final AssetGroupRepository repository;
    private final AssetGroupsView parent;
    private AssetGroup group = new AssetGroup();

    TextField groupName = new TextField("Name");
    Binder<AssetGroup> binder = new BeanValidationBinder<>(AssetGroup.class);

    private boolean editMode;

    public AssetGroupsForm(AssetGroupsView parent, AssetGroupRepository repository) {
        this.parent = parent;
        this.repository = repository;

        this.validateNotEmpty(this.groupName, AssetGroup::getName, AssetGroup::setName);

        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));
        this.add(this.groupName);
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

        this.groupName.setReadOnly(this.editMode);
    }

    @Override
    protected void onSaveButton(ClickEvent<Button> event, AssetGroup value) {
        try {
            this.binder.writeBean(value);
        } catch (ValidationException e) {
            Helper.errorNotif("Error: " + e.getLocalizedMessage());
            log.error("Failed validating RepositoriesForm", e);
            return;
        }

        Helper.successNotif("Created new Group: " + value.getName());

        this.repository.save(value);
        this.parent.updateList();
        this.setValue(null);
    }

    @Override @Transactional
    protected void onDeleteButton(ClickEvent<Button> event, AssetGroup value) {
        AssetGroup group = this.repository.getInitialized(value);
        if (group.getAssets() != null && !group.getAssets().isEmpty()) {
            StringBuilder text = new StringBuilder("Group " + group.getName() + " is still used in FlowAssets:");
            for (FlowAsset asset : group.getAssets()) {
                text.append("\n- ").append(asset.getAssetName());
            }
            Helper.infoDialog("Group still used", text.toString(), e -> {});
            return;
        }

        Helper.successNotif("Group " + group.getName() + " removed");
        this.repository.remove(group);
        this.setValue(null);
        this.parent.updateList();
    }

    @Override
    protected void onExitButton(ClickEvent<Button> event) {
        this.setValue(null);
    }

    @Override
    public AssetGroup getValue() {
        return this.group;
    }

    public void setValue(AssetGroup value, boolean editMode) {
        this.editMode = editMode;
        this.setValue(value);
    }

    @Override
    protected void setValue0(AssetGroup value) {
        this.group = value;
        this.binder.readBean(value);
    }

    protected void validateNotEmpty(AbstractSinglePropertyField<?, String> component, ValueProvider<AssetGroup, String> getter, Setter<AssetGroup, String> setter) {
        this.binder.forField(component)
                .withValidator(str -> str != null && !str.trim().isEmpty(), "Can not be empty")
                .bind(getter, setter);
    }
}
