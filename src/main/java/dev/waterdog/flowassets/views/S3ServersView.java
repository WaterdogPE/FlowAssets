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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.waterdog.flowassets.repositories.S3ServersRepository;
import dev.waterdog.flowassets.structure.S3ServerData;
import dev.waterdog.flowassets.views.forms.S3ServersForm;

import javax.inject.Inject;

@PageTitle("FlowAssets | S3 Servers")
@Route(value = "servers", layout = MainView.class)
public class S3ServersView extends VerticalLayout {
    private final S3ServersRepository repository;

    Grid<S3ServerData> grid = new Grid<>(S3ServerData.class);
    S3ServersForm form;

    @Inject
    public S3ServersView(S3ServersRepository repository) {
        this.repository = repository;
        this.addClassName("list-view");
        this.setSizeFull();
        this.configureGrid();

        this.form = new S3ServersForm(this, repository);
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
        this.grid.addColumn(S3ServerData::getServerName)
                .setHeader("Server Name");
        this.grid.addColumn(S3ServerData::getBucketName)
                .setHeader("Bucket");
        this.grid.addColumn(S3ServerData::getBucketUrl)
                .setHeader("Address");
        this.grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout getToolbar() {
        Button button = new Button("Add Repository");
        button.addClickListener(click -> this.addServer());

        HorizontalLayout toolbar = new HorizontalLayout(button);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addServer() {
        this.grid.asSingleSelect().clear();
        this.form.setValue(new S3ServerData());
        this.form.setVisible(true);
        this.addClassName("editing");
    }

    public void updateList() {
        this.grid.setItems(this.repository.getAll());
    }
}
