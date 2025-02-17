package ch.gatzka.view;

import ch.gatzka.repository.ItemRepository;
import ch.gatzka.repository.KeyReportRepository;
import ch.gatzka.repository.KeyRepository;
import ch.gatzka.repository.LootReportRepository;
import ch.gatzka.repository.view.HighestItemPriceRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.HighestItemPriceRecord;
import ch.gatzka.tables.records.ItemRecord;
import ch.gatzka.tables.records.KeyRecord;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDateTime;
import java.util.*;

@PageTitle("Report")
@Route("report")
@Menu(order = 2, icon = LineAwesomeIconUrl.LIST_SOLID)
@RolesAllowed("USER")
public class ReportView extends VerticalLayout {

    private final AuthenticatedAccount authenticatedAccount;

    private final KeyReportRepository keyReportRepository;
    private final LootReportRepository lootReportRepository;
    private Optional<KeyRecord> selectedKey = Optional.empty();

    private final KeyRepository keyRepository;

    private final ItemRepository itemRepository;

    private final HighestItemPriceRepository highestItemPriceRepository;

    private final Map<Integer, ItemRecord> items = new HashMap<>();

    private final Grid<ItemReport> grid = new Grid<>(ItemReport.class, false);

    private H3 valueDisplay;

    private List<ItemReport> loot = new ArrayList<>();

    private ComboBox<KeyRecord> keyComboBox;

    public ReportView(AuthenticatedAccount authenticatedAccount, KeyRepository keyRepository, ItemRepository itemRepository, HighestItemPriceRepository highestItemPriceRepository, KeyReportRepository keyReportRepository, LootReportRepository lootReportRepository) {
        this.authenticatedAccount = authenticatedAccount;
        this.keyRepository = keyRepository;
        this.itemRepository = itemRepository;
        this.highestItemPriceRepository = highestItemPriceRepository;

        itemRepository.readAll().forEach(item -> items.put(item.getId(), item));

        createHeader();
        createGrid();
        createFooter();

        setSizeFull();
        this.keyReportRepository = keyReportRepository;
        this.lootReportRepository = lootReportRepository;

        if (authenticatedAccount.get().isEmpty()) {
            UI.getCurrent().navigate("/oauth2/authorization/google");
        }
    }

    private void createHeader() {
        keyComboBox = new ComboBox<>("Select Key");
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
            image.setSrc(item.getImageLink() != null ? item.getImageLink() : item.getName());
            image.setAlt(item.getName());
            image.setHeight(50, Unit.PIXELS);
            image.setWidth("var(--lumo-size-m)");
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
        keyComboBox.addValueChangeListener(event -> {
            selectedKey = Optional.ofNullable(event.getValue());
            updateValue();
        });

        Button createButton = new Button("Create Report");
        createButton.addClickListener(this::createReport);

        HorizontalLayout header = new HorizontalLayout(keyComboBox, createButton);
        header.setAlignSelf(Alignment.END, createButton);
        header.setWidthFull();
        add(header);
    }

    private void createReport(ClickEvent<Button> buttonClickEvent) {
        createDialog().open();
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
        saveButton.addClickListener(e -> {
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

    private void addLoot(ItemRecord item, Integer count) {
        ItemReport itemReport = new ItemReport(item, count);
        itemReport.value = ItemReport.getValue(highestItemPriceRepository, item, count);
        loot.add(itemReport);

        refreshGrid();
        updateValue();
    }

    private void createGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addComponentColumn(data -> {
            Image image = new Image(data.imageLink == null ? data.name : data.imageLink, data.name);
            image.setMaxHeight(50, Unit.PIXELS);
            return image;
        }).setHeader("Image");
        grid.addColumn("name").setHeader("Name");
        grid.addColumn("count").setHeader("Count");
        grid.addColumn("value").setHeader("Value");
        grid.addColumn(new ComponentRenderer<>(Button::new, (button, loot) -> {
            button.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            button.addClickListener(e -> {
                this.loot.remove(loot);
                refreshGrid();
                updateValue();
            });
            button.setIcon(new Icon(VaadinIcon.TRASH));
        })).setHeader("Manage");

        grid.setSizeFull();
        grid.setItems(loot);

        add(grid);
    }

    private void updateValue() {
        int keyPrice = 0;
        if (selectedKey.isPresent()) {
            KeyRecord key = selectedKey.get();
            ItemRecord keyItem = items.get(key.getItemId());
            keyPrice = keyItem.getBuyPrice() != null ? keyItem.getBuyPrice() / key.getUses() : 0;
        }
        int lootValue = loot.stream().mapToInt(ItemReport::getValue).sum();

        valueDisplay.setText("₽ " + lootValue + " / ₽ " + keyPrice);

        if (keyPrice > lootValue) {
            valueDisplay.getStyle().set("color", "var(--lumo-error-text-color)");
        } else {
            valueDisplay.getStyle().set("color", "var(--lumo-success-text-color)");
        }
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void createFooter() {
        Button saveButton = new Button("Save");
        saveButton.addClickListener(this::saveReport);

        Button resetButton = new Button("Reset");
        resetButton.addClickListener(this::reset);

        valueDisplay = new H3("₽ N/A");
        valueDisplay.setWidthFull();
        valueDisplay.getStyle().set("text-align", "right");

        HorizontalLayout footer = new HorizontalLayout(saveButton, resetButton, valueDisplay);
        footer.setWidthFull();
        footer.setAlignSelf(Alignment.CENTER, valueDisplay);
        add(footer);
    }

    private void reset(ClickEvent<Button> buttonClickEvent) {
        loot.clear();
        refreshGrid();
        updateValue();
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
        confirmDialog.add("Are you sure you want to save the report?");

        Button confirmButton = new Button("Confirm", _ -> {
            AuthenticatedAccount.UserInfo userInfo = authenticatedAccount.get().orElseThrow();
            Integer accountId = userInfo.account().getId();

            Integer keyReportId = keyReportRepository.insertWithSequence(entity -> entity.setReportedAt(LocalDateTime.now()).setKeyId(selectedKey.get().getId()).setReportedBy(accountId));

            for (ItemReport itemReport : loot) {
                lootReportRepository.insertWithSequence(entity -> entity.setKeyReportId(keyReportId).setCount(itemReport.count).setItemId(itemReport.getItem().getId()));
            }

            Notification.show("Report saved successfully");

            loot.clear();
            refreshGrid();
            updateValue();
            confirmDialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", _ -> confirmDialog.close());

        confirmDialog.getFooter().add(cancelButton, confirmButton);
        confirmDialog.open();
    }

    @Getter
    public static class ItemReport {
        private final ItemRecord item;
        private final String imageLink;
        private final String name;
        private final int count;
        private int value;

        public ItemReport(ItemRecord item, int count) {
            this.item = item;
            this.imageLink = item.getImageLink();
            this.name = item.getName();
            this.count = count;
        }

        private static int getValue(HighestItemPriceRepository highestItemPriceRepository, ItemRecord item, int count) {
            Optional<HighestItemPriceRecord> highestPrice = highestItemPriceRepository.findByItemId(item.getId());
            return highestPrice.map(highestItemPriceRecord -> highestItemPriceRecord.getHighestSellPrice() * count).orElse(0);
        }
    }

}
