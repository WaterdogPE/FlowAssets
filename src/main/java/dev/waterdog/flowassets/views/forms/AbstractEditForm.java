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
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.GeneratedVaadinTextField;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;
import dev.waterdog.flowassets.structure.S3ServerData;

public abstract class AbstractEditForm<T> extends FormLayout {

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");

    public AbstractEditForm() {
        this.addClassName("contact-form");
    }

    protected void addParentComponents() {
        this.add(this.createButtonsLayout());
    }

    public abstract T getValue();
    public abstract void setValue(T value);

    protected abstract void onSaveButton(ClickEvent<Button> event, T value);
    protected abstract void onDeleteButton(ClickEvent<Button> event, T value);
    protected abstract void onExitButton(ClickEvent<Button> event);

    private HorizontalLayout createButtonsLayout() {
        this.save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        this.close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        this.save.addClickShortcut(Key.ENTER);
        this.close.addClickShortcut(Key.ESCAPE);

        this.save.addClickListener(this::validateAndSave);
        this.delete.addClickListener(event -> this.onDeleteButton(event, this.getValue()));
        this.close.addClickListener(this::onExitButton);
        return new HorizontalLayout(this.save, this.delete, this.close);
    }

    protected void validateAndSave(ClickEvent<Button> event) {
        try {
            this.onSaveButton(event, this.getValue());
        } catch (Exception e) {
            Notification.show("Error: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
