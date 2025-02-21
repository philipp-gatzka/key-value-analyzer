package ch.gatzka.view;

import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.ItemRepository;
import ch.gatzka.repository.KeyReportRepository;
import ch.gatzka.repository.KeyRepository;
import ch.gatzka.repository.LootReportRepository;
import ch.gatzka.repository.view.ItemPriceViewRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.ItemPriceViewRecord;
import ch.gatzka.tables.records.ItemRecord;
import ch.gatzka.tables.records.KeyRecord;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PageTitle("Report")
@Route("report")
@Menu(order = 3, icon = LineAwesomeIconUrl.PLUS_SOLID)
@RolesAllowed("USER")
public class ReportView extends VerticalLayout {

    private final ComboBox<KeyRecord> keyComboBox = new ComboBox<>("Select Key");

    private final Grid<ItemReport> grid = new Grid<>(ItemReport.class, false);

    private final KeyRepository keyRepository;

    private final ItemPriceViewRepository itemPriceViewRepository;

    private final KeyReportRepository keyReportRepository;

    private final LootReportRepository lootReportRepository;

    private final Map<Integer, ItemRecord> items;

    private Optional<KeyRecord> selectedKey = Optional.empty();

    private final List<ItemReport> loot = new ArrayList<>();

    private GameMode gameMode;

    private final AuthenticatedAccount authenticatedAccount;

    public ReportView(KeyRepository keyRepository, ItemRepository itemRepository, ItemPriceViewRepository itemPriceViewRepository, KeyReportRepository keyReportRepository, LootReportRepository lootReportRepository, AuthenticatedAccount authenticatedAccount) {
        this.keyRepository = keyRepository;
        this.itemPriceViewRepository = itemPriceViewRepository;
        this.keyReportRepository = keyReportRepository;
        this.lootReportRepository = lootReportRepository;
        this.authenticatedAccount = authenticatedAccount;

        this.items = itemRepository.readAll().intoMap(ItemRecord::getId, item -> item);
        this.gameMode = authenticatedAccount.get().orElseThrow(() -> new RuntimeException("Account not authenticated")).account().getGameMode();

        setSizeFull();

        createHeader();
        createGrid();
        createFooter();
    }

    private void createGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addComponentColumn(entry -> {
            Image icon = new Image(entry.getImageLink(), entry.getName());
            icon.setMaxHeight(50, Unit.PIXELS);
            VerticalLayout layout = new VerticalLayout(icon);
            layout.setPadding(false);
            return layout;
        }).setHeader("Icon");

        grid.addColumn("name").setHeader("Name");
        grid.addColumn("count").setHeader("Count");

