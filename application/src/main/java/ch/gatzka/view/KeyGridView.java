package ch.gatzka.view;

import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.view.KeyGridViewRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.KeyGridViewRecord;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoIcon;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.*;

import static ch.gatzka.Tables.*;

@PageTitle("Keys")
@Route("keys")
@Menu(order = 2, icon = LineAwesomeIconUrl.KEY_SOLID)
@AnonymousAllowed
public class KeyGridView extends VerticalLayout {

    private final Grid<KeyGridViewRecord> grid = new Grid<>(KeyGridViewRecord.class, false);

    private final Map<String, Condition> conditions = new HashMap<>();

    private final KeyGridViewRepository keyGridViewRepository;

    private final List<String> locations = List.of("Customs", "Factory", "Interchange", "Reserve", "Shoreline", "Streets of Tarkov", "The Lab", "Woods");

    private final GameMode gameMode;

    public KeyGridView(KeyGridViewRepository keyGridViewRepository, AuthenticatedAccount authenticatedAccount) {
        this.keyGridViewRepository = keyGridViewRepository;

        if (authenticatedAccount.get().isPresent()) {
            gameMode = authenticatedAccount.get().get().account().getGameMode();
        } else {
            gameMode = GameMode.PvP;
        }

        setSizeFull();

        createHeader();
        createGrid();

        refreshGrid();
    }

    private void addCondition(String key, Condition conditions) {
        this.conditions.put(key, conditions);
    }

    private void removeCondition(String key) {
        this.conditions.remove(key);
    }

    private void createHeader() {
        TextField nameField = new TextField("Name");
        nameField.setPlaceholder("Filter by name");
        nameField.setWidthFull();
        nameField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value == null || value.isEmpty()) {
                conditions.remove("name");
                removeCondition("name");
            } else {
                addCondition("name", KEY_GRID_VIEW.NAME.likeIgnoreCase("%" + value + "%"));
            }
        });

        ComboBox<Boolean> bannedOnFleaField = new ComboBox<>("Can be bought on Flea Market");
        bannedOnFleaField.setPlaceholder("Filter by flea market blacklist");
        bannedOnFleaField.setItems(true, false);
        bannedOnFleaField.setWidthFull();
        bannedOnFleaField.setClearButtonVisible(true);
        bannedOnFleaField.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                removeCondition("itemBannedOnFlea");
            } else {
                addCondition("itemBannedOnFlea", KEY_GRID_VIEW.ITEM_BANNED_ON_FLEA.eq(!event.getValue()));
            }
        });

        MultiSelectComboBox<String> tagSelect = new MultiSelectComboBox<>("Location");
        tagSelect.setItems(locations);
        tagSelect.setPlaceholder("Filter by locations");
        tagSelect.setClearButtonVisible(true);
        tagSelect.setWidthFull();
        tagSelect.addValueChangeListener(event -> {
            Set<String> locations = event.getValue();
            if (locations == null || locations.isEmpty()) {
                removeCondition("locations");
            } else {
                Condition condition = DSL.falseCondition();
                for (String tag : locations) {
                    condition = condition.or(KEY_GRID_VIEW.LOCATIONS.likeIgnoreCase("%" + tag + "%"));
                }
                addCondition("locations", condition);
            }
        });

        Button resetButton = new Button("Reset");
        resetButton.setIcon(new Icon(VaadinIcon.TRASH));
        resetButton.setWidthFull();
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(_ -> {
            nameField.clear();
            bannedOnFleaField.clear();
            tagSelect.clear();

            refreshGrid();
        });

        Button searchButton = new Button("Search");
        searchButton.setIcon(new Icon(VaadinIcon.SEARCH));
        searchButton.setWidthFull();
        searchButton.addClickListener(_ -> refreshGrid());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickShortcut(Key.ENTER);

        VerticalLayout buttons = new VerticalLayout(resetButton, searchButton);
        buttons.setSpacing(false);
        buttons.setWidth("min-content");
        buttons.setPadding(false);
        buttons.getThemeList().add("spacing-xs");

        HorizontalLayout header = new HorizontalLayout(nameField, bannedOnFleaField, tagSelect, buttons);
        header.setAlignSelf(Alignment.END, nameField, bannedOnFleaField, tagSelect);
        header.setWidthFull();
        header.setPadding(false);
        add(header);
    }

    private void refreshGrid() {
        Condition[] conditions = this.conditions.values().toArray(Condition[]::new);
        Result<KeyGridViewRecord> records = keyGridViewRepository.read(conditions);
        grid.setItems(records);
    }

    private void createGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addComponentColumn(entry -> {
            Image icon = new Image(entry.getImageLink(), entry.getName());
            icon.setMaxHeight(50, Unit.PIXELS);
            return icon;
        }).setHeader("Icon");

        grid.addColumn("name").setHeader("Name");
        grid.addColumn("uses").setHeader("Uses");

        grid.addComponentColumn(entry -> {
            Icon icon = (entry.getItemBannedOnFlea() ? LumoIcon.CROSS : LumoIcon.CHECKMARK).create();
            icon.setColor(entry.getItemBannedOnFlea() ? "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            return icon;
        }).setHeader("Can be sold on Flea Market");

        grid.addColumn(gameMode == GameMode.PvP ? "pvpFleaPrice" : "pveFleaPrice").setRenderer(new TextRenderer<>(entry -> "₽ " + (gameMode == GameMode.PvP ? entry.getPvpFleaPrice() : entry.getPveFleaPrice()))).setHeader("Price");
        grid.addColumn(gameMode == GameMode.PvP ? "pvpFleaPricePerUse" : "pveFleaPricePerUse").setRenderer(new TextRenderer<>(entry -> "₽ " + (gameMode == GameMode.PvP ? entry.getPvpFleaPricePerUse() : entry.getPveFleaPricePerUse()))).setHeader("Price per use");

        grid.addComponentColumn(entry -> {
            HorizontalLayout layout = new HorizontalLayout();
            for (String tag : entry.getLocations().split("\\|")) {
                Span badge = new Span(new Span(tag));
                badge.getElement().getThemeList().add("badge");
                layout.add(badge);
            }
            return layout;
        }).setHeader("Locations");

        grid.addComponentColumn(entry -> {
            Button wikiButton = new Button("Wiki", VaadinIcon.BOOKMARK.create());
            wikiButton.setTooltipText("Opens the Tarkov Wiki page for this item");
            wikiButton.addClickListener(_ -> UI.getCurrent().getPage().open(entry.getWikiLink(), "_blank"));
            wikiButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button marketButton = new Button("Market", VaadinIcon.GIFT.create());
            marketButton.setTooltipText("Opens the Tarkov Market page for this item");
            marketButton.addClickListener(_ -> UI.getCurrent().getPage().open(entry.getMarketLink(), "_blank"));
            marketButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            return new HorizontalLayout(marketButton, wikiButton);
        }).setHeader("Actions");

        grid.setSizeFull();

        add(grid);
    }

}
