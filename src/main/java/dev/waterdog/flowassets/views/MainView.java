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

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import dev.waterdog.flowassets.utils.Helper;
import io.quarkus.oidc.IdToken;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.inject.Inject;

@Route("")
public class MainView extends AppLayout {

    @Inject
    @IdToken
    JsonWebToken idToken;

    public MainView() {
        this.createHeader();
        this.createMenu();
        addClassName("centered-content");
    }

    private void createHeader() {
        H2 logo = new H2("FlowAssets");
        logo.addClassNames("text-1", "m-m");

        Span greetingSpan = new Span("Hello");
        Span helloSpan = new Span(this.createIcon(VaadinIcon.HAND), greetingSpan);
        helloSpan.getElement().getThemeList().add("badge success");
        helloSpan.getStyle().set("padding", "var(--lumo-space-s");

        greetingSpan.addAttachListener(event -> {
            String userName = Helper.getUserName(this.idToken);
            if (userName.isEmpty()) {
                greetingSpan.setText("Unauthenticated");
                helloSpan.getElement().getThemeList().add("badge error");
            } else {
                greetingSpan.setText("Hello " + userName);
            }
        });

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, helloSpan);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");
        this.addToNavbar(header);
    }

    private void createMenu() {
        RouterLink assetsLink = new RouterLink("Assets", AssetsView.class);
        assetsLink.setHighlightCondition(HighlightConditions.sameLocation());

        RouterLink repositoriesLink = new RouterLink("Repositories", S3ServersView.class);
        repositoriesLink.setHighlightCondition(HighlightConditions.sameLocation());

        this.addToDrawer(new VerticalLayout(assetsLink, repositoriesLink));
    }

    private Icon createIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }
}
