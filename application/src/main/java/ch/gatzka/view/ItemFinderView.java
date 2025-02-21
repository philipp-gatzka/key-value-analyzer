package ch.gatzka.view;

import ch.gatzka.repository.ItemRepository;
import ch.gatzka.repository.KeyReportRepository;
import ch.gatzka.repository.KeyRepository;
import ch.gatzka.tables.records.ItemRecord;
import ch.gatzka.tables.records.KeyReportRecord;
import ch.gatzka.tables.records.LootReportRecord;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.config.service.Get;
import org.jooq.Result;
import org.jooq.impl.DefaultDSLContext;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.gatzka.Tables.KEY_REPORT;
import static ch.gatzka.Tables.LOOT_REPORT;


@Slf4j
@PageTitle("Item Finder")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.SEARCH_SOLID)
@AnonymousAllowed
public class ItemFinderView extends VerticalLayout {

    private final List<ItemRecord> items;

    private final ItemRepository itemRepository;

    private final FormLayout itemDetails = new FormLayout();

    private Grid<KeyReport> grid ;

    private final DefaultDSLContext dslContext;
    private final KeyRepository keyRepository;
    private final KeyReportRepository keyReportRepository;

    public ItemFinderView(ItemRepository itemRepository, DefaultDSLContext dslContext, KeyRepository keyRepository, KeyReportRepository keyReportRepository) {

        this.itemRepository = itemRepository;

        items = itemRepository.readAll();

        setSizeFull();
        createHeader();
        createGrid();

        add(itemDetails);
        add(grid);

        this.dslContext = dslContext;
        this.keyRepository = keyRepository;
        this.keyReportRepository = keyReportRepository;
    }

    private void createHeader() {
        ComboBox<ItemRecord> selectItemBox = new ComboBox<>();
        selectItemBox.setAutofocus(true);
        selectItemBox.setLabel("Item");
        selectItemBox.setItems(items);
        selectItemBox.setItemLabelGenerator(ItemRecord::getName);
        selectItemBox.setWidthFull();

        selectItemBox.addValueChangeListener(event -> showItemData(event.getValue()));

        HorizontalLayout header = new HorizontalLayout(selectItemBox);
        header.setWidthFull();
        header.setPadding(false);

        add(header);
    }

    private void createGrid() {
        grid = new Grid<>(KeyReport.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addComponentColumn(entry -> {
            Image icon = new Image(entry.keyIcon, entry.keyName);
            icon.setMaxHeight(50, Unit.PIXELS);
            VerticalLayout layout = new VerticalLayout(icon);
            layout.setPadding(false);
            return layout;
        }).setHeader("Icon");

        grid.addColumn("keyName").setHeader("Name");
        grid.addColumn("itemsFound").setHeader("Total times found");
        grid.addColumn("chance").setHeader("Chance").setRenderer(new TextRenderer<>(entry -> String.format("%.2f", entry.chance * 100) + "%" ));
    }

    private void showItemData(ItemRecord selectedItem) {
        itemDetails.removeAll();

        TextField nameField = new TextField("Name");
        nameField.setValue(selectedItem.getName());
        nameField.setReadOnly(true);
        itemDetails.add(nameField);

        TextField wikiLinkField = new TextField("Wiki Link");
        wikiLinkField.setValue(selectedItem.getWikiLink());
        wikiLinkField.setReadOnly(true);
        itemDetails.add(wikiLinkField);

        Result<LootReportRecord> lootReports = dslContext.selectFrom(LOOT_REPORT).where(LOOT_REPORT.ITEM_ID.eq(selectedItem.getId())).fetch();

        IntegerField totalReports = new IntegerField("Total Reports");
        totalReports.setValue(lootReports.size());
        totalReports.setReadOnly(true);
        itemDetails.add(totalReports);

        int totalItemsFound = lootReports.stream().mapToInt(LootReportRecord::getCount).sum();

        IntegerField totalItemsFoundField = new IntegerField("Total Items Found");
        totalItemsFoundField.setValue(totalItemsFound);
        totalItemsFoundField.setReadOnly(true);
        itemDetails.add(totalItemsFoundField);

        List<KeyReport> relevantReports = fetchRelevantKeyReports(selectedItem);

        grid.setItems(relevantReports);
    }

    private List<KeyReport> fetchRelevantKeyReports(ItemRecord selectedItem) {
        Result<LootReportRecord> lootReports = dslContext.selectFrom(LOOT_REPORT).where(LOOT_REPORT.ITEM_ID.eq(selectedItem.getId())).fetch();

        List<KeyReport> data = new ArrayList<>();
        {
            List<Integer> keyReportIds = lootReports.map(LootReportRecord::getKeyReportId);
            Map<Integer, Integer> keys = new HashMap<>();
            Map<Integer, KeyReportRecord> keyReports = keyReportRepository.read(KEY_REPORT.ID.in(keyReportIds)).intoMap(KEY_REPORT.ID, entry -> entry);

            lootReports.forEach(lootReport -> {
                KeyReportRecord keyReport = keyReports.get(lootReport.getKeyReportId());
                keys.computeIfPresent(keyReport.getKeyId(), (k, v) -> v + lootReport.getCount());
                keys.computeIfAbsent(keyReport.getKeyId(), k -> lootReport.getCount());
            });

            keys.forEach((keyId, count) -> {
                keyRepository.findById(keyId).ifPresent(key -> {

                    int totalKeyReports = keyReportRepository.findByKeyId(keyId).size();

                    double chance = (double) count / totalKeyReports;

                    itemRepository.findById(key.getItemId()).ifPresent(keyItem -> {
                        data.add(new KeyReport(keyItem.getIcon(), keyItem.getName(), count, chance));
                    });
                });
            });
        }

        data.sort((a, b) -> Double.compare(b.chance, a.chance));

        return data.stream().filter(a -> a.chance < 1).limit(10).toList();
    }

    @Getter
    @RequiredArgsConstructor
    public static class KeyReport {
        private final String keyIcon;
        private final String keyName;
        private final int itemsFound;
        private final double chance;
    }

}