        grid.addColumn(new ComponentRenderer<>(Button::new, (button, loot) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(_ -> {
                this.loot.remove(loot);
                refreshGrid();
            });
            button.setIcon(new Icon(VaadinIcon.TRASH));
        })).setHeader("Manage");

        grid.setItems(loot);
        add(grid);
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void createHeader() {
        keyComboBox.setRequired(true);
        keyComboBox.setRequiredIndicatorVisible(true);
        keyComboBox.setErrorMessage("Key is required");
        keyComboBox.setWidthFull();
        keyComboBox.setItems(keyRepository.readAll());
        keyComboBox.setItemLabelGenerator(key -> {
            ItemRecord item = items.get(key.getItemId());
            return item.getName() + " (" + key.getUses() + " uses)";
        });
        keyComboBox.setRenderer(new ComponentRenderer<>(key -> {
            ItemRecord item = items.get(key.getItemId());
            FlexLayout wrapper = new FlexLayout();
            wrapper.setAlignItems(FlexComponent.Alignment.CENTER);

            Image image = new Image();
            image.setSrc(item.getIcon() != null ? item.getIcon() : item.getImageBigLink());
            image.setAlt(item.getName());
            image.setMaxHeight(50, Unit.PIXELS);
            image.getStyle().set("margin-right", "var(--lumo-space-s)");

            Div name = new Div();
            name.setText(item.getName());

            Div uses = new Div();
            uses.setText(key.getUses() + " uses");
            uses.getStyle().set("font-size", "var(--lumo-font-size-s)");
            uses.getStyle().set("color", "var(--lumo-secondary-text-color)");
            name.add(uses);

            wrapper.add(image, name);
            return wrapper;
        }));
        keyComboBox.addValueChangeListener(event -> selectedKey = Optional.ofNullable(event.getValue()));

        Button createButton = new Button("Create Report");
        createButton.addClickListener(this::createReport);
        createButton.setIcon(VaadinIcon.PLUS.create());

        HorizontalLayout header = new HorizontalLayout(keyComboBox, createButton);
        header.setAlignSelf(Alignment.END, createButton);
        header.setWidthFull();
        add(header);
    }

    private void createFooter() {
        Button saveButton = new Button("Save");
        saveButton.setIcon(LineAwesomeIcon.SAVE.create());
        saveButton.addClickListener(this::saveReport);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button resetButton = new Button("Reset");
        resetButton.setIcon(VaadinIcon.TRASH.create());
        resetButton.addClickListener(this::reset);
        resetButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout footer = new HorizontalLayout(saveButton, resetButton);
        footer.setWidthFull();
        add(footer);
    }

    private void reset(ClickEvent<Button> buttonClickEvent) {
        loot.clear();
        refreshGrid();
    }

    private void saveReport(ClickEvent<Button> buttonClickEvent) {
        if (selectedKey.isEmpty()) {
            keyComboBox.setInvalid(true);
            return;
        } else {
            keyComboBox.setInvalid(false);
        }

        if (loot.isEmpty()) {
            Notification.show("No loot to save");
            return;
        }

        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirm Save");

        Select<GameMode> gameModeSelect = new Select<>();
        gameModeSelect.setLabel("Game Mode");
        gameModeSelect.setItems(GameMode.values());
        gameModeSelect.setRequiredIndicatorVisible(true);
        gameModeSelect.setEmptySelectionAllowed(false);
        gameModeSelect.setErrorMessage("Game Mode is required");

        gameModeSelect.addValueChangeListener(event -> this.gameMode = event.getValue());

        confirmDialog.add(gameModeSelect);

        Button confirmButton = new Button("Confirm", _ -> {
            if (gameModeSelect.isEmpty()) {
                gameModeSelect.setInvalid(true);
                return;
            }

            AuthenticatedAccount.UserInfo userInfo = authenticatedAccount.get().orElseThrow();
            Integer accountId = userInfo.account().getId();

            Integer keyReportId = keyReportRepository.insertWithSequence(entity -> entity.setReportedAt(LocalDateTime.now()).setMode(gameMode).setKeyId(selectedKey.get().getId()).setAccountId(accountId));

            for (ItemReport itemReport : loot) {
                lootReportRepository.insertWithSequence(entity -> entity.setKeyReportId(keyReportId).setCount(itemReport.count).setItemId(itemReport.getItem().getId()));
            }

            Notification.show("Report saved successfully");

            reset(null);
            confirmDialog.close();
        });

        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", _ -> confirmDialog.close());

        confirmDialog.getFooter().add(cancelButton, confirmButton);
        confirmDialog.open();
    }

    private void addLoot(ItemRecord item, Integer count) {
        ItemPriceViewRecord itemPrices = itemPriceViewRepository.findByItemId(item.getId()).orElseThrow(() -> new IllegalArgumentException("Item prices not found"));

        int itemValue;
        String itemValueString;
        if (gameMode == GameMode.PvP) {
            if (itemPrices.getPvpFleaValue() > itemPrices.getPvpTraderValue()) {
                itemValueString = "₽ " + itemPrices.getPvpFleaValue() * count;
                itemValue = itemPrices.getPvpFleaValue() * count;
            } else {
                itemValueString = itemPrices.getPvpTraderValueCurrency() + " " + itemPrices.getPvpTraderValue() * count;
                itemValue = itemPrices.getPvpTraderValue() * count;
            }
        } else {
            if (itemPrices.getPveFleaValue() > itemPrices.getPveTraderValue()) {
                itemValueString = "₽ " + itemPrices.getPveFleaValue() * count;
                itemValue = itemPrices.getPveFleaValue() * count;
            } else {
                itemValueString = itemPrices.getPveTraderValueCurrency() + " " + itemPrices.getPveTraderValue() * count;
                itemValue = itemPrices.getPveTraderValue() * count;
            }
        }

        this.loot.add(new ItemReport(item, count, itemValue, itemValueString));
        refreshGrid();
    }

    private void createReport(ClickEvent<Button> buttonClickEvent) {
        createDialog().open();
    }

    @Getter
    public static class ItemReport {
        private final ItemRecord item;
        private final String imageLink;
        private final String name;
        private final int count;
        private final int itemValue;
        private final String itemValueString;

        public ItemReport(ItemRecord item, int count, int itemValue, String itemValueString) {
            this.item = item;
            this.imageLink = item.getImageLink();
            this.name = item.getName();
            this.count = count;
            this.itemValue = itemValue;
            this.itemValueString = itemValueString;
        }
    }

    private Dialog createDialog() {
        Dialog dialog = new Dialog();

        dialog.setHeaderTitle("Report Loot");
        dialog.setMinWidth(270, Unit.PIXELS);
        dialog.setWidth(30, Unit.PERCENTAGE);
        dialog.setMaxWidth(40, Unit.PERCENTAGE);

        ComboBox<ItemRecord> selectItemBox = new ComboBox<>();
        selectItemBox.setAutofocus(true);
        selectItemBox.setLabel("Item");
        selectItemBox.setItems(items.values());
        selectItemBox.setItemLabelGenerator(ItemRecord::getName);
        selectItemBox.setWidth(100, Unit.PERCENTAGE);
        selectItemBox.setRequired(true);
        selectItemBox.setRequiredIndicatorVisible(true);
        selectItemBox.setManualValidation(true);
        selectItemBox.setErrorMessage("Item is required");

        IntegerField count = new IntegerField("Count");
        count.setValue(1);
        count.setMin(1);
        count.setStepButtonsVisible(true);
        count.setWidth(100, Unit.PERCENTAGE);
        count.setRequired(true);
        count.setRequiredIndicatorVisible(true);
        count.setManualValidation(true);
        count.setErrorMessage("Count is required");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.add(selectItemBox, count);

        dialog.add(dialogLayout);

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(_ -> {
            if (selectItemBox.isEmpty()) {
                selectItemBox.setInvalid(true);
            } else if (count.isEmpty()) {
                selectItemBox.setInvalid(false);
                count.setInvalid(true);
            } else {
                count.setInvalid(false);
                addLoot(selectItemBox.getValue(), count.getValue());
                dialog.close();
            }
        });
        Button cancelButton = new Button("Cancel", _ -> dialog.close());
        dialog.getFooter().add(cancelButton);
        dialog.getFooter().add(saveButton);

        return dialog;
    }
}
