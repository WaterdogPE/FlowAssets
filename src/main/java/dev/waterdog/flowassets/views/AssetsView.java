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

package dev.waterdog.flowassets.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.views.forms.AssetsForm;

import javax.inject.Inject;

@PageTitle("FlowAssets | Assets")
@Route(value = "assets", layout = MainView.class)
public class AssetsView extends VerticalLayout {
    private final AssetsRepository assetsRepository;

    Grid<FlowAsset> grid = new Grid<>(FlowAsset.class);
    TextField nameFilter = new TextField();
    AssetsForm form;

    @Inject
    public AssetsView(AssetsRepository assetsRepository) {
        this.assetsRepository = assetsRepository;
        this.addClassName("list-view");
        this.setSizeFull();
        this.configureGrid();

        this.form = new AssetsForm(this, assetsRepository);
        this.form.setWidth("25em");
        this.form.setVisible(false);

        FlexLayout content = new FlexLayout(this.grid, this.form);
        content.setFlexGrow(2, this.grid);
        content.setFlexGrow(1, this.form);
        content.setFlexShrink(0, this.form);
        content.addClassNames("content", "gap-m");
        content.setSizeFull();
        this.add(this.getToolbar(), content);

        this.updateList("");
    }

    private void configureGrid() {
        this.grid.addClassNames("contact-grid");
        this.grid.setSizeFull();
        this.grid.removeAllColumns();
        this.grid.addColumn(FlowAsset::getAssetName)
                .setHeader("Name");
        this.grid.addColumn(FlowAsset::getAssetRepository)
                .setHeader("Repository");
        this.grid.addColumn(FlowAsset::getAssetLocation)
                .setHeader("Location");
        this.grid.getColumns().forEach(col -> col.setAutoWidth(true));
        this.grid.asSingleSelect().addValueChangeListener(event -> this.editContact(event.getValue(), false));
    }

    private HorizontalLayout getToolbar() {
        this.nameFilter.setPlaceholder("Filter by name...");
        this.nameFilter.setClearButtonVisible(true);
        this.nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        this.nameFilter.addValueChangeListener(e -> this.updateList(e.getValue()));

        Button button = new Button("Add Asset");
        button.addClickListener(click -> this.addContact());

        HorizontalLayout toolbar = new HorizontalLayout(nameFilter, button);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addContact() {
        this.grid.asSingleSelect().clear();
        this.editContact(new FlowAsset(), true);
    }

    public void editContact(FlowAsset asset, boolean create) {
        if (asset == null) {
            this.form.closeForm();
        } else {
            this.form.setValue(asset, create);
            this.form.setVisible(true);
            this.addClassName("editing");
        }
    }

    public void updateList(String filter) {
        this.grid.setItems(this.assetsRepository.findByName(filter));
    }
}
