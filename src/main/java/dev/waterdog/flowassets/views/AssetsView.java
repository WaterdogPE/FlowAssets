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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.waterdog.flowassets.repositories.AssetsRepository;
import dev.waterdog.flowassets.repositories.DeployPathsRepository;
import dev.waterdog.flowassets.repositories.storage.StorageRepositoryImpl;
import dev.waterdog.flowassets.repositories.storage.StoragesRepository;
import dev.waterdog.flowassets.structure.FlowAsset;
import dev.waterdog.flowassets.views.forms.AssetsForm;

import javax.inject.Inject;

@PageTitle("FlowAssets | Assets")
@Route(value = "assets", layout = MainView.class)
public class AssetsView extends VerticalLayout {
    private final AssetsRepository assetsRepository;
    private final StoragesRepository storagesRepository;

    Grid<FlowAsset> grid = new Grid<>(FlowAsset.class);
    TextField nameFilter = new TextField();
    AssetsForm form;

    @Inject
    public AssetsView(AssetsRepository assetsRepository, StoragesRepository storagesRepository,
                      DeployPathsRepository pathsRepository) {
        this.assetsRepository = assetsRepository;
        this.storagesRepository = storagesRepository;
        this.addClassName("list-view");
        this.setSizeFull();
        this.configureGrid();

        this.form = new AssetsForm(this, assetsRepository, storagesRepository, pathsRepository);
        this.form.setWidth("25em");
        this.form.setVisible(false);

        FlexLayout content = new FlexLayout(this.grid, this.form);
        content.setFlexGrow(2, this.grid);
        content.setFlexGrow(1, this.form);
        content.setFlexShrink(0, this.form);
        content.addClassNames("content", "gap-m");
        content.setSizeFull();
        this.add(this.getToolbar(), content);
    }

    private void configureGrid() {
        this.grid.addClassNames("contact-grid");
        this.grid.setSizeFull();
        this.grid.removeAllColumns();
        this.grid.addColumn(FlowAsset::getAssetName)
                .setHeader("Name").setSortable(true);
        this.grid.addColumn(FlowAsset::getUuid)
                .setHeader("UUID");
        this.grid.addColumn(asset -> asset.getDeployPath() == null ? "None" : asset.getDeployPath().getName())
                .setHeader("Deploy Path");
        this.grid.addColumn(FlowAsset::getAssetRepository)
                .setHeader("Repository").setSortable(true);

        this.grid.addComponentColumn(asset -> {
            Button button = new Button(new Icon(VaadinIcon.DOWNLOAD));
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            button.addClickListener(e -> this.downloadDialog(asset));
            button.getElement().setAttribute("aria-label", "Download");
            return button;
        });

        this.grid.setItems(DataProvider.fromCallbacks(query ->
                        this.assetsRepository.findByName(this.nameFilter.getValue(), query.getPage(), query.getPageSize()).stream(),
                query -> (int) this.assetsRepository.countByName(this.nameFilter.getValue())));


        this.grid.getColumns().forEach(col -> col.setAutoWidth(true));
        this.grid.asSingleSelect().addValueChangeListener(event -> this.editContact(event.getValue(), false));
    }

    private HorizontalLayout getToolbar() {
        this.nameFilter.setPlaceholder("Filter by name...");
        this.nameFilter.setClearButtonVisible(true);
        this.nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        this.nameFilter.addValueChangeListener(e -> this.updateList());

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

    private void downloadDialog(FlowAsset asset) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Download Asset");

        StorageRepositoryImpl storage = this.storagesRepository.getStorageRepository(asset.getAssetRepository());
        if (storage == null) {
            dialog.add(new Paragraph("Asset has invalid storage: " + asset.getAssetRepository()));
        } else {
            Button button = new Button("Download", (e) -> dialog.close());
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            button.getStyle().set("margin-right", "auto");
            dialog.getFooter().add(button);
            dialog.getFooter().add(new Anchor(StorageRepositoryImpl.createDownloadUrl(asset, storage), button));
            dialog.add(new Paragraph("You can download the asset files here:"));
        }

        dialog.open();
    }

    public void updateList() {
        this.grid.getDataProvider().refreshAll();
    }
}
