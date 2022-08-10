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
import dev.waterdog.flowassets.repositories.DeployPathsRepository;
import dev.waterdog.flowassets.structure.DeployPath;
import dev.waterdog.flowassets.views.forms.DeployPathsForm;

import javax.inject.Inject;

@PageTitle("FlowAssets | DeployPaths")
@Route(value = "deploy_paths", layout = MainView.class)
public class DeployPathsView extends VerticalLayout {
    private final DeployPathsRepository repository;

    private final Grid<DeployPath> grid = new Grid<>(DeployPath.class);
    private final TextField nameFilter = new TextField();
    private final DeployPathsForm form;

    @Inject
    public DeployPathsView(DeployPathsRepository repository) {
        this.repository = repository;
        this.addClassName("list-view");
        this.setSizeFull();
        this.configureGrid();

        this.form = new DeployPathsForm(this, repository);
        this.form.setWidth("25em");
        this.form.setVisible(false);

        FlexLayout content = new FlexLayout(this.grid, this.form);
        content.setFlexGrow(2, this.grid);
        content.setFlexGrow(1, this.form);
        content.setFlexShrink(0, this.form);
        content.addClassNames("content", "gap-m");
        content.setSizeFull();
        this.add(this.getToolbar(), content);
        this.updateList();
    }

    private void configureGrid() {
        this.grid.addClassNames("contact-grid");
        this.grid.setSizeFull();
        this.grid.removeAllColumns();
        this.grid.addColumn(DeployPath::getName)
                .setHeader("Name");
        this.grid.addColumn(DeployPath::getPath)
                .setHeader("Deploy Path");

        this.grid.getColumns().forEach(col -> col.setAutoWidth(true));
        this.grid.asSingleSelect().addValueChangeListener(event -> this.editDeployPath(event.getValue(), false));
    }

    private HorizontalLayout getToolbar() {
        this.nameFilter.setPlaceholder("Filter by name...");
        this.nameFilter.setClearButtonVisible(true);
        this.nameFilter.setValueChangeMode(ValueChangeMode.LAZY);
        this.nameFilter.addValueChangeListener(e -> this.updateList());

        Button button = new Button("Create DeployPath");
        button.addClickListener(click -> this.addSecret());

        HorizontalLayout toolbar = new HorizontalLayout(this.nameFilter, button);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addSecret() {
        this.grid.asSingleSelect().clear();
        this.form.setValue(new DeployPath());
    }

    public void editDeployPath(DeployPath deployPath, boolean create) {
        if (deployPath == null) {
            this.form.setValue(null, false);
        } else {
            this.form.setValue(deployPath, true);
        }
    }

    public void updateList() {
        this.grid.setItems(this.repository.findByName(this.nameFilter.getValue()));
    }
}
