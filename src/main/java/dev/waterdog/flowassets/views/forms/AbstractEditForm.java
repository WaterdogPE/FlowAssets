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

public abstract class AbstractEditForm<T> extends FormLayout {

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");

    HorizontalLayout buttonsLayout = new HorizontalLayout();

    public AbstractEditForm() {
        this.addClassName("contact-form");
    }

    protected void addParentComponents() {
        this.addSaveButton();
        this.addDeleteButton();
        this.addCloseButton();
        this.add(this.buttonsLayout);
    }

    public abstract T getValue();
    protected abstract void setValue0(T value);

    public final void setValue(T value) {
        this.setValue0(value);
        this.adjustComponents();
    }

    protected abstract void onSaveButton(ClickEvent<Button> event, T value);
    protected abstract void onDeleteButton(ClickEvent<Button> event, T value);
    protected abstract void onExitButton(ClickEvent<Button> event);

    protected void adjustComponents() {
        // Run when setting new value
    }

    protected void addSaveButton() {
        this.save.addClickShortcut(Key.ENTER);
        this.save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        this.save.addClickListener(this::validateAndSave);
        this.buttonsLayout.add(this.save);
    }

    protected void addDeleteButton() {
        this.delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        this.delete.addClickListener(event -> this.onDeleteButton(event, this.getValue()));
        this.buttonsLayout.add(this.delete);
    }

    protected void addCloseButton() {
        this.close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        this.close.addClickShortcut(Key.ESCAPE);
        this.close.addClickListener(this::onExitButton);
        this.buttonsLayout.add(this.close);
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
