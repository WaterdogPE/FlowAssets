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
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.ValueProvider;
import dev.waterdog.flowassets.repositories.SecretTokensRepository;
import dev.waterdog.flowassets.structure.SecretToken;
import dev.waterdog.flowassets.utils.Helper;
import dev.waterdog.flowassets.views.SecretsView;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class SecretsForm extends AbstractEditForm<SecretToken> {

    private final SecretTokensRepository repository;
    private final SecretsView parent;
    private SecretToken asset = new SecretToken();

    TextField tokenName = new TextField("Name");
    TextField description = new TextField("Bucket Name");
    Binder<SecretToken> binder = new BeanValidationBinder<>(SecretToken.class);

    public SecretsForm(SecretsView parent, SecretTokensRepository repository) {
        super();
        this.parent = parent;
        this.repository = repository;

        this.binder.bindInstanceFields(this);
        this.validateNotEmpty(this.tokenName, SecretToken::getTokenName, SecretToken::setTokenName);
        this.validateNotEmpty(this.description, SecretToken::getDescription, SecretToken::setDescription);

        this.binder.addStatusChangeListener(e -> this.save.setEnabled(this.binder.isValid()));
        this.add(this.tokenName, this.description);
        this.addParentComponents();
    }

    @Override
    protected void addParentComponents() {
        this.addSaveButton();
        this.addCloseButton();
        this.add(this.buttonsLayout);
    }

    @Override
    protected void onSaveButton(ClickEvent<Button> event, SecretToken value) {
        try {
            this.binder.writeBean(value);
        } catch (ValidationException e) {
            Helper.errorNotif("Error: " + e.getLocalizedMessage());
            log.error("Failed validating RepositoriesForm", e);
            return;
        }

        SecretToken token = SecretToken.generateToken(value.getTokenName(), value.getDescription());
        this.repository.save(token);
        this.parent.updateList();
        this.closeForm();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New Token: " + token.getTokenName());
        dialog.add(new Html("<p>Please store this secret. You can not view it again!<br>" + token.getTokenString() + "</p>"));

        Button closeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);;
        closeButton.addClickListener(e -> dialog.close());
        dialog.getHeader().add(closeButton);
        dialog.open();
    }

    @Override
    protected void onDeleteButton(ClickEvent<Button> event, SecretToken value) {
        // Noop
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
    public SecretToken getValue() {
        return this.asset;
    }

    @Override
    protected void setValue0(SecretToken value) {
        this.asset = value;
        this.binder.readBean(value);
    }

    protected void validateNotEmpty(AbstractSinglePropertyField<?, String> component, ValueProvider<SecretToken, String> getter, Setter<SecretToken, String> setter) {
        this.binder.forField(component)
                .withValidator(str -> str != null && !str.trim().isEmpty(), "Can not be empty")
                .bind(getter, setter);
    }
}
