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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.waterdog.flowassets.repositories.SecretTokensRepository;
import dev.waterdog.flowassets.structure.SecretToken;
import dev.waterdog.flowassets.utils.Helper;
import dev.waterdog.flowassets.views.forms.SecretsForm;

import javax.inject.Inject;

@PageTitle("FlowAssets | Secrets")
@Route(value = "secrets", layout = MainView.class)
public class SecretsView extends VerticalLayout {
    private final SecretTokensRepository repository;

    private final Grid<SecretToken> grid = new Grid<>(SecretToken.class);
    private final SecretsForm form;

    @Inject
    public SecretsView(SecretTokensRepository repository) {
        this.repository = repository;
        this.addClassName("list-view");
        this.setSizeFull();
        this.configureGrid();

        this.form = new SecretsForm(this, repository);
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
        this.grid.addColumn(SecretToken::getTokenName)
                .setHeader("Token Name");
        this.grid.addColumn(SecretToken::getDescription)
                .setHeader("Description");

        this.grid.addComponentColumn(token -> {
            Button button = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
            button.getElement().setAttribute("aria-label", "Delete");
            button.addClickListener(e -> this.removeSecret(token));
            return button;
        });

        this.grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout getToolbar() {
        Button button = new Button("Create Secret");
        button.addClickListener(click -> this.addSecret());

        HorizontalLayout toolbar = new HorizontalLayout(button);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addSecret() {
        this.grid.asSingleSelect().clear();
        this.form.setValue(new SecretToken());
        this.form.setVisible(true);
        this.addClassName("editing");
    }

    private void removeSecret(SecretToken token) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete Token");
        dialog.add(new Text("Are you sure you want to delete token '" + token.getTokenName() + "' ? This can not be undone!"));


        Button deleteButton = new Button("Delete", (e) -> dialog.close());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        deleteButton.getStyle().set("margin-right", "auto");
        deleteButton.addClickListener(e -> this.deleteSecret(token));
        dialog.getFooter().add(deleteButton);

        Button cancelButton = new Button("Cancel", (e) -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> dialog.close());
        dialog.getFooter().add(cancelButton);
        dialog.open();
    }

    private void deleteSecret(SecretToken token) {
        this.repository.remove(token);
        Helper.successNotif("Removed " + token.getTokenName() + " token");
        this.updateList();
    }

    public void updateList() {
        this.grid.setItems(this.repository.loadAll());
    }
}
