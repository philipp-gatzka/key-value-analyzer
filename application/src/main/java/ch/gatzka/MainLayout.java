package ch.gatzka;


import ch.gatzka.enums.GameMode;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.AccountRecord;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
@StyleSheet("context://fonts/bender/style.css")
public class MainLayout extends AppLayout {

    private H1 viewTitle;

    private AuthenticatedAccount authenticatedUser;
    private AccessAnnotationChecker accessChecker;

    public MainLayout(AuthenticatedAccount authenticatedUser, AccessAnnotationChecker accessChecker) {
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        Span appName = new Span("Key Value Analyzer");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        VerticalLayout layout = new VerticalLayout();

        Footer footer = new Footer(layout);

        Optional<AuthenticatedAccount.UserInfo> maybeUser = authenticatedUser.get();

        Select<GameMode> gameModeSelect = new Select<>();
        gameModeSelect.setItems(GameMode.values());
        gameModeSelect.setItemLabelGenerator(GameMode::getLiteral);

        if (maybeUser.isPresent()) {
            AuthenticatedAccount.UserInfo user = maybeUser.get();
            AccountRecord account = user.account();
            gameModeSelect.setValue(account.getGameMode());

        } else {
            gameModeSelect.setValue(GameMode.PvP);
        }

        gameModeSelect.setLabel("Game mode");
        gameModeSelect.addValueChangeListener(event -> {
            if (maybeUser.isEmpty()) {
                Dialog dialog = new Dialog();
                dialog.setCloseOnEsc(true);

                dialog.setHeaderTitle("Game mode selection requires login");

                Anchor loginLink = new Anchor("/oauth2/authorization/google", "Login with Google");
                loginLink.setRouterIgnore(true);

                Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create());

                dialog.getFooter().add(loginLink, cancelButton);

                dialog.open();
            } else {
                AuthenticatedAccount.UserInfo userInfo = maybeUser.get();
                userInfo.account().setGameMode(event.getValue()).update();
                UI.getCurrent().getPage().reload();
            }
        });

        layout.add(gameModeSelect);

        if (maybeUser.isPresent()) {
            AuthenticatedAccount.UserInfo user = maybeUser.get();
            AccountRecord account = user.account();

            String picture = user.attributes().get("picture").toString();

            Avatar avatar = new Avatar(account.getEmail());
            avatar.setImage(picture);
            avatar.setThemeName("xsmall");
            avatar.getElement().setAttribute("tabindex", "-1");

            Div div = new Div();
            div.add(avatar);
            div.add(user.attributes().get("name").toString());
            div.add(LumoIcon.DROPDOWN.create());
            div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);

            MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");
            userMenu.setWidthFull();

            MenuItem userName = userMenu.addItem("");
            userName.add(div);
            userName.getSubMenu().addItem("Logout", _ -> {
                authenticatedUser.logout();
            });

            layout.add(userMenu);
        } else {
            Anchor loginLink = new Anchor("/oauth2/authorization/google", "Login with Google");
            loginLink.setRouterIgnore(true);
            loginLink.setWidthFull();
            layout.add(loginLink);
        }

        return footer;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }
}
