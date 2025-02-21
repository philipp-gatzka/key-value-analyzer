package ch.gatzka.view;

import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.view.ItemGridViewRepository;
import ch.gatzka.repository.view.TagViewRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.ItemGridViewRecord;
import ch.gatzka.tables.records.TagViewRecord;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoIcon;
import org.jooq.Condition;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.gatzka.Tables.ITEM_GRID_VIEW;

@PageTitle("Items")
@Route("items")
@Menu(order = 1, icon = LineAwesomeIconUrl.BOX_SOLID)
@AnonymousAllowed
public class ItemGridView extends VerticalLayout {

    private final Grid<ItemGridViewRecord> grid = new Grid<>(ItemGridViewRecord.class, false);

    private final ItemGridViewRepository itemGridViewRepository;

    private final Map<String, Condition> conditions = new HashMap<>();

    private final List<String> tags;

    private final GameMode gameMode;

    public ItemGridView(ItemGridViewRepository itemGridViewRepository, AuthenticatedAccount authenticatedAccount, TagViewRepository tagViewRepository) {
        this.itemGridViewRepository = itemGridViewRepository;

        this.tags = tagViewRepository.readAll().map(TagViewRecord::getCleanName);

        if (authenticatedAccount.get().isPresent()) {
            gameMode = authenticatedAccount.get().get().account().getGameMode();
        } else {
            gameMode = GameMode.PvP;
        }

        setSizeFull();

        createHeader();
        createGrid();
        createFooter();

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
                addCondition("name", ITEM_GRID_VIEW.NAME.likeIgnoreCase("%" + value + "%"));
            }
        });

        ComboBox<Boolean> bannedOnFleaField = new ComboBox<>("Can be sold on Flea Market");
        bannedOnFleaField.setPlaceholder("Filter by flea market blacklist");
        bannedOnFleaField.setItems(true, false);
        bannedOnFleaField.setWidthFull();
        bannedOnFleaField.setClearButtonVisible(true);
        bannedOnFleaField.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                removeCondition("itemBannedOnFlea");
            } else {
                addCondition("itemBannedOnFlea", ITEM_GRID_VIEW.ITEM_BANNED_ON_FLEA.eq(!event.getValue()));
            }
        });

        MultiSelectComboBox<String> tagSelect = new MultiSelectComboBox<>("Tags");
        tagSelect.setItems(tags);
        tagSelect.setPlaceholder("Filter by tags");
        tagSelect.setClearButtonVisible(true);
        tagSelect.setWidthFull();
        tagSelect.addValueChangeListener(event -> {
            Set<String> tags = event.getValue();
            if (tags == null || tags.isEmpty()) {
                removeCondition("tags");
            } else {
                Condition condition = DSL.falseCondition();
                for (String tag : tags) {
                    condition = condition.or(ITEM_GRID_VIEW.TAGS.likeIgnoreCase("%" + tag + "%"));
                }
                addCondition("tags", condition);
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

    private void createFooter() {
        // Select<String> modeSelect = new Select<>();
        // modeSelect.setItems("PvE", "PvP");
        // modeSelect.setValue("PvP");
        // modeSelect.addValueChangeListener(event -> {
        //     grid.getColumnByKey("pveSellPriceFlea").setVisible(event.getValue().equals("PvE"));
        //     grid.getColumnByKey("pvpSellPriceFlea").setVisible(event.getValue().equals("PvP"));
        //     grid.getColumnByKey("pveSellPriceTrader").setVisible(event.getValue().equals("PvE"));
        //     grid.getColumnByKey("pvpSellPriceTrader").setVisible(event.getValue().equals("PvP"));
        // });
        // HorizontalLayout spacer = new HorizontalLayout();
        // spacer.setWidthFull();
        // HorizontalLayout layout = new HorizontalLayout(spacer, modeSelect);
        // layout.setWidthFull();
        // add(layout);
    }

    private void refreshGrid() {
        Condition[] conditions = this.conditions.values().toArray(Condition[]::new);
        Result<ItemGridViewRecord> records = itemGridViewRepository.read(conditions);
        grid.setItems(records);
    }

    private void createGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addClassName("grid");

        grid.addComponentColumn(entry -> {
            Image icon = new Image(entry.getImageLink(), entry.getName());
            icon.setMaxHeight(50, Unit.PIXELS);
            VerticalLayout layout = new VerticalLayout(icon);
            layout.setPadding(false);
            return layout;
        }).setHeader("Icon");

        grid.addColumn("name").setHeader("Name");

        grid.addComponentColumn(entry -> {
            Icon icon = (entry.getItemBannedOnFlea() ? LumoIcon.CROSS : LumoIcon.CHECKMARK).create();
            icon.setColor(entry.getItemBannedOnFlea() ? "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            return icon;
        }).setHeader("Can be sold on Flea Market");

        grid.addColumn(gameMode == GameMode.PvP ? "pvpFleaValue" : "pveFleaValue").setRenderer(new TextRenderer<>(entry -> "₽ " + (gameMode == GameMode.PvP ? entry.getPvpFleaValue() : entry.getPveFleaValue()))).setHeader("Flea Market Value");
        grid.addColumn(gameMode == GameMode.PvP ? "pvpTraderValue" : "pveTraderValue").setRenderer(new TextRenderer<>(entry -> "₽ " + (gameMode == GameMode.PvP ? entry.getPvpTraderValue() : entry.getPveTraderValue()))).setHeader("Trader Value");

        grid.addComponentColumn(entry -> {
            HorizontalLayout layout = new HorizontalLayout();
            for (String tag : entry.getTags().split("\\|")) {
                Span badge = new Span(new Span(tag));
                badge.getElement().getThemeList().add("badge");
                layout.add(badge);
            }
            return layout;
        }).setHeader("Tags");

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
